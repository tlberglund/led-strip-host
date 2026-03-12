package com.timberglund.ledhost.pattern.patterns

import com.timberglund.ledhost.pattern.ParameterDef
import com.timberglund.ledhost.pattern.Pattern
import com.timberglund.ledhost.pattern.PatternParameters
import com.timberglund.ledhost.viewport.Color
import com.timberglund.ledhost.viewport.Viewport
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.abs
import kotlin.math.floor
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

            // Find the nearest bar boundary (integer value of t) and the signed
            // distance to it. Each boundary separates one color from the other,
            // so we blend across it consistently rather than per-bar.
            val b          = floor(t + 0.5f).toInt()   // nearest boundary index
            val dt         = t - b                      // signed distance in t-units
            val dpx        = abs(dt) * barWidthPixels   // distance in pixels

            // Colors on each side of boundary b
            val leftColor  = if((b - 1) % 2 == 0) colorA else colorB
            val rightColor = if(b % 2 == 0) colorA else colorB

            // Smoothstep: 0 at the boundary, 1 beyond the transition zone
            val raw        = (dpx / TRANSITION_HALF_PX).coerceIn(0f, 1f)
            val smooth     = raw * raw * (3f - 2f * raw)

            // bf=0 → leftColor, bf=1 → rightColor; 0.5 at the boundary itself
            val bf         = if(dt <= 0f) 0.5f * (1f - smooth) else 0.5f + 0.5f * smooth
            viewport.setPixel(x, y, Color.blend(leftColor, rightColor, bf))
         }
      }
   }

   override fun cleanup() {}

   companion object {
      const val MM_PER_LED         = 16.0f
      const val TRANSITION_HALF_PX = 1.5f
   }
}
