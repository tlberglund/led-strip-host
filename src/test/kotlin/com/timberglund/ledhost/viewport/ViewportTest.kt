package com.timberglund.ledhost.viewport

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertFailsWith

class ViewportTest {

   @Test
   fun `creates viewport with valid dimensions`() {
      val viewport = ArrayViewport(10, 5)
      assertEquals(10, viewport.width)
      assertEquals(5, viewport.height)
   }

   @Test
   fun `rejects invalid dimensions`() {
      assertFailsWith<IllegalArgumentException> { ArrayViewport(0, 5) }
      assertFailsWith<IllegalArgumentException> { ArrayViewport(10, 0) }
      assertFailsWith<IllegalArgumentException> { ArrayViewport(-1, 5) }
      assertFailsWith<IllegalArgumentException> { ArrayViewport(10, -1) }
   }

   @Test
   fun `initializes with black pixels`() {
      val viewport = ArrayViewport(10, 10)
      for(y in 0 until viewport.height) {
         for(x in 0 until viewport.width) {
               assertEquals(Color.BLACK, viewport.getPixel(x, y))
         }
      }
   }

   @Test
   fun `sets and gets pixel correctly`() {
      val viewport = ArrayViewport(10, 10)
      val color = Color(255, 128, 64)

      viewport.setPixel(5, 3, color)
      assertEquals(color, viewport.getPixel(5, 3))
   }

   @Test
   fun `ignores out of bounds setPixel`() {
      val viewport = ArrayViewport(10, 10)
      val color = Color(255, 0, 0)

      // Should not throw exceptions
      viewport.setPixel(-1, 5, color)
      viewport.setPixel(5, -1, color)
      viewport.setPixel(10, 5, color)
      viewport.setPixel(5, 10, color)
   }

   @Test
   fun `returns BLACK for out of bounds getPixel`() {
      val viewport = ArrayViewport(10, 10)

      assertEquals(Color.BLACK, viewport.getPixel(-1, 5))
      assertEquals(Color.BLACK, viewport.getPixel(5, -1))
      assertEquals(Color.BLACK, viewport.getPixel(10, 5))
      assertEquals(Color.BLACK, viewport.getPixel(5, 10))
   }

   @Test
   fun `fill colors entire viewport`() {
      val viewport = ArrayViewport(10, 10)
      val color = Color(100, 150, 200)

      viewport.fill(color)

      for(y in 0 until viewport.height) {
         for(x in 0 until viewport.width) {
               assertEquals(color, viewport.getPixel(x, y))
         }
      }
   }

   @Test
   fun `clear fills viewport with black`() {
      val viewport = ArrayViewport(10, 10)

      // First fill with a color
      viewport.fill(Color.RED)
      // Then clear
      viewport.clear()

      for(y in 0 until viewport.height) {
         for(x in 0 until viewport.width) {
            assertEquals(Color.BLACK, viewport.getPixel(x, y))
         }
      }
   }

   @Test
   fun `draws horizontal line`() {
      val viewport = ArrayViewport(10, 10)
      val color = Color.RED

      viewport.drawLine(2, 5, 7, 5, color)

      for(x in 2..7) {
         assertEquals(color, viewport.getPixel(x, 5))
      }
   }

   @Test
   fun `draws vertical line`() {
      val viewport = ArrayViewport(10, 10)
      val color = Color.GREEN

      viewport.drawLine(5, 2, 5, 7, color)

      for(y in 2..7) {
         assertEquals(color, viewport.getPixel(5, y))
      }
   }

   @Test
   fun `draws diagonal line`() {
      val viewport = ArrayViewport(10, 10)
      val color = Color.BLUE

      viewport.drawLine(0, 0, 9, 9, color)

      // Check that diagonal pixels are set (approximately)
      for(i in 0..9) {
         assertEquals(color, viewport.getPixel(i, i))
      }
   }

   @Test
   fun `draws filled rectangle`() {
      val viewport = ArrayViewport(20, 20)
      val color = Color.RED

      viewport.drawRect(5, 5, 6, 4, color, filled = true)

      // Check inside the rectangle
      for(y in 5 until 9) {
         for(x in 5 until 11) {
            assertEquals(color, viewport.getPixel(x, y), "Expected RED at ($x, $y)")
         }
      }

      // Check outside the rectangle
      assertEquals(Color.BLACK, viewport.getPixel(4, 5))
      assertEquals(Color.BLACK, viewport.getPixel(11, 5))
      assertEquals(Color.BLACK, viewport.getPixel(5, 4))
      assertEquals(Color.BLACK, viewport.getPixel(5, 9))
   }

