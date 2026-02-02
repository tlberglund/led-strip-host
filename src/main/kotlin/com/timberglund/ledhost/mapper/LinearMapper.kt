package com.timberglund.ledhost.mapper

import com.timberglund.ledhost.config.StripLayout
import com.timberglund.ledhost.viewport.Color
import com.timberglund.ledhost.viewport.Point
import com.timberglund.ledhost.viewport.Viewport
import kotlin.math.abs

/**
 * Maps viewport pixels to LED strips using linear interpolation along each strip's path.
 *
 * This mapper draws a line from the strip's start point to end point and maps viewport
 * pixels along that line to sequential LED indices. Supports both horizontal, vertical,
 * and diagonal strip layouts.
 *
 * @property layouts List of LED strip configurations
 */
class LinearMapper(private val layouts: List<StripLayout>) : PixelMapper {

   // Pre-computed mapping from viewport coordinates to LED addresses
   private val mapping: Map<Point, LEDAddress>

   init {
      mapping = buildMapping()
   }

   /**
    * Builds the complete mapping from viewport points to LED addresses.
    */
   private fun buildMapping(): Map<Point, LEDAddress> {
      val map = mutableMapOf<Point, LEDAddress>()

      for(layout in layouts) {
         val start = layout.position.start.toPoint()
         val end = layout.position.end.toPoint()

         // Calculate the points along the line from start to end
         val points = bresenhamLine(start, end)

         // Ensure we have exactly 'length' LEDs
         val scaledPoints = if (points.size != layout.length) {
            interpolatePoints(start, end, layout.length)
         }
         else {
            points
         }

         // Map each point to an LED address
         for(i in scaledPoints.indices) {
            val ledIndex = if (layout.reverse) {
               layout.length - 1 - i
            } else {
               i
            }

            val point = scaledPoints[i]
            map[point] = LEDAddress(layout.id, ledIndex)
         }
      }

      return map
   }

   /**
    * Uses Bresenham's line algorithm to get all points along a line.
    */
   private fun bresenhamLine(start: Point, end: Point): List<Point> {
      val points = mutableListOf<Point>()
      var x0 = start.x
      var y0 = start.y
      val x1 = end.x
      val y1 = end.y

      val dx = abs(x1 - x0)
      val dy = abs(y1 - y0)
      val sx = if (x0 < x1) 1 else -1
      val sy = if (y0 < y1) 1 else -1
      var err = dx - dy

      while(true) {
         points.add(Point(x0, y0))

         if(x0 == x1 && y0 == y1) break

         val e2 = 2 * err
         if(e2 > -dy) {
            err -= dy
            x0 += sx
         }
         if(e2 < dx) {
            err += dx
            y0 += sy
         }
      }

      return points
   }

   /**
    * Interpolates exactly N points along the line from start to end.
    */
   private fun interpolatePoints(start: Point, end: Point, count: Int): List<Point> {
      require(count > 0) { "Count must be positive" }

      if(count == 1) {
         return listOf(start)
      }

      val points = mutableListOf<Point>()
      for(i in 0 until count) {
         val t = i.toFloat() / (count - 1)
         val x = (start.x + t * (end.x - start.x)).toInt()
         val y = (start.y + t * (end.y - start.y)).toInt()
         points.add(Point(x, y))
      }

      return points
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
}
