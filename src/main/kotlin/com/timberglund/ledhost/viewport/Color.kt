package com.timberglund.ledhost.viewport

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Represents a color in RGB color space.
 * @property r Red component (0-255)
 * @property g Green component (0-255)
 * @property b Blue component (0-255)
 */
data class Color(val r: Int, val g: Int, val b: Int, val brightness: Int = 31) {
   init {
      require(r in 0..255) { "Red component must be in range 0..255, got $r" }
      require(g in 0..255) { "Green component must be in range 0..255, got $g" }
      require(b in 0..255) { "Blue component must be in range 0..255, got $b" }
      require(brightness in 0..31) { "Brightness must be in range 0..31, got $brightness" }
   }

   /**
    * Converts this color to HSV color space.
    * @return HSVColor representation
    */
   fun toHSV(): HSVColor {
      val rNorm = r / 255f
      val gNorm = g / 255f
      val bNorm = b / 255f

      val maxVal = max(max(rNorm, gNorm), bNorm)
      val minVal = min(min(rNorm, gNorm), bNorm)
      val delta = maxVal - minVal

      // Calculate Hue
      val h = when {
         delta == 0f -> 0f
         maxVal == rNorm -> 60f * (((gNorm - bNorm) / delta) % 6f)
         maxVal == gNorm -> 60f * (((bNorm - rNorm) / delta) + 2f)
         else -> 60f * (((rNorm - gNorm) / delta) + 4f)
      }.let { if (it < 0) it + 360f else it }

      // Calculate Saturation
      val s = if (maxVal == 0f) 0f else delta / maxVal

      // Value is just the max
      val v = maxVal

      return HSVColor(h, s, v)
   }

   /**
    * Converts this color to a single integer in RGB format (0xRRGGBB).
    * @return Integer representation of the color
    */
   fun toInt(): Int = (r shl 16) or (g shl 8) or b

   companion object {
      /**
      * Creates a Color from HSV color space.
      * @param h Hue (0-360 degrees)
      * @param s Saturation (0.0-1.0)
      * @param v Value/Brightness (0.0-1.0)
      * @return Color in RGB space
      */
      fun fromHSV(h: Float, s: Float, v: Float, brightness: Int = 31): Color {
         val hNorm = ((h % 360f) + 360f) % 360f  // Normalize to 0-360
         val sNorm = s.coerceIn(0f, 1f)
         val vNorm = v.coerceIn(0f, 1f)

         val c = vNorm * sNorm
         val x = c * (1f - abs((hNorm / 60f) % 2f - 1f))
         val m = vNorm - c

         val (rPrime, gPrime, bPrime) = when {
               hNorm < 60f -> Triple(c, x, 0f)
               hNorm < 120f -> Triple(x, c, 0f)
               hNorm < 180f -> Triple(0f, c, x)
               hNorm < 240f -> Triple(0f, x, c)
               hNorm < 300f -> Triple(x, 0f, c)
               else -> Triple(c, 0f, x)
         }

         return Color(
               ((rPrime + m) * 255f).roundToInt(),
               ((gPrime + m) * 255f).roundToInt(),
               ((bPrime + m) * 255f).roundToInt(),
               brightness.coerceIn(0, 31)
         )
      }

      /**
      * Blends two colors together.
      * @param c1 First color
      * @param c2 Second color
      * @param ratio Blend ratio (0.0 = all c1, 1.0 = all c2)
      * @return Blended color
      */
      fun blend(c1: Color, c2: Color, ratio: Float): Color {
         val r = ratio.coerceIn(0f, 1f)
         return Color(
               (c1.r * (1 - r) + c2.r * r).roundToInt(),
               (c1.g * (1 - r) + c2.g * r).roundToInt(),
               (c1.b * (1 - r) + c2.b * r).roundToInt(),
               (c1.brightness * (1 - r) + c2.brightness * r).roundToInt().coerceIn(0, 31)
         )
      }

      // Common color constants
      val BLACK = Color(0, 0, 0)
      val WHITE = Color(255, 255, 255)
      val RED = Color(255, 0, 0)
      val GREEN = Color(0, 255, 0)
      val BLUE = Color(0, 0, 255)
      val YELLOW = Color(255, 255, 0)
      val CYAN = Color(0, 255, 255)
      val MAGENTA = Color(255, 0, 255)
   }
}

/**
 * Represents a color in HSV (Hue, Saturation, Value) color space.
 * @property h Hue in degrees (0-360)
 * @property s Saturation (0.0-1.0)
 * @property v Value/Brightness (0.0-1.0)
 */
data class HSVColor(val h: Float, val s: Float, val v: Float) {
   /**
    * Converts this HSV color to RGB color space.
    * @return Color in RGB space
    */
   fun toRGB(): Color = Color.fromHSV(h, s, v)
}

/**
 * Represents a 2D point in the viewport.
 * @property x X coordinate
 * @property y Y coordinate
 */
data class Point(val x: Int, val y: Int)