   @Test
   fun `draws rectangle outline`() {
      val viewport = ArrayViewport(20, 20)
      val color = Color.GREEN

      viewport.drawRect(5, 5, 6, 4, color, filled = false)

      // Check top edge
      for(x in 5 until 11) {
         assertEquals(color, viewport.getPixel(x, 5), "Expected GREEN at top edge ($x, 5)")
      }

      // Check bottom edge
      for(x in 5 until 11) {
         assertEquals(color, viewport.getPixel(x, 8), "Expected GREEN at bottom edge ($x, 8)")
      }

      // Check left edge
      for(y in 5 until 9) {
         assertEquals(color, viewport.getPixel(5, y), "Expected GREEN at left edge (5, $y)")
      }

      // Check right edge
      for(y in 5 until 9) {
         assertEquals(color, viewport.getPixel(10, y), "Expected GREEN at right edge (10, $y)")
      }

      // Check inside is still black
      assertEquals(Color.BLACK, viewport.getPixel(6, 6))
      assertEquals(Color.BLACK, viewport.getPixel(7, 7))
   }

   @Test
   fun `draws circle outline`() {
      val viewport = ArrayViewport(20, 20)
      val color = Color.BLUE

      viewport.drawCircle(10, 10, 5, color, filled = false)

      // Check that some expected circle points are colored
      assertEquals(color, viewport.getPixel(10, 5))  // Top
      assertEquals(color, viewport.getPixel(10, 15)) // Bottom
      assertEquals(color, viewport.getPixel(5, 10))  // Left
      assertEquals(color, viewport.getPixel(15, 10)) // Right

      // Check that center is still black (not filled)
      assertEquals(Color.BLACK, viewport.getPixel(10, 10))
   }

   @Test
   fun `draws filled circle`() {
      val viewport = ArrayViewport(20, 20)
      val color = Color.RED

      viewport.drawCircle(10, 10, 3, color, filled = true)

      // Check that center is colored
      assertEquals(color, viewport.getPixel(10, 10))

      // Check some points inside the circle
      assertEquals(color, viewport.getPixel(10, 9))
      assertEquals(color, viewport.getPixel(10, 11))
      assertEquals(color, viewport.getPixel(9, 10))
      assertEquals(color, viewport.getPixel(11, 10))
   }

   @Test
   fun `setPixels batch operation works`() {
      val viewport = ArrayViewport(10, 10)
      val pixels = listOf(
         Point(0, 0) to Color.RED,
         Point(1, 1) to Color.GREEN,
         Point(2, 2) to Color.BLUE
      )

      viewport.setPixels(pixels)

      assertEquals(Color.RED, viewport.getPixel(0, 0))
      assertEquals(Color.GREEN, viewport.getPixel(1, 1))
      assertEquals(Color.BLUE, viewport.getPixel(2, 2))
   }

   @Test
   fun `getBuffer returns copy of buffer`() {
      val viewport = ArrayViewport(5, 5)
      viewport.setPixel(2, 3, Color.RED)

      val buffer1 = viewport.getBuffer()
      val buffer2 = viewport.getBuffer()

      // Should be different instances (copies)
      assertNotSame(buffer1, buffer2)

      // But should have the same content
      assertEquals(buffer1[3][2], buffer2[3][2])
      assertEquals(Color.RED.toInt(), buffer1[3][2])
   }

   @Test
   fun `getBuffer modifications do not affect viewport`() {
      val viewport = ArrayViewport(5, 5)
      viewport.setPixel(2, 3, Color.RED)

      val buffer = viewport.getBuffer()
      buffer[3][2] = Color.BLUE.toInt()

      // Original viewport should still have RED
      assertEquals(Color.RED, viewport.getPixel(2, 3))
   }

   @Test
   fun `buffer format is correct`() {
      val viewport = ArrayViewport(5, 5)
      val color = Color(255, 128, 64)
      viewport.setPixel(2, 3, color)

      val buffer = viewport.getBuffer()

      // Buffer should be [y][x]
      assertEquals(5, buffer.size) // height
      assertEquals(5, buffer[0].size) // width
      assertEquals(color.toInt(), buffer[3][2])
   }

   @Test
   fun `drawing respects viewport bounds`() {
      val viewport = ArrayViewport(10, 10)
      val color = Color.RED

      // Draw line that extends beyond viewport
      viewport.drawLine(-5, 5, 15, 5, color)

      // Only pixels within bounds should be set
      assertEquals(color, viewport.getPixel(0, 5))
      assertEquals(color, viewport.getPixel(9, 5))

      // Draw rectangle partially outside
      viewport.drawRect(-5, -5, 10, 10, Color.GREEN, filled = true)

      // Should only affect visible portion
      assertEquals(Color.GREEN, viewport.getPixel(0, 0))
   }
}
