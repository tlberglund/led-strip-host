package com.timberglund.ledhost.web

import com.timberglund.ledhost.db.SettingsRepository
import com.timberglund.ledhost.mapper.PixelMapper
import com.timberglund.ledstrip.BluetoothHost
import com.timberglund.ledstrip.StripDiscoveryEvent
import com.timberglund.ledhost.pattern.ParameterDef
import com.timberglund.ledhost.pattern.Pattern
import com.timberglund.ledhost.pattern.PatternParameters
import com.timberglund.ledhost.pattern.PatternRegistry
import com.timberglund.ledhost.renderer.FrameRenderer
import com.timberglund.ledhost.renderer.RenderStats
import com.timberglund.ledhost.viewport.Viewport
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import kotlin.time.Duration.Companion.seconds
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import mu.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger {}

/**
 * Web server providing real-time LED strip preview and control interface.
 *
 * @property port Server port (default from configuration)
 * @property viewport The viewport to broadcast
 * @property patternRegistry Registry of available patterns
 * @property renderer Frame renderer for statistics
 * @property mapper Pixel mapper for LED strip visualization
 */
class PreviewServer(private val port: Int,
                    private val viewport: Viewport,
                    private val patternRegistry: PatternRegistry,
                    private val renderer: FrameRenderer?,
                    private val settingsRepository: SettingsRepository,
                    private val mapper: PixelMapper,
                    private val bleManager: BluetoothHost? = null) {
   private val broadcaster = WebSocketBroadcaster()
   private val stripsBroadcaster = StripsWsBroadcaster()
   private var server: EmbeddedServer<*, *>? = null
   private var currentPattern: Pattern? = null
   private var patternChangeListener: ((String, PatternParameters) -> Unit)? = null

   /**
    * Sets a listener to be notified when the pattern changes via the web interface.
    */
   fun setPatternChangeListener(listener: (String, PatternParameters) -> Unit) {
      patternChangeListener = listener
   }

   /**
    * Starts the web server.
    */
   fun start() {
      server = embeddedServer(Netty, port = port) {
         install(WebSockets) {
            pingPeriod = 15.seconds
            timeout = 15.seconds
            maxFrameSize = Long.MAX_VALUE
            masking = false
         }

         install(ContentNegotiation) {
            json(Json {
               prettyPrint = true
               isLenient = true
               encodeDefaults = true
            })
         }

         routing {
            // Serve static files (HTML, CSS, JS)
            staticResources("/", "web") {
               default("index.html")
            }

            // WebSocket endpoint for real-time viewport updates
            webSocket("/viewport") {
               broadcaster.addClient(this)
               try {
                  for(frame in incoming) {
                     // Handle incoming commands from client
                     when(frame) {
                        is Frame.Text -> handleCommand(frame.readText())
                        else -> {}
                     }
                  }
               }
               finally {
                  broadcaster.removeClient(this)
               }
            }

            // WebSocket endpoint for real-time strip discovery and status updates
            webSocket("/ws/strips") {
               stripsBroadcaster.addClient(this)
               // Send current strip list immediately on connect
               stripsBroadcaster.sendTo(this, buildStripsUpdate())
               try {
                  for(frame in incoming) { /* read-only channel */ }
               }
               finally {
                  stripsBroadcaster.removeClient(this)
               }
            }

            // REST API endpoints
            get("/api/patterns") {
               val patternInfos = patternRegistry.listPatterns().map { name ->
                  val pattern = patternRegistry.get(name)!!
                  PatternInfo(name, pattern.description, pattern.parameters)
               }
               call.respond(patternInfos)
            }

            post("/api/pattern/{name}") {
               val name = call.parameters["name"]
               if (name == null) {
                  call.respond(HttpStatusCode.BadRequest, "Pattern name required")
                  return@post
               }

               val params = try {
                  val jsonMap = call.receive<Map<String, kotlinx.serialization.json.JsonElement>>()
                  parseJsonParams(jsonMap)
               }
               catch (e: Exception) {
                  emptyMap()
               }

               setPattern(name, params)
               call.respond(HttpStatusCode.OK)
            }

            get("/api/config") {
               val w = settingsRepository.getSetting("viewportWidth")?.toIntOrNull() ?: 240
               val h = settingsRepository.getSetting("viewportHeight")?.toIntOrNull() ?: 135
               val fps = settingsRepository.getSetting("targetFPS")?.toIntOrNull() ?: 60
               call.respond(buildJsonObject {
                  putJsonObject("viewport") {
                     put("width", w)
                     put("height", h)
                  }
                  put("targetFPS", fps)
               })
            }

            get("/api/stats") {
               val stats = renderer?.getStatistics() ?: RenderStats()
               call.respond(stats)
            }

            get("/api/clients") {
               call.respond(mapOf("count" to broadcaster.getClientCount()))
            }

            get("/api/led-strips") {
               val ledColors = mapper.mapViewportToLEDs(viewport)
               val mapping = mapper.getMapping()

               // Invert mapping to get LEDAddress -> Point
               val ledToPoint = mapping.entries.associate { (point, ledAddress) ->
                  ledAddress to point
               }

               // Group by strip ID and include positions
               val stripData = ledColors.entries
                  .groupBy({ it.key.stripId }, { it.key.ledIndex to it.value })
                  .map { (stripId, leds) ->
                     val sortedLeds = leds.sortedBy { it.first }.map { (ledIndex, color) ->
                        val ledAddress = com.timberglund.ledhost.mapper.LEDAddress(stripId, ledIndex)
                        val position = ledToPoint[ledAddress]
                        LEDData(
                           x = position?.x ?: 0,
                           y = position?.y ?: 0,
                           r = color.r,
                           g = color.g,
                           b = color.b
                        )
                     }
                     LEDStripData(
                        id = stripId,
                        leds = sortedLeds
                     )
                  }
                  .sortedBy { it.id }

               call.respond(stripData)
            }

            get("/api/strips") {
               val stripLengths = settingsRepository.getAllStrips().associate { it.id to (it.length ?: 0) }
               val infos = bleManager?.getStripInfos() ?: emptyList()
               val response = infos.map { info ->
                  StripStatusResponse(
                     id = info.id,
                     name = info.name,
                     address = info.address,
                     connected = info.connected,
                     length = stripLengths[info.id] ?: 0
                  )
               }
               call.respond(response)
            }

            post("/api/strips/{id}/connect") {
               val id = call.parameters["id"]?.toIntOrNull()
               if(id == null) {
                  call.respond(HttpStatusCode.BadRequest, "Invalid strip ID")
                  return@post
               }
               val success = bleManager?.connectStrip(id) ?: false
               if(success)
                  call.respond(HttpStatusCode.OK)
               else
                  call.respond(HttpStatusCode.ServiceUnavailable, "Failed to connect to strip $id")
            }

            post("/api/strips/{id}/disconnect") {
               val id = call.parameters["id"]?.toIntOrNull()
               if(id == null) {
                  call.respond(HttpStatusCode.BadRequest, "Invalid strip ID")
                  return@post
               }
               bleManager?.disconnectStrip(id)
               call.respond(HttpStatusCode.OK)
            }

            get("/api/background-image") {
               val cachePath = settingsRepository.getCacheFilePath()
               if(cachePath != null) {
                  val file = File(cachePath)
                  call.respondFile(file)
               } else {
                  call.respond(HttpStatusCode.NotFound, "No background image stored")
               }
            }

            // ── Settings API ──────────────────────────────────────────────────

            get("/api/settings") {
               val w = settingsRepository.getSetting("viewportWidth")?.toIntOrNull() ?: 240
               val h = settingsRepository.getSetting("viewportHeight")?.toIntOrNull() ?: 135
               val fps = settingsRepository.getSetting("targetFPS")?.toIntOrNull() ?: 60
               val scan = settingsRepository.getSetting("scanIntervalSeconds")?.toIntOrNull() ?: 15
               val telemetry = settingsRepository.getSetting("telemetryIntervalSeconds")?.toIntOrNull() ?: 5
               call.respond(ScalarSettingsResponse(
                  viewportWidth = w,
                  viewportHeight = h,
                  targetFPS = fps,
                  scanIntervalSeconds = scan,
                  telemetryIntervalSeconds = telemetry
               ))
            }

            put("/api/settings") {
               val body = call.receive<Map<String, JsonElement>>()
               val errors = mutableListOf<String>()

               val fieldMap = mapOf(
                  "viewportWidth" to body["viewportWidth"],
                  "viewportHeight" to body["viewportHeight"],
                  "targetFPS" to body["targetFPS"],
                  "scanIntervalSeconds" to body["scanIntervalSeconds"],
                  "telemetryIntervalSeconds" to body["telemetryIntervalSeconds"]
               )

               for((key, element) in fieldMap) {
                  if(element == null) continue
                  val v = (element as? JsonPrimitive)?.intOrNull
                  if(v == null || v <= 0) {
                     errors += "$key must be a positive integer"
                  } else {
                     settingsRepository.setSetting(key, v.toString())
                  }
               }

               if(errors.isNotEmpty()) {
                  call.respond(HttpStatusCode.BadRequest, mapOf("errors" to errors))
               } else {
                  call.respond(HttpStatusCode.OK)
               }
            }

            get("/api/settings/strips") {
               val strips = settingsRepository.getAllStrips().map { it.toResponse() }
               call.respond(strips)
            }

            post("/api/settings/strips") {
               val req = call.receive<CreateStripRequest>()
               if(req.btName.isBlank()) {
                  call.respond(HttpStatusCode.BadRequest, "btName is required")
                  return@post
               }
               if(req.length == null || req.length <= 0) {
                  call.respond(HttpStatusCode.BadRequest, "length must be a positive integer")
                  return@post
               }
               val id = settingsRepository.createStrip(
                  btName = req.btName,
                  length = req.length,
                  startX = req.startX,
                  startY = req.startY,
                  endX = req.endX,
                  endY = req.endY,
                  reverse = req.reverse
               )
               val created = settingsRepository.getAllStrips().first { it.id == id }
               call.respond(HttpStatusCode.Created, created.toResponse())
            }

            put("/api/settings/strips/{id}") {
               val id = call.parameters["id"]?.toIntOrNull()
               if(id == null) {
                  call.respond(HttpStatusCode.BadRequest, "Invalid strip ID")
                  return@put
               }
               val req = call.receive<UpdateStripRequest>()
               val updated = settingsRepository.updateStrip(
                  id = id,
                  btName = req.btName,
                  length = req.length,
                  startX = req.startX,
                  startY = req.startY,
                  endX = req.endX,
                  endY = req.endY,
                  reverse = req.reverse
               )
               if(updated)
                  call.respond(HttpStatusCode.OK)
               else
                  call.respond(HttpStatusCode.NotFound, "Strip $id not found")
            }

            delete("/api/settings/strips/{id}") {
               val id = call.parameters["id"]?.toIntOrNull()
               if(id == null) {
                  call.respond(HttpStatusCode.BadRequest, "Invalid strip ID")
                  return@delete
               }
               // Disconnect any active BLE client before deleting
               bleManager?.disconnectStrip(id)
               val deleted = settingsRepository.deleteStrip(id)
               if(deleted)
                  call.respond(HttpStatusCode.NoContent)
               else
                  call.respond(HttpStatusCode.NotFound, "Strip $id not found")
            }

            post("/api/settings/background-image") {
               val maxBytes = 10 * 1024 * 1024 // 10 MB
               val multipart = call.receiveMultipart()
               var imageBytes: ByteArray? = null
               var mimeType = "image/jpeg"

               multipart.forEachPart { part ->
                  if(part is PartData.FileItem) {
                     val bytes = part.streamProvider().readBytes()
                     if(bytes.size > maxBytes) {
                        imageBytes = null
                     } else {
                        imageBytes = bytes
                        mimeType = part.contentType?.toString() ?: "image/jpeg"
                     }
                  }
                  part.dispose()
               }

               val bytes = imageBytes
               if(bytes == null) {
                  call.respond(HttpStatusCode.BadRequest, "Image required and must be under 10 MB")
                  return@post
               }

               settingsRepository.setBackgroundImage(bytes, mimeType)
               call.respond(HttpStatusCode.OK)
            }

            delete("/api/settings/background-image") {
               val cachePath = settingsRepository.getCacheFilePath()
               if(cachePath == null) {
                  call.respond(HttpStatusCode.NotFound, "No background image stored")
                  return@delete
               }
               settingsRepository.deleteBackgroundImage()
               call.respond(HttpStatusCode.NoContent)
            }
         }
      }

      server?.start(wait = false)
      logger.info { "Preview server running at http://localhost:$port" }

      // Collect discovery events and push to /ws/strips clients
      if(bleManager != null) {
         server?.application?.launch {
            bleManager.discoveryEvents.collect { event ->
               val message = when(event) {
                  is StripDiscoveryEvent.ScanStarted ->
                     "Scanning for strip controllers..."
                  is StripDiscoveryEvent.NewDeviceFound ->
                     "Found ${event.name} (${event.address})"
                  is StripDiscoveryEvent.ScanCompleted ->
                     "Scan complete — ${event.found} new device(s) found"
                  is StripDiscoveryEvent.ScanError ->
                     "Scan error: ${event.message}"
                  is StripDiscoveryEvent.ReconnectAttempted ->
                     "Reconnecting to ${event.name} (strip ${event.id})..."
                  is StripDiscoveryEvent.ReconnectSucceeded ->
                     "Reconnected to ${event.name} (strip ${event.id})"
                  is StripDiscoveryEvent.ReconnectFailed ->
                     "Failed to reconnect to ${event.name} (strip ${event.id}): ${event.reason}"
               }
               stripsBroadcaster.broadcast(DiscoveryEventMessage(message = message))
               if(event is StripDiscoveryEvent.NewDeviceFound ||
                  event is StripDiscoveryEvent.ScanCompleted ||
                  event is StripDiscoveryEvent.ReconnectSucceeded ||
                  event is StripDiscoveryEvent.ReconnectFailed) {
                  stripsBroadcaster.broadcast(buildStripsUpdate())
               }
            }
         }
      }
   }

   private suspend fun buildStripsUpdate(): StripsUpdateMessage {
      val stripLengths = settingsRepository.getAllStrips().associate { it.id to (it.length ?: 0) }
      val infos = bleManager?.getStripInfos() ?: emptyList()
      val strips = infos.map { info ->
         StripStatusResponse(
            id = info.id,
            name = info.name,
            address = info.address,
            connected = info.connected,
            length = stripLengths[info.id] ?: 0
         )
      }
      return StripsUpdateMessage(strips = strips)
   }

   /**
    * Broadcasts a telemetry reading to all connected /ws/strips clients.
    */
   suspend fun broadcastTelemetry(message: StripTelemetryMessage) {
      stripsBroadcaster.broadcast(message)
   }

   /**
    * Stops the web server.
    */
   fun stop() {
      server?.stop(1000, 2000)
      server = null
   }

   /**
    * Broadcasts the current viewport to all connected clients.
    */
   suspend fun broadcastViewport() {
      broadcaster.broadcastViewport(viewport)
   }

   /**
    * Handles commands received from web clients.
    */
   private fun handleCommand(commandText: String) {
      try {
         val command = Json.decodeFromString<Command>(commandText)
         when(command.type) {
            "setPattern" -> {
               if (command.pattern.isNotEmpty()) {
                  setPattern(command.pattern, command.params)
               }
            }
            "setFPS" -> {
               // Could implement FPS adjustment here
            }
            else -> {}
         }
      }
      catch (e: Exception) {
         // Ignore malformed commands
      }
   }

   /**
    * Converts a JSON map into a Map<String, Any> with native types.
    */
   private fun parseJsonParams(jsonMap: Map<String, kotlinx.serialization.json.JsonElement>): Map<String, Any> {
      val result = mutableMapOf<String, Any>()
      for ((key, element) in jsonMap) {
         if (element is kotlinx.serialization.json.JsonPrimitive) {
            val content = element.content
            if (element.isString) {
               result[key] = content
            } else if (content == "true") {
               result[key] = true
            } else if (content == "false") {
               result[key] = false
            } else {
               // Try to parse as number (double covers all numeric JSON values)
               content.toDoubleOrNull()?.let { result[key] = it }
            }
         }
      }
      return result
   }

   /**
    * Sets the active pattern with parameters.
    */
   private fun setPattern(name: String, params: Map<String, Any>) {
      val pattern = patternRegistry.get(name)
      if(pattern != null) {
         currentPattern = pattern

         val patternParams = PatternParameters()
         params.forEach { (key, value) ->
            when(value) {
               is Number -> {
                  when {
                     value is Double || value is Float ->
                        patternParams.set(key, value.toFloat())
                     else ->
                        patternParams.set(key, value.toInt())
                  }
               }
               is String -> patternParams.set(key, value)
               is Boolean -> patternParams.set(key, value)
            }
         }

         // Notify listener if set
         patternChangeListener?.invoke(name, patternParams)
      }
   }
}

