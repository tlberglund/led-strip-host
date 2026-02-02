package com.timberglund.ledhost.mapper

import com.timberglund.ledhost.config.PointConfig
import com.timberglund.ledhost.config.StripLayout
import com.timberglund.ledhost.config.StripPosition
import com.timberglund.ledhost.viewport.ArrayViewport
import com.timberglund.ledhost.viewport.Color
import com.timberglund.ledhost.viewport.Point
import kotlin.test.*

class PixelMapperTest {

   @Test
   fun `LEDAddress validates stripId`() {
      assertFailsWith<IllegalArgumentException> {
         LEDAddress(-1, 0)
      }
   }

   @Test
   fun `LEDAddress validates ledIndex`() {
      assertFailsWith<IllegalArgumentException> {
         LEDAddress(0, -1)
      }
   }

   @Test
   fun `LEDAddress creates valid address`() {
      val addr = LEDAddress(5, 10)
      assertEquals(5, addr.stripId)
      assertEquals(10, addr.ledIndex)
   }

   @Test
   fun `LinearMapper maps horizontal strip correctly`() {
      val layout = StripLayout(
         id = 0,
         length = 10,
         position = StripPosition(
            start = PointConfig(0, 0),
            end = PointConfig(9, 0)
         ),
         reverse = false
      )

      val mapper = LinearMapper(listOf(layout))
      val mapping = mapper.getMapping()

      // Check first pixel
      assertEquals(LEDAddress(0, 0), mapping[Point(0, 0)])

      // Check last pixel
      assertEquals(LEDAddress(0, 9), mapping[Point(9, 0)])

      // Check middle pixel
      assertEquals(LEDAddress(0, 5), mapping[Point(5, 0)])

      // Check total mappings
      assertEquals(10, mapping.size)
   }

   @Test
   fun `LinearMapper handles reverse direction`() {
      val layout = StripLayout(
         id = 0,
         length = 10,
         position = StripPosition(
            start = PointConfig(0, 0),
            end = PointConfig(9, 0)
         ),
         reverse = true
      )

      val mapper = LinearMapper(listOf(layout))
      val mapping = mapper.getMapping()

      // With reverse, first pixel maps to last LED
      assertEquals(LEDAddress(0, 9), mapping[Point(0, 0)])

      // Last pixel maps to first LED
      assertEquals(LEDAddress(0, 0), mapping[Point(9, 0)])
   }

   @Test
   fun `LinearMapper maps vertical strip correctly`() {
      val layout = StripLayout(
         id = 0,
         length = 5,
         position = StripPosition(
            start = PointConfig(0, 0),
            end = PointConfig(0, 4)
         ),
         reverse = false
      )

      val mapper = LinearMapper(listOf(layout))
      val mapping = mapper.getMapping()

      // Check vertical mapping
      assertEquals(LEDAddress(0, 0), mapping[Point(0, 0)])
      assertEquals(LEDAddress(0, 1), mapping[Point(0, 1)])
      assertEquals(LEDAddress(0, 4), mapping[Point(0, 4)])

      assertEquals(5, mapping.size)
   }

   @Test
   fun `LinearMapper handles diagonal strip`() {
      val layout = StripLayout(
         id = 0,
         length = 5,
         position = StripPosition(
            start = PointConfig(0, 0),
            end = PointConfig(4, 4)
         ),
         reverse = false
      )

      val mapper = LinearMapper(listOf(layout))
      val mapping = mapper.getMapping()

      // Should map diagonal points
      assertTrue(mapping.containsKey(Point(0, 0)))
      assertTrue(mapping.containsKey(Point(4, 4)))

      // Should have 5 mappings
      assertEquals(5, mapping.size)
   }

   @Test
   fun `LinearMapper handles multiple strips`() {
      val layouts = listOf(
         StripLayout(
            id = 0,
            length = 5,
            position = StripPosition(
               start = PointConfig(0, 0),
               end = PointConfig(4, 0)
            )
         ),
         StripLayout(
            id = 1,
            length = 5,
            position = StripPosition(
               start = PointConfig(0, 1),
               end = PointConfig(4, 1)
            )
         )
      )

      val mapper = LinearMapper(layouts)
      val mapping = mapper.getMapping()

      // Check first strip
      assertEquals(LEDAddress(0, 0), mapping[Point(0, 0)])
      assertEquals(LEDAddress(0, 4), mapping[Point(4, 0)])

      // Check second strip
      assertEquals(LEDAddress(1, 0), mapping[Point(0, 1)])
      assertEquals(LEDAddress(1, 4), mapping[Point(4, 1)])

      // Total mappings
      assertEquals(10, mapping.size)
   }

