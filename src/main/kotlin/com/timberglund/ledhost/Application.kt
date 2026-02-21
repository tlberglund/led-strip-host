package com.timberglund.ledhost

import com.timberglund.ledhost.config.Configuration
import com.timberglund.ledhost.mapper.LinearMapper
import com.timberglund.ledhost.pattern.DefaultPatternRegistry
import com.timberglund.ledhost.pattern.PatternParameters
import com.timberglund.ledhost.pattern.patterns.PlasmaPattern
import com.timberglund.ledhost.pattern.patterns.RainbowPattern
import com.timberglund.ledhost.pattern.patterns.SolidColorPattern
import com.timberglund.ledhost.renderer.FrameRenderer
import com.timberglund.ledhost.viewport.ArrayViewport
import com.timberglund.ledhost.web.PreviewServer
import kotlinx.coroutines.*
import kotlin.io.path.Path
import kotlin.io.path.exists

/**
 * Main application entry point for the LED Strip Host.
 */
fun main(args: Array<String>) {
   println("LED Strip Host Application")
   println("=".repeat(40))

   // Load configuration
   val configPath = args.firstOrNull() ?: "config.yaml"
   val config = if(Path(configPath).exists()) {
      println("Loading configuration from: $configPath")
      Configuration.load(configPath)
   }
   else {
      println("Configuration file not found: $configPath")
      println("Using default configuration")
      createDefaultConfiguration()
   }

   println("Viewport: ${config.viewport.width}x${config.viewport.height}")
   println("Target FPS: ${config.targetFPS}")
   println("Strips: ${config.strips.size}")
   println()

   // Create viewport
   val viewport = ArrayViewport(config.viewport.width, config.viewport.height)

   // Create pixel mapper for LED strips
   val mapper = LinearMapper(config.strips)

   // Create and populate pattern registry
   val patternRegistry = DefaultPatternRegistry()
   patternRegistry.register(PlasmaPattern())
   patternRegistry.register(RainbowPattern())
   patternRegistry.register(SolidColorPattern())
   println("Registered patterns: ${patternRegistry.listPatterns().joinToString(", ")}")
   println()

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
                                    mapper = mapper)

      // Handle pattern changes from web interface
      previewServer.setPatternChangeListener { patternName, params ->
         val pattern = patternRegistry.get(patternName)
         if(pattern != null) {
            renderer.setPattern(pattern, params)
         }
      }

      previewServer.start()
      println()
   }

   // Set initial pattern
   val initialPattern = patternRegistry.get("Rainbow")
   if(initialPattern != null) {
      println("Starting with Rainbow pattern")
      renderer.setPattern(initialPattern, PatternParameters())
   }

   // Start rendering
   renderer.start()

   // Print status
   println()
   println("Application started successfully!")
   if(config.webServer.enabled) {
      println("Web preview: http://localhost:${config.webServer.port}")
   }
   println()
   println("Press Ctrl+C to stop")

   // Keep application running
   Runtime.getRuntime().addShutdownHook(Thread {
      println("\nShutting down...")
      renderer.stop()
      previewServer?.stop()
      println("Goodbye!")
   })

   // Wait forever (until Ctrl+C)
   Thread.currentThread().join()
}

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
