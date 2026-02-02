package com.timberglund.ledhost.mapper

import com.timberglund.ledhost.config.StripLayout
import com.timberglund.ledhost.viewport.Color
import com.timberglund.ledhost.viewport.Point
import com.timberglund.ledhost.viewport.Viewport

/**
 * Maps viewport pixels to LED strips arranged in a grid pattern.
 *
 * Supports serpentine (zigzag) layouts where alternating rows reverse direction.
 * This is the most common physical arrangement for LED matrices.
 *
 * Example 3x3 grid with serpentine enabled:
 * ```
 * 0 → 1 → 2
 *         ↓
 * 5 ← 4 ← 3
 * ↓
 * 6 → 7 → 8
 * ```
 *
 * @property layouts List of LED strip configurations
 * @property columns Number of columns in the grid
 * @property rows Number of rows in the grid
 */
class GridMapper(
   private val layouts: List<StripLayout>,
   private val columns: Int,
   private val rows: Int
) : PixelMapper {

   // Pre-computed mapping from viewport coordinates to LED addresses
   private val mapping: Map<Point, LEDAddress>

   init {
      require(columns > 0) { "Columns must be positive, got $columns" }
      require(rows > 0) { "Rows must be positive, got $rows" }

      mapping = buildMapping()
   }

   /**
    * Builds the complete mapping from viewport points to LED addresses using grid layout.
    */
   private fun buildMapping(): Map<Point, LEDAddress> {
      val map = mutableMapOf<Point, LEDAddress>()

      // Group layouts by row (assuming horizontal strips)
      val sortedLayouts = layouts.sortedBy { it.position.start.y }

      for((rowIndex, layout) in sortedLayouts.withIndex()) {
         val start = layout.position.start
         val end = layout.position.end

         // Determine if this row goes left-to-right or right-to-left
         val isReversed = layout.reverse

         // Map each column position to an LED index
         for(col in 0 until columns) {
            val ledIndex = if (isReversed) {
               layout.length - 1 - col
            }
            else {
               col
            }

            // Calculate the actual viewport coordinate
            val x = if(start.x <= end.x) {
               start.x + col
            }
            else {
               start.x - col
            }
            val y = start.y

            // Only map if within bounds
            if(x >= 0 && x < columns && y >= 0 && y < rows && ledIndex < layout.length) {
               map[Point(x, y)] = LEDAddress(layout.id, ledIndex)
            }
         }
      }

      return map
   }

   override fun mapViewportToLEDs(viewport: Viewport): Map<LEDAddress, Color> {
      val ledColors = mutableMapOf<LEDAddress, Color>()

      for((point, ledAddress) in mapping) {
         // Check if point is within viewport bounds
         if(point.x in 0 until viewport.width && point.y in 0 until viewport.height) {
            val color = viewport.getPixel(point.x, point.y)
            ledColors[ledAddress] = color
         }
      }

      return ledColors
   }

   override fun getMapping(): Map<Point, LEDAddress> {
      return mapping
   }

   /**
    * Gets the LED address for a specific grid position.
    *
    * @param column Column index (0-based)
    * @param row Row index (0-based)
    * @return LED address or null if position is not mapped
    */
   fun getLEDAt(column: Int, row: Int): LEDAddress? {
      return mapping[Point(column, row)]
   }
}