/**
 * WebSocket message pushed to /ws/strips clients: telemetry reading for a single strip.
 */
@Serializable
data class StripTelemetryMessage(
   val type: String = "strip_telemetry",
   val stripId: Int,
   val status: Int,
   val temperature: Float,
   val current: Float,
   val uptimeMs: Long,
   val frames: Long,
   val history: TelemetryHistory
)

@Serializable
data class TelemetryHistory(
   val temperature: List<Float>,
   val current: List<Float>
)

/**
 * WebSocket message pushed to /ws/strips clients: current strip list snapshot.
 */
@Serializable
data class StripsUpdateMessage(
   val type: String = "strips_update",
   val strips: List<StripStatusResponse>
)

/**
 * WebSocket message pushed to /ws/strips clients: free-text discovery activity entry.
 */
@Serializable
data class DiscoveryEventMessage(
   val type: String = "discovery_event",
   val message: String
)

/**
 * Strip connection status for the frontend.
 */
@Serializable
data class StripStatusResponse(
   val id: Int,
   val name: String,
   val address: String,
   val connected: Boolean,
   val length: Int
)

/**
 * Pattern info with metadata for the frontend.
 */
@Serializable
data class PatternInfo(
   val name: String,
   val description: String,
   val parameters: List<ParameterDef>
)

