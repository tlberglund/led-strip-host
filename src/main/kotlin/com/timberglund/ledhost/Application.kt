package com.timberglund.ledhost

import com.timberglund.ledhost.config.Configuration
import com.timberglund.ledhost.config.StripLayout
import com.timberglund.ledhost.db.SettingsRepository
import com.timberglund.ledhost.mapper.LinearMapper
import com.timberglund.ledhost.mapper.PixelMapper
import com.timberglund.ledhost.viewport.Color
import com.timberglund.ledhost.viewport.Viewport
import java.nio.ByteBuffer
import java.nio.ByteOrder
import com.timberglund.ledhost.pattern.DefaultPatternRegistry
import com.timberglund.ledhost.pattern.PatternParameters
import com.timberglund.ledhost.pattern.patterns.PlasmaPattern
import com.timberglund.ledstrip.BluetoothHost
import com.timberglund.ledhost.pattern.patterns.RainbowPattern
import com.timberglund.ledhost.pattern.patterns.SolidColorPattern
import com.timberglund.ledhost.renderer.FrameRenderer
import com.timberglund.ledhost.viewport.ArrayViewport
import com.timberglund.ledhost.web.PreviewServer
import kotlinx.coroutines.*
import mu.KotlinLogging
import kotlin.io.path.Path
import kotlin.io.path.exists

private val logger = KotlinLogging.logger {}

/**
 * Main application entry point for the LED Strip Host.
 */
fun main(args: Array<String>) {
   logger.info { "LED Strip Host Application" }
   logger.info { "=".repeat(40) }

   // Load configuration (only port and DB connection fields are required)
   val configPath = args.firstOrNull() ?: "config.yaml"
   val config = if(Path(configPath).exists()) {
      logger.info { "Loading configuration from: $configPath" }
      Configuration.load(configPath)
   }
   else {
      logger.info { "No configuration file found at $configPath — using defaults" }
      Configuration()
   }

   // Connect to database and seed on first run
   val settingsRepository = SettingsRepository()
   try {
      settingsRepository.connect(config.databaseUrl, config.databaseUser, config.databasePassword)
   }
   catch(e: Exception) {
      logger.error { "Cannot start: database unreachable at ${config.databaseUrl} — ${e.message}" }
      System.exit(1)
   }
   settingsRepository.seedFromConfig(config)

   // Load runtime settings from database (blocking reads at startup)
   val (viewportWidth, viewportHeight, targetFPS) = runBlocking {
      val w = settingsRepository.getSetting("viewportWidth")?.toIntOrNull() ?: config.viewport.width
      val h = settingsRepository.getSetting("viewportHeight")?.toIntOrNull() ?: config.viewport.height
      val fps = settingsRepository.getSetting("targetFPS")?.toIntOrNull() ?: config.targetFPS
      Triple(w, h, fps)
   }

   logger.info { "Viewport: ${viewportWidth}x${viewportHeight}" }
   logger.info { "Target FPS: $targetFPS" }

   // Load strips from DB and build StripLayout list for mapper + renderer
   val dbStrips = runBlocking { settingsRepository.getAllStrips() }
   val loadedStrips = dbStrips.map { strip ->
      com.timberglund.ledhost.config.StripLayout(
         id = strip.id,
         length = strip.length ?: 0,
         position = com.timberglund.ledhost.config.StripPosition(
            start = com.timberglund.ledhost.config.PointConfig(strip.startX ?: 0, strip.startY ?: 0),
            end = com.timberglund.ledhost.config.PointConfig(strip.endX ?: 0, strip.endY ?: 0)
         ),
         reverse = strip.reverse
      )
   }
   logger.info { "Loaded ${loadedStrips.size} strip(s) from database" }

   // Create viewport
   val viewport = ArrayViewport(viewportWidth, viewportHeight)

   // Create pixel mapper for LED strips
   val mapper = LinearMapper(loadedStrips)

   // Create and populate pattern registry
   val patternRegistry = DefaultPatternRegistry()
   patternRegistry.register(PlasmaPattern())
   patternRegistry.register(RainbowPattern())
   patternRegistry.register(SolidColorPattern())
   logger.info { "Registered patterns: ${patternRegistry.listPatterns().joinToString(", ")}" }

   // Read scan and telemetry intervals from DB
   val (scanIntervalSeconds, telemetryIntervalSeconds) = runBlocking {
      val scan = settingsRepository.getSetting("scanIntervalSeconds")?.toIntOrNull() ?: 15
      val telemetry = settingsRepository.getSetting("telemetryIntervalSeconds")?.toIntOrNull() ?: 5
      Pair(scan, telemetry)
   }

   // Create and start BLE strip manager; seed with known strips from DB
   val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
   val bleManager = BluetoothHost()
   bleManager.seedKnownStrips(dbStrips.map { it.id to it.btName })
   appScope.launch {
      bleManager.scanAndConnect()
   }
   bleManager.startBackgroundScanning(appScope, scanIntervalSeconds * 1000L)

   // Create preview server (before renderer so we can reference it in the callback)
   var previewServer: PreviewServer? = null

   // Throttle WebSocket broadcasts to reduce bandwidth
   val webSocketFPS = 20 // Broadcast at 20 FPS regardless of render FPS
   var lastBroadcastTime = 0L
   val broadcastIntervalMs = 1000L / webSocketFPS

   // Create frame renderer
   val renderer = FrameRenderer(targetFPS = targetFPS,
                                viewport = viewport,
                                onFrameRendered = { renderedViewport ->
         // Build strip frames synchronously while viewport is stable, then send
         val frames = buildStripFrames(renderedViewport, mapper, loadedStrips)
         appScope.launch {
            for((stripId, pair) in frames) {
               bleManager.sendFrame(stripId, pair.first)
            }
         }

         // Broadcast to web clients (throttled, non-blocking)
         val now = System.currentTimeMillis()
         if (now - lastBroadcastTime >= broadcastIntervalMs) {
            lastBroadcastTime = now
            appScope.launch {
               previewServer?.broadcastViewport()
               previewServer?.broadcastLeds(frames.map { (stripId, pair) ->
                  com.timberglund.ledhost.web.StripLedsMessage(
                     stripId = stripId,
                     rgb = toRgbHex(pair.second)
                  )
               })
            }
         }
      }
   )

   if(config.webServer.enabled) {
      previewServer = PreviewServer(port = config.webServer.port,
                                    viewport = viewport,
                                    patternRegistry = patternRegistry,
                                    renderer = renderer,
                                    settingsRepository = settingsRepository,
                                    mapper = mapper,
                                    bleManager = bleManager)

      // Handle pattern changes from web interface
      previewServer.setPatternChangeListener { patternName, params ->
         val pattern = patternRegistry.get(patternName)
         if(pattern != null) {
            renderer.setPattern(pattern, params)
         }
      }

      previewServer.start()

      // Wire telemetry polling: store readings and broadcast to /ws/strips clients
      val telemetryStore = TelemetryStore()
      bleManager.startTelemetryPolling(appScope, telemetryIntervalSeconds * 1000L) { stripId, reading ->
         telemetryStore.record(stripId, reading)
         val history = telemetryStore.getHistory(stripId)
         val msg = com.timberglund.ledhost.web.StripTelemetryMessage(
            stripId = stripId,
            status = reading.status,
            temperature = reading.temperature,
            current = reading.current,
            uptimeMs = reading.uptimeMs,
            frames = reading.frames,
            history = com.timberglund.ledhost.web.TelemetryHistory(
               temperature = history.map { it.temperature },
               current = history.map { it.current }
            )
         )
         appScope.launch { previewServer.broadcastTelemetry(msg) }
      }
   }

   // Set initial pattern
   val initialPattern = patternRegistry.get("Rainbow")
   if(initialPattern != null) {
      logger.info { "Starting with Rainbow pattern" }
      renderer.setPattern(initialPattern, PatternParameters())
   }

   // Start rendering
   renderer.start()

   // Log status
   logger.info { "Application started successfully!" }
   if(config.webServer.enabled) {
      logger.info { "Web preview: http://localhost:${config.webServer.port}" }
   }
   logger.info { "Press Ctrl+C to stop" }

   // Keep application running
   Runtime.getRuntime().addShutdownHook(Thread {
      logger.info { "Shutting down..." }
      renderer.stop()
      previewServer?.stop()
      appScope.cancel()
      runBlocking { bleManager.disconnectAll() }
      logger.info { "Goodbye!" }
   })

   // Wait forever (until Ctrl+C)
   Thread.currentThread().join()
}

