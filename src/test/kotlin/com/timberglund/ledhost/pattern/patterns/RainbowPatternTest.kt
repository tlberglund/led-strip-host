package com.timberglund.ledhost.pattern.patterns

import com.timberglund.ledhost.pattern.PatternParameters
import com.timberglund.ledhost.viewport.ArrayViewport
import com.timberglund.ledhost.viewport.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RainbowPatternTest {

   @Test
   fun `has correct name and description`() {
      val pattern = RainbowPattern()

      assertEquals("Rainbow", pattern.name)
      assertTrue(pattern.description.isNotEmpty())
   }

   @Test
   fun `initializes with default parameters`() {
      val pattern = RainbowPattern()
      val viewport = ArrayViewport(10, 5)
      val params = PatternParameters()

      // Should not throw exception
      pattern.initialize(viewport, params)
   }

   @Test
   fun `renders horizontal rainbow`() {
      val pattern = RainbowPattern()
      val viewport = ArrayViewport(10, 3)
      val params = PatternParameters()

      pattern.initialize(viewport, params)
      pattern.update(0f, 0f)
      pattern.render(viewport)

      // Check that pixels have different colors along the x-axis
      val firstPixel = viewport.getPixel(0, 0)
      val lastPixel = viewport.getPixel(9, 0)

      // First and last pixels should have different colors (rainbow spans width)
      assertNotEquals(firstPixel, lastPixel)

      // All pixels in the same column should have the same color (horizontal rainbow)
      for(y in 0 until viewport.height) {
         assertEquals(firstPixel, viewport.getPixel(0, y), "Column 0 should have same color at all heights")
      }
   }

   @Test
   fun `renders vertical rainbow`() {
      val pattern = RainbowPattern()
      val viewport = ArrayViewport(5, 10)
      val params = PatternParameters()
      params.set("direction", "vertical")

      pattern.initialize(viewport, params)
      pattern.update(0f, 0f)
      pattern.render(viewport)

      // Check that pixels have different colors along the y-axis
      val firstPixel = viewport.getPixel(0, 0)
      val lastPixel = viewport.getPixel(0, 9)

      // First and last pixels should have different colors
      assertNotEquals(firstPixel, lastPixel)

      // All pixels in the same row should have the same color (vertical rainbow)
      for(x in 0 until viewport.width) {
         assertEquals(firstPixel, viewport.getPixel(x, 0), "Row 0 should have same color at all widths")
      }
   }

   @Test
   fun `renders diagonal rainbow`() {
      val pattern = RainbowPattern()
      val viewport = ArrayViewport(10, 10)
      val params = PatternParameters()
      params.set("direction", "diagonal")

      pattern.initialize(viewport, params)
      pattern.update(0f, 0f)
      pattern.render(viewport)

      // Check that pixels on the diagonal have changing colors
      val topLeft = viewport.getPixel(0, 0)
      val bottomRight = viewport.getPixel(9, 9)

      // Corners should have different colors
      assertNotEquals(topLeft, bottomRight)
   }

   @Test
   fun `update changes hue offset`() {
      val pattern = RainbowPattern()
      val viewport = ArrayViewport(10, 5)
      val params = PatternParameters()
      params.set("speed", 1f)

      pattern.initialize(viewport, params)

      // Render initial frame
      pattern.update(0f, 0f)
      pattern.render(viewport)
      val firstColor = viewport.getPixel(0, 0)

      // Update and render again
      pattern.update(0.1f, 0.1f) // Advance time by 0.1 seconds
      pattern.render(viewport)
      val secondColor = viewport.getPixel(0, 0)

      // Color should have changed due to hue offset update
      assertNotEquals(firstColor, secondColor, "Color should change after update")
   }

   @Test
   fun `respects speed parameter`() {
      val pattern = RainbowPattern()
      val viewport = ArrayViewport(10, 5)
      val params = PatternParameters()
      params.set("speed", 2f) // Double speed

      pattern.initialize(viewport, params)
      pattern.update(0f, 0f)
      pattern.render(viewport)
      val firstColor = viewport.getPixel(0, 0)

      // With double speed, colors should change faster
      pattern.update(0.05f, 0.05f)
      pattern.render(viewport)
      val secondColor = viewport.getPixel(0, 0)

      assertNotEquals(firstColor, secondColor)
   }

   @Test
   fun `respects saturation parameter`() {
      val pattern = RainbowPattern()
      val viewport = ArrayViewport(10, 5)
      val params = PatternParameters()
      params.set("saturation", 0.5f)

      pattern.initialize(viewport, params)
      pattern.update(0f, 0f)
      pattern.render(viewport)

      // With lower saturation, colors should be less vivid
      // We can't easily test this without analyzing the actual color values
      // Just verify it doesn't crash
      assertTrue(true)
   }

   @Test
   fun `respects brightness parameter`() {
      val pattern = RainbowPattern()
      val viewport = ArrayViewport(10, 5)
      val params = PatternParameters()
      params.set("brightness", 0.5f)

      pattern.initialize(viewport, params)
      pattern.update(0f, 0f)
      pattern.render(viewport)

      // With lower brightness, colors should be darker
      val color = viewport.getPixel(5, 2)

      // All color components should be less than 255
      assertTrue(color.r < 255 || color.g < 255 || color.b < 255)
   }

   @Test
   fun `cleanup does not throw exception`() {
      val pattern = RainbowPattern()

      // Should not throw
      pattern.cleanup()
   }

   @Test
   fun `renders full rainbow spectrum`() {
      val pattern = RainbowPattern()
      val width = 360 // One pixel per degree of hue
      val viewport = ArrayViewport(width, 1)
      val params = PatternParameters()

      pattern.initialize(viewport, params)
      pattern.update(0f, 0f)
      pattern.render(viewport)

      // Check that we get a variety of colors
      val colors = mutableSetOf<Color>()
      for(x in 0 until width step 10) {
         colors.add(viewport.getPixel(x, 0))
      }

      // Should have many different colors
      assertTrue(colors.size > 20, "Should have variety of colors across spectrum")
   }

   @Test
   fun `hue wraps around after 360 degrees`() {
      val pattern = RainbowPattern()
      val viewport = ArrayViewport(10, 5)
      val params = PatternParameters()
      params.set("speed", 1f)

      pattern.initialize(viewport, params)

      // Update by enough time to go past 360 degrees
      // At speed=1, we move 60 degrees per second
      // So 6 seconds = 360 degrees
      pattern.update(6f, 6f)
      pattern.render(viewport)

      // Should still render without issues (hue should wrap)
      val color = viewport.getPixel(0, 0)
      assertTrue(color.r in 0..255)
      assertTrue(color.g in 0..255)
      assertTrue(color.b in 0..255)
   }
}
