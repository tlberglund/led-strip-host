package com.timberglund.ledhost.viewport

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class ColorTest {

   @Test
   fun `creates valid RGB colors`() {
      val color = Color(255, 128, 0)
      assertEquals(255, color.r)
      assertEquals(128, color.g)
      assertEquals(0, color.b)
   }

   @Test
   fun `rejects invalid RGB values`() {
      assertFailsWith<IllegalArgumentException> { Color(-1, 0, 0) }
      assertFailsWith<IllegalArgumentException> { Color(256, 0, 0) }
      assertFailsWith<IllegalArgumentException> { Color(0, -1, 0) }
      assertFailsWith<IllegalArgumentException> { Color(0, 256, 0) }
      assertFailsWith<IllegalArgumentException> { Color(0, 0, -1) }
      assertFailsWith<IllegalArgumentException> { Color(0, 0, 256) }
   }

   @Test
   fun `converts RGB to integer correctly`() {
      assertEquals(0xff0000, Color(255, 0, 0).toInt())
      assertEquals(0x00ff00, Color(0, 255, 0).toInt())
      assertEquals(0x0000ff, Color(0, 0, 255).toInt())
      assertEquals(0xffffff, Color(255, 255, 255).toInt())
      assertEquals(0x000000, Color(0, 0, 0).toInt())
      assertEquals(0xff8000, Color(255, 128, 0).toInt())
   }

   @Test
   fun `converts red to HSV correctly`() {
      val red = Color(255, 0, 0)
      val hsv = red.toHSV()
      assertEquals(0f, hsv.h, 0.01f)
      assertEquals(1f, hsv.s, 0.01f)
      assertEquals(1f, hsv.v, 0.01f)
   }

   @Test
   fun `converts green to HSV correctly`() {
      val green = Color(0, 255, 0)
      val hsv = green.toHSV()
      assertEquals(120f, hsv.h, 0.01f)
      assertEquals(1f, hsv.s, 0.01f)
      assertEquals(1f, hsv.v, 0.01f)
   }

   @Test
   fun `converts blue to HSV correctly`() {
      val blue = Color(0, 0, 255)
      val hsv = blue.toHSV()
      assertEquals(240f, hsv.h, 0.01f)
      assertEquals(1f, hsv.s, 0.01f)
      assertEquals(1f, hsv.v, 0.01f)
   }

   @Test
   fun `converts black to HSV correctly`() {
      val black = Color(0, 0, 0)
      val hsv = black.toHSV()
      assertEquals(0f, hsv.h, 0.01f)
      assertEquals(0f, hsv.s, 0.01f)
      assertEquals(0f, hsv.v, 0.01f)
   }

   @Test
   fun `converts white to HSV correctly`() {
      val white = Color(255, 255, 255)
      val hsv = white.toHSV()
      assertEquals(0f, hsv.h, 0.01f)
      assertEquals(0f, hsv.s, 0.01f)
      assertEquals(1f, hsv.v, 0.01f)
   }

   @Test
   fun `converts HSV to RGB red correctly`() {
      val color = Color.fromHSV(0f, 1f, 1f)
      assertEquals(255, color.r)
      assertEquals(0, color.g)
      assertEquals(0, color.b)
   }

   @Test
   fun `converts HSV to RGB green correctly`() {
      val color = Color.fromHSV(120f, 1f, 1f)
      assertEquals(0, color.r)
      assertEquals(255, color.g)
      assertEquals(0, color.b)
   }

   @Test
   fun `converts HSV to RGB blue correctly`() {
      val color = Color.fromHSV(240f, 1f, 1f)
      assertEquals(0, color.r)
      assertEquals(0, color.g)
      assertEquals(255, color.b)
   }

   @Test
   fun `converts HSV to RGB yellow correctly`() {
      val color = Color.fromHSV(60f, 1f, 1f)
      assertEquals(255, color.r)
      assertEquals(255, color.g)
      assertEquals(0, color.b)
   }

   @Test
   fun `converts HSV to RGB with saturation`() {
      val color = Color.fromHSV(0f, 0.5f, 1f)
      assertEquals(255, color.r)
      assertEquals(128.0, color.g.toDouble(), 1.0)
      assertEquals(128.0, color.b.toDouble(), 1.0)
   }

   @Test
   fun `RGB to HSV to RGB round trip`() {
      val original = Color(123, 234, 45)
      val hsv = original.toHSV()
      val converted = Color.fromHSV(hsv.h, hsv.s, hsv.v)

      // Allow small rounding errors
      assertEquals(original.r.toDouble(), converted.r.toDouble(), 1.0)
      assertEquals(original.g.toDouble(), converted.g.toDouble(), 1.0)
      assertEquals(original.b.toDouble(), converted.b.toDouble(), 1.0)
   }

   @Test
   fun `blends two colors equally`() {
      val red = Color(255, 0, 0)
      val blue = Color(0, 0, 255)
      val blended = Color.blend(red, blue, 0.5f)

      assertEquals(128.0, blended.r.toDouble(), 1.0)
      assertEquals(0, blended.g)
      assertEquals(128.0, blended.b.toDouble(), 1.0)
   }

   @Test
   fun `blends with ratio 0 returns first color`() {
      val red = Color(255, 0, 0)
      val blue = Color(0, 0, 255)
      val blended = Color.blend(red, blue, 0f)

      assertEquals(red.r, blended.r)
      assertEquals(red.g, blended.g)
      assertEquals(red.b, blended.b)
   }

   @Test
   fun `blends with ratio 1 returns second color`() {
      val red = Color(255, 0, 0)
      val blue = Color(0, 0, 255)
      val blended = Color.blend(red, blue, 1f)

      assertEquals(blue.r, blended.r)
      assertEquals(blue.g, blended.g)
      assertEquals(blue.b, blended.b)
   }

   @Test
   fun `blend ratio is clamped to 0-1 range`() {
      val red = Color(255, 0, 0)
      val blue = Color(0, 0, 255)

      // Ratio > 1 should be clamped to 1
      val blended1 = Color.blend(red, blue, 2f)
      assertEquals(blue.r, blended1.r)
      assertEquals(blue.g, blended1.g)
      assertEquals(blue.b, blended1.b)

      // Ratio < 0 should be clamped to 0
      val blended2 = Color.blend(red, blue, -1f)
      assertEquals(red.r, blended2.r)
      assertEquals(red.g, blended2.g)
      assertEquals(red.b, blended2.b)
   }

   @Test
   fun `color constants are correct`() {
      assertEquals(Color(0, 0, 0), Color.BLACK)
      assertEquals(Color(255, 255, 255), Color.WHITE)
      assertEquals(Color(255, 0, 0), Color.RED)
      assertEquals(Color(0, 255, 0), Color.GREEN)
      assertEquals(Color(0, 0, 255), Color.BLUE)
      assertEquals(Color(255, 255, 0), Color.YELLOW)
      assertEquals(Color(0, 255, 255), Color.CYAN)
      assertEquals(Color(255, 0, 255), Color.MAGENTA)
   }

   @Test
   fun `HSV color converts back to RGB`() {
      val hsv = HSVColor(180f, 0.5f, 0.8f)
      val rgb = hsv.toRGB()

      // Check that conversion produces valid RGB values
      assertTrue(rgb.r in 0..255)
      assertTrue(rgb.g in 0..255)
      assertTrue(rgb.b in 0..255)
   }
}
