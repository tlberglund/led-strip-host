package com.timberglund.ledhost.mapper

import com.timberglund.ledhost.viewport.Color
import com.timberglund.ledhost.viewport.Point
import com.timberglund.ledhost.viewport.Viewport

/**
 * Represents a specific LED on a physical strip.
 *
 * @property stripId The ID of the LED strip
 * @property ledIndex The position/index of the LED on that strip (0-based)
 */
data class LEDAddress(
   val stripId: Int,
   val ledIndex: Int
) {
   init {
      require(stripId >= 0) { "Strip ID must be non-negative, got $stripId" }
      require(ledIndex >= 0) { "LED index must be non-negative, got $ledIndex" }
   }
}

/**
 * Maps 2D viewport coordinates to physical LED strip positions.
 *
 * This interface allows different mapping strategies (linear, grid, custom)
 * to translate the 2D graphics viewport into actual LED addresses.
 */
interface PixelMapper {
   /**
    * Maps all pixels in the viewport to their corresponding LED addresses.
    *
    * @param viewport The viewport containing pixel colors
    * @return Map from LED address to color value
    */
   fun mapViewportToLEDs(viewport: Viewport): Map<LEDAddress, Color>

   /**
    * Gets the complete mapping from viewport coordinates to LED addresses.
    *
    * @return Map from viewport Point to LED address
    */
   fun getMapping(): Map<Point, LEDAddress>
}
