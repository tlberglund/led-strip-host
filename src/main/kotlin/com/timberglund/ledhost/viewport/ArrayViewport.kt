package com.timberglund.ledhost.viewport

import kotlin.math.abs

/**
 * Array-based implementation of Viewport using a 2D integer array for pixel storage.
 * Each pixel is stored as an integer in 0xRRGGBB format.
 *
 * @property width Width of the viewport in pixels
 * @property height Height of the viewport in pixels
 */
class ArrayViewport(
   override val width: Int,
   override val height: Int
) : Viewport {

   init {
      require(width > 0) { "Width must be positive, got $width" }
      require(height > 0) { "Height must be positive, got $height" }
   }

   // Pixel buffer stored as [y][x] = color integer
   private val buffer: Array<IntArray> = Array(height) { IntArray(width) }

   override fun setPixel(x: Int, y: Int, color: Color) {
      if(x in 0 until width && y in 0 until height) {
         buffer[y][x] = color.toInt()
      }
   }

   override fun getPixel(x: Int, y: Int): Color {
      if(x !in 0 until width || y !in 0 until height) {
         return Color.BLACK
      }
      val colorInt = buffer[y][x]
      return Color(
         r = (colorInt shr 16) and 0xFF,
         g = (colorInt shr 8) and 0xFF,
         b = colorInt and 0xFF
      )
   }

   override fun fill(color: Color) {
      val colorInt = color.toInt()
      for(y in 0 until height) {
         for(x in 0 until width) {
            buffer[y][x] = colorInt
         }
      }
   }

   override fun clear() {
      fill(Color.BLACK)
   }

   override fun drawLine(x1: Int, y1: Int, x2: Int, y2: Int, color: Color) {
      // Bresenham's line algorithm
      var x = x1
      var y = y1
      val dx = abs(x2 - x1)
      val dy = abs(y2 - y1)
      val sx = if (x1 < x2) 1 else -1
      val sy = if (y1 < y2) 1 else -1
      var err = dx - dy

      while(true) {
         setPixel(x, y, color)

         if (x == x2 && y == y2) break

         val e2 = 2 * err
         if(e2 > -dy) {
            err -= dy
            x += sx
         }
         if(e2 < dx) {
            err += dx
            y += sy
         }
      }
   }

   override fun drawRect(x: Int, y: Int, width: Int, height: Int, color: Color, filled: Boolean) {
      if(filled) {
         // Fill the rectangle
         for(dy in 0 until height) {
            for(dx in 0 until width) {
               setPixel(x + dx, y + dy, color)
            }
         }
      } else {
         // Draw outline
         // Top and bottom edges
         for(dx in 0 until width) {
            setPixel(x + dx, y, color)
            setPixel(x + dx, y + height - 1, color)
         }
         // Left and right edges
         for(dy in 0 until height) {
            setPixel(x, y + dy, color)
            setPixel(x + width - 1, y + dy, color)
         }
      }
   }

   override fun drawCircle(cx: Int, cy: Int, radius: Int, color: Color, filled: Boolean) {
      if(radius < 0) return

      if(filled) {
         // Filled circle using midpoint algorithm for each row
         var x = 0
         var y = radius
         var d = 1 - radius

         while(x <= y) {
               // Draw horizontal lines for each octant
               drawHorizontalLine(cx - x, cx + x, cy + y, color)
               drawHorizontalLine(cx - x, cx + x, cy - y, color)
               drawHorizontalLine(cx - y, cx + y, cy + x, color)
               drawHorizontalLine(cx - y, cx + y, cy - x, color)

               x++
               if(d < 0) {
                  d += 2 * x + 1
               } 
               else {
                  y--
                  d += 2 * (x - y) + 1
               }
         }
      } 
      else {
         // Circle outline using midpoint circle algorithm
         var x = 0
         var y = radius
         var d = 1 - radius

         while(x <= y) {
            // Draw 8 octants
            setPixel(cx + x, cy + y, color)
            setPixel(cx - x, cy + y, color)
            setPixel(cx + x, cy - y, color)
            setPixel(cx - x, cy - y, color)
            setPixel(cx + y, cy + x, color)
            setPixel(cx - y, cy + x, color)
            setPixel(cx + y, cy - x, color)
            setPixel(cx - y, cy - x, color)

            x++
            if(d < 0) {
               d += 2 * x + 1
            } 
            else {
               y--
               d += 2 * (x - y) + 1
            }
         }
      }
   }

   /**
    * Helper function to draw a horizontal line (used for filled circles).
    */
   private fun drawHorizontalLine(x1: Int, x2: Int, y: Int, color: Color) {
      val startX = maxOf(0, minOf(x1, x2))
      val endX = minOf(width - 1, maxOf(x1, x2))
      for(x in startX..endX) {
         setPixel(x, y, color)
      }
   }

   override fun setPixels(pixels: List<Pair<Point, Color>>) {
      pixels.forEach { (point, color) ->
         setPixel(point.x, point.y, color)
      }
   }

   override fun getBuffer(): Array<IntArray> {
      // Return a deep copy of the buffer
      return Array(height) { y ->
         buffer[y].copyOf()
      }
   }
}
