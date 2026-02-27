package com.timberglund.ledhost.pattern.patterns

import com.timberglund.ledhost.pattern.ParameterDef
import com.timberglund.ledhost.pattern.Pattern
import com.timberglund.ledhost.pattern.PatternParameters
import com.timberglund.ledhost.viewport.Color
import com.timberglund.ledhost.viewport.Viewport
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Plasma pattern that creates an animated, psychedelic color field by combining
 * multiple sine waves at different frequencies and phases. Produces organic,
 * fluid-looking color blobs that shift and morph over time.
 */
class PlasmaPattern : Pattern {
   override val name = "Plasma"
   override val description = "Animated plasma effect with colorful interference patterns"

   override val parameters = listOf(
      ParameterDef.FloatParam("speed", "Speed", 0.1f, 5f, 0.1f, 1f),
      ParameterDef.FloatParam("value", "Value", 0f, 1f, 0.05f, 1f),
      ParameterDef.FloatParam("saturation", "Saturation", 0f, 1f, 0.05f, 1f),
      ParameterDef.FloatParam("scale", "Scale", 0.1f, 5f, 0.1f, 1f),
      ParameterDef.FloatParam("hueMin", "Hue Min", 0f, 360f, 1f, 0f),
      ParameterDef.FloatParam("hueMax", "Hue Max", 0f, 360f, 1f, 360f),
      ParameterDef.IntParam("brightness", "Brightness", 0, 31, 1, 31),
   )

   private var time = 0f
   private var speed = 1f
   private var value = 1f
   private var brightness = 31
   private var saturation = 1f
   private var scale = 1f
   private var hueMin = 0f
   private var hueMax = 360f
   private var centerX = 0f
   private var centerY = 0f

   override fun initialize(viewport: Viewport, params: PatternParameters) {
      speed = params.get("speed", 1f)
      value = params.get("value", 1f).coerceIn(0f, 1f)
      brightness = params.get("brightness", 31f).roundToInt().coerceIn(0, 31)
      saturation = params.get("saturation", 1f).coerceIn(0f, 1f)
      scale = params.get("scale", 1f).coerceIn(0.1f, 5f)
      hueMin = params.get("hueMin", 0f).coerceIn(0f, 360f)
      hueMax = params.get("hueMax", 360f).coerceIn(0f, 360f)

      centerX = viewport.width / 2f
      centerY = viewport.height / 2f
      time = 0f
   }

   override fun update(deltaTime: Float, totalTime: Float) {
      time += deltaTime * speed
   }

   override fun render(viewport: Viewport) {
      val scaleBase = 8f * scale

      for(y in 0 until viewport.height) {
         for(x in 0 until viewport.width) {
            val nx = x / scaleBase
            val ny = y / scaleBase

            // Four sine waves at different orientations and speeds
            val v1 = sin(nx + time)
            val v2 = sin(ny + time * 0.7f)
            val v3 = sin((nx + ny) + time * 1.3f)

            val dx = nx - centerX / scaleBase
            val dy = ny - centerY / scaleBase
            val dist = sqrt(dx * dx + dy * dy)
            val v4 = sin(dist + time * 0.5f)

            // Combine and normalize from -1..1 to hueMin..hueMax range
            val value = (v1 + v2 + v3 + v4) / 4f
            val hueRange = hueMax - hueMin
            val hue = (((value + 1f) / 2f * hueRange) + hueMin + time * 20f) % 360f

            val color = Color.fromHSV(hue, saturation, value, brightness)
            viewport.setPixel(x, y, color)
         }
      }
   }

   override fun cleanup() {
      // No resources to clean up
   }
}