   @Test
   fun `LinearMapper mapViewportToLEDs returns correct colors`() {
      val layout = StripLayout(
         id = 0,
         length = 3,
         position = StripPosition(
            start = PointConfig(0, 0),
            end = PointConfig(2, 0)
         )
      )

      val mapper = LinearMapper(listOf(layout))
      val viewport = ArrayViewport(3, 1)

      // Set different colors
      viewport.setPixel(0, 0, Color.RED)
      viewport.setPixel(1, 0, Color.GREEN)
      viewport.setPixel(2, 0, Color.BLUE)

      val ledColors = mapper.mapViewportToLEDs(viewport)

      assertEquals(Color.RED, ledColors[LEDAddress(0, 0)])
      assertEquals(Color.GREEN, ledColors[LEDAddress(0, 1)])
      assertEquals(Color.BLUE, ledColors[LEDAddress(0, 2)])
   }

   @Test
   fun `GridMapper creates valid grid mapping`() {
      val layouts = listOf(
         StripLayout(
            id = 0,
            length = 3,
            position = StripPosition(
               start = PointConfig(0, 0),
               end = PointConfig(2, 0)
            ),
            reverse = false
         ),
         StripLayout(
            id = 1,
            length = 3,
            position = StripPosition(
               start = PointConfig(0, 1),
               end = PointConfig(2, 1)
            ),
            reverse = true  // Serpentine
         )
      )

      val mapper = GridMapper(layouts, columns = 3, rows = 2)
      val mapping = mapper.getMapping()

      // First row (not reversed)
      assertEquals(LEDAddress(0, 0), mapping[Point(0, 0)])
      assertEquals(LEDAddress(0, 1), mapping[Point(1, 0)])
      assertEquals(LEDAddress(0, 2), mapping[Point(2, 0)])

      // Second row (reversed for serpentine)
      assertEquals(LEDAddress(1, 2), mapping[Point(0, 1)])
      assertEquals(LEDAddress(1, 1), mapping[Point(1, 1)])
      assertEquals(LEDAddress(1, 0), mapping[Point(2, 1)])
   }

   @Test
   fun `GridMapper getLEDAt returns correct addresses`() {
      val layouts = listOf(
         StripLayout(
            id = 0,
            length = 2,
            position = StripPosition(
               start = PointConfig(0, 0),
               end = PointConfig(1, 0)
            ),
            reverse = false
         )
      )

      val mapper = GridMapper(layouts, columns = 2, rows = 1)

      assertEquals(LEDAddress(0, 0), mapper.getLEDAt(0, 0))
      assertEquals(LEDAddress(0, 1), mapper.getLEDAt(1, 0))
      assertNull(mapper.getLEDAt(5, 5))
   }

   @Test
   fun `GridMapper validates dimensions`() {
      val layouts = listOf(
         StripLayout(
            id = 0,
            length = 1,
            position = StripPosition(
               start = PointConfig(0, 0),
               end = PointConfig(0, 0)
            )
         )
      )

      assertFailsWith<IllegalArgumentException> {
         GridMapper(layouts, columns = 0, rows = 1)
      }

      assertFailsWith<IllegalArgumentException> {
         GridMapper(layouts, columns = 1, rows = -1)
      }
   }

   @Test
   fun `GridMapper mapViewportToLEDs handles serpentine correctly`() {
      val layouts = listOf(
         StripLayout(
            id = 0,
            length = 2,
            position = StripPosition(
               start = PointConfig(0, 0),
               end = PointConfig(1, 0)
            ),
            reverse = false
         ),
         StripLayout(
            id = 1,
            length = 2,
            position = StripPosition(
               start = PointConfig(0, 1),
               end = PointConfig(1, 1)
            ),
            reverse = true
         )
      )

      val mapper = GridMapper(layouts, columns = 2, rows = 2)
      val viewport = ArrayViewport(2, 2)

      viewport.setPixel(0, 0, Color.RED)
      viewport.setPixel(1, 0, Color.GREEN)
      viewport.setPixel(0, 1, Color.BLUE)
      viewport.setPixel(1, 1, Color.YELLOW)

      val ledColors = mapper.mapViewportToLEDs(viewport)

      // First strip (not reversed)
      assertEquals(Color.RED, ledColors[LEDAddress(0, 0)])
      assertEquals(Color.GREEN, ledColors[LEDAddress(0, 1)])

      // Second strip (reversed)
      assertEquals(Color.YELLOW, ledColors[LEDAddress(1, 0)])
      assertEquals(Color.BLUE, ledColors[LEDAddress(1, 1)])
   }

   @Test
   fun `mappers handle out of bounds gracefully`() {
      val layout = StripLayout(
         id = 0,
         length = 5,
         position = StripPosition(
            start = PointConfig(10, 10),  // Outside typical viewport
            end = PointConfig(14, 10)
         )
      )

      val mapper = LinearMapper(listOf(layout))
      val viewport = ArrayViewport(5, 5)  // Small viewport

      val ledColors = mapper.mapViewportToLEDs(viewport)

      // Should return empty map since strip is outside viewport
      assertTrue(ledColors.isEmpty())
   }
}