/**
 * Builds BLE frame bytes for every configured strip from the current viewport.
 * Also returns the per-LED color arrays for use by the LED display broadcaster.
 * Must be called while the viewport is stable (i.e. synchronously in the render callback).
 */
private fun buildStripFrames(
   viewport: Viewport,
   mapper: PixelMapper,
   strips: List<StripLayout>): Map<Int, Pair<ByteArray, Array<Color>>> {
   val ledColors = mapper.mapViewportToLEDs(viewport)
   return strips.associate { strip ->
      val leds = Array(strip.length) { Color.BLACK }
      for((address, color) in ledColors) {
         if(address.stripId == strip.id && address.ledIndex in 0 until strip.length) {
            leds[address.ledIndex] = color
         }
      }
      strip.id to Pair(buildStripFrame(leds), leds)
   }
}

/**
 * Serializes an array of colors into the BLE frame format:
 * 2-byte command (0x0001), 2-byte LED count, then per-LED: brightness, R, G, B.
 */
private fun buildStripFrame(leds: Array<Color>): ByteArray =
   ByteBuffer.allocate(4 + leds.size * 4)
      .apply {
         order(ByteOrder.LITTLE_ENDIAN)
         putShort(0x0001)
         putShort(leds.size.toShort())
         for(led in leds) {
            put(led.brightness.toByte())
            put(led.r.toByte())
            put(led.g.toByte())
            put(led.b.toByte())
         }
      }
      .array()

/**
 * Encodes an array of LED colors as a compact lowercase hex string (RRGGBB per LED, no separators).
 */
private fun toRgbHex(leds: Array<Color>): String {
   val sb = StringBuilder(leds.size * 6)
   for(led in leds) {
      sb.append(String.format("%02x%02x%02x", led.r, led.g, led.b))
   }
   return sb.toString()
}

