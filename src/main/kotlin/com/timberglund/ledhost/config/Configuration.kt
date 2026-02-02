package com.timberglund.ledhost.config

import com.charleskorn.kaml.Yaml
import com.timberglund.ledhost.viewport.Point
import kotlinx.serialization.Serializable
import java.io.File

/**
 * Main configuration for the LED Host application.
 *
 * @property viewport Viewport dimensions
 * @property strips LED strip layout configurations
 * @property mapper Pixel mapping configuration
 * @property output Output layer configuration
 * @property webServer Web server configuration
 * @property targetFPS Target frames per second (default: 60)
 */
@Serializable
data class Configuration(
   val viewport: ViewportConfig,
   val strips: List<StripLayout>,
   val mapper: MapperConfig = MapperConfig(),
   val output: OutputConfig,
   val webServer: WebServerConfig = WebServerConfig(),
   val targetFPS: Int = 60
) {
   init {
      require(targetFPS > 0) { "Target FPS must be positive, got $targetFPS" }
      require(viewport.width > 0) { "Viewport width must be positive" }
      require(viewport.height > 0) { "Viewport height must be positive" }
      require(strips.isNotEmpty()) { "At least one strip must be configured" }
   }

   companion object {
      /**
      * Loads configuration from a YAML file.
      *
      * @param path Path to the YAML configuration file
      * @return Parsed and validated configuration
      * @throws IllegalArgumentException if configuration is invalid
      * @throws java.io.FileNotFoundException if file doesn't exist
      */
      fun load(path: String): Configuration {
         val file = File(path)
         require(file.exists()) { "Configuration file not found: $path" }
         require(file.canRead()) { "Configuration file not readable: $path" }

         val yaml = Yaml.default
         val content = file.readText()

         return try {
            yaml.decodeFromString(serializer(), content)
         }
         catch (e: Exception) {
            throw IllegalArgumentException("Failed to parse configuration from $path: ${e.message}", e)
         }
      }
   }
}

/**
 * Viewport dimension configuration.
 *
 * @property width Viewport width in pixels
 * @property height Viewport height in pixels
 */
@Serializable
data class ViewportConfig(
   val width: Int,
   val height: Int
) {
   init {
      require(width > 0) { "Viewport width must be positive, got $width" }
      require(height > 0) { "Viewport height must be positive, got $height" }
   }
}

/**
 * LED strip physical layout configuration.
 *
 * @property id Strip identifier
 * @property length Number of LEDs in the strip
 * @property position Start and end positions in viewport
 * @property reverse Whether to reverse the LED order (for serpentine layouts)
 */
@Serializable
data class StripLayout(
   val id: Int,
   val length: Int,
   val position: StripPosition,
   val reverse: Boolean = false
) {
   init {
      require(id >= 0) { "Strip ID must be non-negative, got $id" }
      require(length > 0) { "Strip length must be positive, got $length" }
   }
}

/**
 * Strip position in the viewport.
 *
 * @property start Starting point
 * @property end Ending point
 */
@Serializable
data class StripPosition(
   val start: PointConfig,
   val end: PointConfig
)

/**
 * Point configuration for serialization.
 *
 * @property x X coordinate
 * @property y Y coordinate
 */
@Serializable
data class PointConfig(
   val x: Int,
   val y: Int
) {
   /**
    * Converts to a Point object.
    */
   fun toPoint(): Point = Point(x, y)
}

/**
 * Pixel mapper configuration.
 *
 * @property type Mapper type ("linear", "grid", or "custom")
 * @property columns Number of columns (for grid mapper)
 * @property rows Number of rows (for grid mapper)
 */
@Serializable
data class MapperConfig(
   val type: String = "linear",
   val columns: Int = 1,
   val rows: Int = 1
) {
   init {
      require(type in listOf("linear", "grid", "custom")) {
         "Mapper type must be 'linear', 'grid', or 'custom', got '$type'"
      }
      require(columns > 0) { "Columns must be positive, got $columns" }
      require(rows > 0) { "Rows must be positive, got $rows" }
   }
}

/**
 * Output layer configuration.
 *
 * @property type Output type ("serial," "network," "bluetooth," or "preview")
 * @property parameters Output-specific parameters
 */
@Serializable
data class OutputConfig(
   val type: String,
   val parameters: Map<String, String> = emptyMap()
) {
   init {
      require(type.isNotBlank()) { "Output type cannot be blank" }
   }

   /**
    * Gets a parameter value with a default.
    */
   fun getParameter(key: String, default: String = ""): String {
      return parameters[key] ?: default
   }

   /**
    * Gets an integer parameter.
    */
   fun getIntParameter(key: String, default: Int = 0): Int {
      return parameters[key]?.toIntOrNull() ?: default
   }
}

/**
 * Web server configuration.
 *
 * @property port Server port (default: 8080)
 * @property enabled Whether the web server is enabled (default: true)
 */
@Serializable
data class WebServerConfig(
   val port: Int = 8080,
   val enabled: Boolean = true
) {
   init {
      require(port in 1..65535) { "Port must be between 1 and 65535, got $port" }
   }
}
