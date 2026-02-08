package com.timberglund.ledhost.web

import com.timberglund.ledhost.config.Configuration
import com.timberglund.ledhost.mapper.PixelMapper
import com.timberglund.ledhost.pattern.Pattern
import com.timberglund.ledhost.pattern.PatternParameters
import com.timberglund.ledhost.pattern.PatternRegistry
import com.timberglund.ledhost.renderer.FrameRenderer
import com.timberglund.ledhost.renderer.RenderStats
import com.timberglund.ledhost.viewport.Viewport
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Duration

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
                    private val configuration: Configuration,
                    private val mapper: PixelMapper) {
   private val broadcaster = WebSocketBroadcaster()
   private var server: ApplicationEngine? = null
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
            pingPeriod = Duration.ofSeconds(15)
            timeout = Duration.ofSeconds(15)
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
            static("/") {
               resources("web")
               defaultResource("web/index.html")
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

            // REST API endpoints
            get("/api/patterns") {
               call.respond(patternRegistry.listPatterns())
            }

            post("/api/pattern/{name}") {
               val name = call.parameters["name"]
               if (name == null) {
                  call.respond(HttpStatusCode.BadRequest, "Pattern name required")
                  return@post
               }

               val params = try {
                  val received = call.receive<PatternParams>()

                  // Convert PatternParams to Map<String, Any>
                  val paramsMap = mutableMapOf<String, Any>()
                  received.speed?.let { paramsMap["speed"] = it }
                  received.brightness?.let { paramsMap["brightness"] = it }
                  received.direction?.let { paramsMap["direction"] = it }
                  received.saturation?.let { paramsMap["saturation"] = it }

                  paramsMap
               }
               catch (e: Exception) {
                  emptyMap()
               }

               setPattern(name, params)
               call.respond(HttpStatusCode.OK)
            }

            get("/api/config") {
               call.respond(configuration)
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

            get("/api/background-image") {
               val imagePath = configuration.backgroundImage
               if (imagePath.isNotEmpty()) {
                  val file = File(imagePath)
                  if (file.exists() && file.isFile) {
                     call.respondFile(file)
                  } else {
                     call.respond(HttpStatusCode.NotFound, "Background image not found: $imagePath")
                  }
               } else {
                  call.respond(HttpStatusCode.NotFound, "No background image configured")
               }
            }
         }
      }

      server?.start(wait = false)
      println("Preview server running at http://localhost:$port")
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
 * Pattern parameters received from web client.
 */
@Serializable
data class PatternParams(
   val speed: Double? = null,
   val brightness: Double? = null,
   val direction: String? = null,
   val saturation: Double? = null
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