/**
 * Command received from web client.
 */
@Serializable
data class Command(
   val type: String,
   val pattern: String = "",
   val params: Map<String, String> = emptyMap(),
   val fps: Int = 60
)

/**
 * LED data with position and color for web client.
 */
@Serializable
data class LEDData(
   val x: Int,
   val y: Int,
   val r: Int,
   val g: Int,
   val b: Int
)

/**
 * LED strip data for web client.
 */
@Serializable
data class LEDStripData(
   val id: Int,
   val leds: List<LEDData>
)

// ── Settings API data classes ──────────────────────────────────────────────

@Serializable
data class ScalarSettingsResponse(
   val viewportWidth: Int,
   val viewportHeight: Int,
   val targetFPS: Int,
   val scanIntervalSeconds: Int,
   val telemetryIntervalSeconds: Int
)

@Serializable
data class StripSettingResponse(
   val id: Int,
   val btName: String,
   val length: Int?,
   val startX: Int?,
   val startY: Int?,
   val endX: Int?,
   val endY: Int?,
   val reverse: Boolean
)

@Serializable
data class CreateStripRequest(
   val btName: String,
   val length: Int? = null,
   val startX: Int? = null,
   val startY: Int? = null,
   val endX: Int? = null,
   val endY: Int? = null,
   val reverse: Boolean = false
)

@Serializable
data class UpdateStripRequest(
   val btName: String? = null,
   val length: Int? = null,
   val startX: Int? = null,
   val startY: Int? = null,
   val endX: Int? = null,
   val endY: Int? = null,
   val reverse: Boolean? = null
)

private fun com.timberglund.ledhost.db.StripRow.toResponse() = StripSettingResponse(
   id = id,
   btName = btName,
   length = length,
   startX = startX,
   startY = startY,
   endX = endX,
   endY = endY,
   reverse = reverse
)
