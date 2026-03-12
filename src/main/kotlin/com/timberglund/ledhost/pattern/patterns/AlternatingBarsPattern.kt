package com.timberglund.ledhost.pattern.patterns

import com.timberglund.ledhost.pattern.ParameterDef
import com.timberglund.ledhost.pattern.Pattern
import com.timberglund.ledhost.pattern.PatternParameters
import com.timberglund.ledhost.viewport.Color
import com.timberglund.ledhost.viewport.Viewport
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin

class AlternatingBarsPattern : Pattern {
   override val name = "Alternating Bars"
   override val description = "Alternating solid color bars that scroll at configurable speed and angle"

   override val parameters = listOf(
      ParameterDef.ColorParam("colorA",   "Color A",       "#ff00001f"),
      ParameterDef.ColorParam("colorB",   "Color B",       "#0000ff1f"),
      ParameterDef.FloatParam("barWidth", "Bar Width (mm)", 10f, 1000f, 1f, 80f),
      ParameterDef.FloatParam("speed",    "Speed (mm/s)", -500f, 500f,  1f, 50f),
      ParameterDef.FloatParam("angle",    "Angle (°)",      0f,  360f,  1f,  0f),
   )

   private var colorA            = Color(255, 0,   0,   31)
   private var colorB            = Color(0,   0,   255, 31)
   private var barWidthPixels    = 80f / MM_PER_LED
   private var speedPixelsPerSec = 50f / MM_PER_LED
   private var cosTheta          = 1f
   private var sinTheta          = 0f
   private var scrollOffset      = 0f

   override fun initialize(viewport: Viewport, params: PatternParameters) {
      colorA            = params.getColor("colorA", Color(255, 0, 0, 31))
      colorB            = params.getColor("colorB", Color(0, 0, 255, 31))
      val barWidthMm    = params.get("barWidth", 80f).coerceIn(10f, 1000f)
      val speedMmPerSec = params.get("speed",    50f).coerceIn(-500f, 500f)
      val angleDeg      = params.get("angle",     0f)
      val angleRad      = angleDeg * (PI.toFloat() / 180f)
      barWidthPixels    = barWidthMm / MM_PER_LED
      speedPixelsPerSec = speedMmPerSec / MM_PER_LED
      cosTheta          = cos(angleRad)
      sinTheta          = sin(angleRad)
      scrollOffset      = 0f
   }

   override fun update(deltaTime: Float, totalTime: Float) {
      scrollOffset += speedPixelsPerSec * deltaTime
   }

   override fun render(viewport: Viewport) {
      for(y in 0 until viewport.height) {
         for(x in 0 until viewport.width) {
            val projection = x * cosTheta + y * sinTheta
            val t          = (projection + scrollOffset) / barWidthPixels
            val floored    = floor(t)
            val frac       = t - floored
            val barIndex   = floored.toInt()
            val baseColor  = if(barIndex % 2 == 0) colorA else colorB
            val otherColor = if(barIndex % 2 == 0) colorB else colorA
            val distToEdge = min(frac, 1f - frac) * barWidthPixels
            val raw        = (distToEdge / TRANSITION_HALF_PX).coerceIn(0f, 1f)
            val smooth     = raw * raw * (3f - 2f * raw)
            viewport.setPixel(x, y, Color.blend(otherColor, baseColor, smooth))
         }
      }
   }

   override fun cleanup() {}

   companion object {
      const val MM_PER_LED         = 16.0f
      const val TRANSITION_HALF_PX = 1.5f
   }
}
