package com.timberglund.ledhost

import com.timberglund.ledhost.config.Configuration
import com.timberglund.ledhost.config.StripLayout
import com.timberglund.ledhost.mapper.LinearMapper
import com.timberglund.ledhost.mapper.PixelMapper
import com.timberglund.ledhost.viewport.Color
import com.timberglund.ledhost.viewport.Viewport
import java.nio.ByteBuffer
import java.nio.ByteOrder
import com.timberglund.ledhost.pattern.DefaultPatternRegistry
import com.timberglund.ledhost.pattern.PatternParameters
import com.timberglund.ledhost.pattern.patterns.PlasmaPattern
import com.timberglund.ledstrip.BluetoothTester
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

   // Load configuration
   val configPath = args.firstOrNull() ?: "config.yaml"
   val config = if(Path(configPath).exists()) {
      logger.info { "Loading configuration from: $configPath" }
      Configuration.load(configPath)
   }
   else {
      logger.warn { "Configuration file not found: $configPath" }
      logger.info { "Using default configuration" }
      createDefaultConfiguration()
   }

   logger.info { "Viewport: ${config.viewport.width}x${config.viewport.height}" }
   logger.info { "Target FPS: ${config.targetFPS}" }
   logger.info { "Strips: ${config.strips.size}" }

   // Create viewport
   val viewport = ArrayViewport(config.viewport.width, config.viewport.height)

   // Create pixel mapper for LED strips
   val mapper = LinearMapper(config.strips)

   // Create and populate pattern registry
   val patternRegistry = DefaultPatternRegistry()
   patternRegistry.register(PlasmaPattern())
   patternRegistry.register(RainbowPattern())
   patternRegistry.register(SolidColorPattern())
   logger.info { "Registered patterns: ${patternRegistry.listPatterns().joinToString(", ")}" }

   // Create and start BLE strip manager
   val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
   val bleManager = BluetoothTester()
   appScope.launch {
      bleManager.scanAndConnect()
   }
   bleManager.startBackgroundScanning(appScope, config.scanIntervalSeconds * 1000L)

   // Create preview server (before renderer so we can reference it in the callback)
   var previewServer: PreviewServer? = null

   // Throttle WebSocket broadcasts to reduce bandwidth
   val webSocketFPS = 20 // Broadcast at 20 FPS regardless of render FPS
   var lastBroadcastTime = 0L
   val broadcastIntervalMs = 1000L / webSocketFPS

   // Create frame renderer
   val renderer = FrameRenderer(targetFPS = config.targetFPS,
                                viewport = viewport,
                                onFrameRendered = { renderedViewport ->
         // Build strip frames synchronously while viewport is stable, then send
         val frames = buildStripFrames(renderedViewport, mapper, config.strips)
         GlobalScope.launch {
            for((stripId, frame) in frames) {
               bleManager.sendFrame(stripId, frame)
            }
         }

         // Broadcast to web clients (throttled, non-blocking)
         val now = System.currentTimeMillis()
         if (now - lastBroadcastTime >= broadcastIntervalMs) {
            lastBroadcastTime = now
            GlobalScope.launch {
               previewServer?.broadcastViewport()
            }
         }
      }
   )

   if(config.webServer.enabled) {
      previewServer = PreviewServer(port = config.webServer.port,
                                    viewport = viewport,
                                    patternRegistry = patternRegistry,
                                    renderer = renderer,
                                    configuration = config,
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
 * Must be called while the viewport is stable (i.e. synchronously in the render callback).
 */
private fun buildStripFrames(
   viewport: Viewport,
   mapper: PixelMapper,
   strips: List<StripLayout>): Map<Int, ByteArray> {
   val ledColors = mapper.mapViewportToLEDs(viewport)
   return strips.associate { strip ->
      val leds = Array(strip.length) { Color.BLACK }
      for((address, color) in ledColors) {
         if(address.stripId == strip.id && address.ledIndex in 0 until strip.length) {
            leds[address.ledIndex] = color
         }
      }
      strip.id to buildStripFrame(leds)
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
 * Creates a default configuration for demonstration purposes.
 */
private fun createDefaultConfiguration(): Configuration {
   return Configuration(
      viewport = com.timberglund.ledhost.config.ViewportConfig(60, 3),
      strips = listOf(
         com.timberglund.ledhost.config.StripLayout(
            id = 0,
            length = 60,
            position = com.timberglund.ledhost.config.StripPosition(
               start = com.timberglund.ledhost.config.PointConfig(0, 0),
               end = com.timberglund.ledhost.config.PointConfig(59, 0)
            )
         ),
         com.timberglund.ledhost.config.StripLayout(
            id = 1,
            length = 60,
            position = com.timberglund.ledhost.config.StripPosition(
               start = com.timberglund.ledhost.config.PointConfig(0, 1),
               end = com.timberglund.ledhost.config.PointConfig(59, 1)
            ),
            reverse = true
         ),
         com.timberglund.ledhost.config.StripLayout(
            id = 2,
            length = 60,
            position = com.timberglund.ledhost.config.StripPosition(
               start = com.timberglund.ledhost.config.PointConfig(0, 2),
               end = com.timberglund.ledhost.config.PointConfig(59, 2)
            )
         )
      ),
      output = com.timberglund.ledhost.config.OutputConfig("preview"),
      webServer = com.timberglund.ledhost.config.WebServerConfig(8080, true),
      targetFPS = 60
   )
}
