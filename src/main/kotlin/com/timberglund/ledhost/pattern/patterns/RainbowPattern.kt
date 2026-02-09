package com.timberglund.ledhost.pattern.patterns

import com.timberglund.ledhost.pattern.ParameterDef
import com.timberglund.ledhost.pattern.Pattern
import com.timberglund.ledhost.pattern.PatternParameters
import com.timberglund.ledhost.viewport.Color
import com.timberglund.ledhost.viewport.Viewport

/**
 * Rainbow pattern that creates a scrolling rainbow effect across the viewport.
 * Uses HSV color space for smooth color transitions.
 */
class RainbowPattern : Pattern {
   override val name = "Rainbow"
   override val description = "Scrolling rainbow effect with smooth color transitions"

   override val parameters = listOf(
      ParameterDef.FloatParam("speed", "Speed", 0.1f, 5f, 0.1f, 1f),
      ParameterDef.FloatParam("brightness", "Brightness", 0f, 1f, 0.05f, 1f),
      ParameterDef.FloatParam("saturation", "Saturation", 0f, 1f, 0.05f, 1f),
      ParameterDef.SelectParam("direction", "Direction", listOf("horizontal", "vertical", "diagonal"), "horizontal"),
   )

   private var hueOffset = 0f
   private var speed = 1f
   private var saturation = 1f
   private var brightness = 1f
   private var direction = Direction.HORIZONTAL

   enum class Direction {
      HORIZONTAL,
      VERTICAL,
      DIAGONAL
   }

   override fun initialize(viewport: Viewport, params: PatternParameters) {
      speed = params.get("speed", 1f)
      saturation = params.get("saturation", 1f).coerceIn(0f, 1f)
      brightness = params.get("brightness", 1f).coerceIn(0f, 1f)

      val directionStr = params.get("direction", "horizontal")
      direction = when (directionStr.lowercase()) {
         "vertical" -> Direction.VERTICAL
         "diagonal" -> Direction.DIAGONAL
         else -> Direction.HORIZONTAL
      }

      hueOffset = 0f
   }

   override fun update(deltaTime: Float, totalTime: Float) {
      // Update hue offset based on speed
      hueOffset += speed * deltaTime * 60f // 60 degrees per second at speed=1

      // Keep hue in 0-360 range
      if(hueOffset >= 360f) {
         hueOffset -= 360f
      }
   }

   override fun render(viewport: Viewport) {
      when(direction) {
         Direction.HORIZONTAL -> renderHorizontal(viewport)
         Direction.VERTICAL -> renderVertical(viewport)
         Direction.DIAGONAL -> renderDiagonal(viewport)
      }
   }

   private fun renderHorizontal(viewport: Viewport) {
      for(y in 0 until viewport.height) {
         for(x in 0 until viewport.width) {
            val hue = (hueOffset + (x * 360f / viewport.width)) % 360f
            val color = Color.fromHSV(hue, saturation, brightness)
            viewport.setPixel(x, y, color)
         }
      }
   }

   private fun renderVertical(viewport: Viewport) {
      for(y in 0 until viewport.height) {
         for(x in 0 until viewport.width) {
            val hue = (hueOffset + (y * 360f / viewport.height)) % 360f
            val color = Color.fromHSV(hue, saturation, brightness)
            viewport.setPixel(x, y, color)
         }
      }
   }

   private fun renderDiagonal(viewport: Viewport) {
      val maxDimension = maxOf(viewport.width, viewport.height)
      for(y in 0 until viewport.height) {
         for(x in 0 until viewport.width) {
            val distance = (x + y).toFloat()
            val hue = (hueOffset + (distance * 360f / (maxDimension * 2))) % 360f
            val color = Color.fromHSV(hue, saturation, brightness)
            viewport.setPixel(x, y, color)
         }
      }
   }

   override fun cleanup() {
      // No resources to clean up
   }
}
