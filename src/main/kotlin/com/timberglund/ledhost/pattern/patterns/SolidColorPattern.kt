package com.timberglund.ledhost.pattern.patterns

import com.timberglund.ledhost.pattern.ParameterDef
import com.timberglund.ledhost.pattern.Pattern
import com.timberglund.ledhost.pattern.PatternParameters
import com.timberglund.ledhost.viewport.Color
import com.timberglund.ledhost.viewport.Viewport

class SolidColorPattern : Pattern {
   override val name = "Solid Color"
   override val description = "A solid color fill using HSV color model"

   override val parameters = listOf(
      ParameterDef.FloatParam("hue", "Hue", 0f, 360f, 1f, 0f),
      ParameterDef.FloatParam("saturation", "Saturation", 0f, 1f, 0.01f, 1f),
      ParameterDef.FloatParam("brightness", "Brightness", 0f, 1f, 0.01f, 1f),
   )

   private var color = Color.WHITE

   override fun initialize(viewport: Viewport, params: PatternParameters) {
      val hue = params.get("hue", 0f).coerceIn(0f, 360f)
      val saturation = params.get("saturation", 1f).coerceIn(0f, 1f)
      val brightness = params.get("brightness", 1f).coerceIn(0f, 1f)
      color = Color.fromHSV(hue, saturation, brightness)
   }

   override fun update(deltaTime: Float, totalTime: Float) {
      // Static pattern, nothing to update
   }

   override fun render(viewport: Viewport) {
      viewport.fill(color)
   }

   override fun cleanup() {
      // No resources to clean up
   }
}
