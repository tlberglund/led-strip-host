package com.timberglund.ledhost.pattern.patterns

import com.timberglund.ledhost.pattern.ParameterDef
import com.timberglund.ledhost.pattern.Pattern
import com.timberglund.ledhost.pattern.PatternParameters
import com.timberglund.ledhost.viewport.Color
import com.timberglund.ledhost.viewport.Viewport
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Plasma pattern implementing the 4rknova multiplicative interference algorithm.
 * Two phase oscillators are multiplied together to produce sharp banding and
 * radial symmetry, with a linear gradient mapped onto the resulting plasma value.
 */
class PlasmaInterferencePattern : Pattern {
   override val name = "Plasma (Interference)"
   override val description = "Animated plasma using multiplicative wave interference with a two-color gradient"

   override val parameters = listOf(
      ParameterDef.ColorParam("colorStart", "Color Start", "#0000ff1f"),
      ParameterDef.ColorParam("colorEnd",   "Color End",   "#ff00ff1f"),
      ParameterDef.FloatParam("speed", "Speed", 0.1f, 2f, 0.1f, 1f),
      ParameterDef.FloatParam("scale", "Scale", 0.1f, 4f, 0.1f, 1f),
   )

   private var time = 0f
   private var speed = 1f
   private var scale = 1f
   private var colorStart = Color(0, 0, 255, 31)
   private var colorEnd   = Color(255, 0, 255, 31)

   override fun initialize(viewport: Viewport, params: PatternParameters) {
      speed      = params.get("speed", 1f).coerceIn(0.1f, 2f)
      scale      = params.get("scale", 1f).coerceIn(0.1f, 4f)
      colorStart = params.getColor("colorStart", Color(0, 0, 255, 31))
      colorEnd   = params.getColor("colorEnd",   Color(255, 0, 255, 31))
      time       = 0f
   }

   override fun update(deltaTime: Float, totalTime: Float) {
      time += deltaTime * speed
   }

   override fun render(viewport: Viewport) {
      val t = time
      val w = viewport.width.toFloat()
      val h = viewport.height.toFloat()

      for(y in 0 until viewport.height) {
         for(x in 0 until viewport.width) {
            val uvX = (x / w - 0.5f) * scale
            val uvY = (y / h - 0.5f) * scale

            val phaseV = cos(uvY + sin(0.148f - t)) + 2.4f * t
            val phaseH = sin(uvX + cos(0.628f + t)) - 0.7f * t

            val radialDist = sqrt(uvX * uvX + uvY * uvY)
            val plasma = 7f * cos(radialDist + phaseH) * sin(phaseV + phaseH)

            val blendT = 0.5f + 0.5f * cos(plasma)

            viewport.setPixel(x, y, Color.blend(colorStart, colorEnd, blendT))
         }
      }
   }

   override fun cleanup() {}
}
