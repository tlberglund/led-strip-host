package com.timberglund.ledhost.pattern.patterns

import com.timberglund.ledhost.pattern.ParameterDef
import com.timberglund.ledhost.pattern.Pattern
import com.timberglund.ledhost.pattern.PatternParameters
import com.timberglund.ledhost.viewport.Color
import com.timberglund.ledhost.viewport.Viewport

class SolidColorPattern : Pattern {
   override val name = "Solid Color"
   override val description = "A solid color fill"

   override val parameters = listOf(
      ParameterDef.ColorParam("color", "Color", "#ff00001f"),
   )

   private var color = Color.RED

   override fun initialize(viewport: Viewport, params: PatternParameters) {
      color = params.getColor("color", Color.RED)
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
