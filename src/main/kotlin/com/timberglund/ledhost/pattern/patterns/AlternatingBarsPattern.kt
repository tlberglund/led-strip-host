package com.timberglund.ledhost.pattern.patterns

import com.timberglund.ledhost.pattern.ParameterDef
import com.timberglund.ledhost.pattern.Pattern
import com.timberglund.ledhost.pattern.PatternParameters
import com.timberglund.ledhost.viewport.Color
import com.timberglund.ledhost.viewport.Viewport
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.abs
import kotlin.math.sin

class AlternatingBarsPattern : Pattern {
   override val name = "Alternating Bars"
   override val description = "Alternating solid color bars that scroll at configurable speed and angle"

   override val parameters = listOf(
      ParameterDef.ColorParam("colorA",    "Color A",        "#ff00001f"),
      ParameterDef.ColorParam("colorB",    "Color B",        "#0000ff1f"),
      ParameterDef.FloatParam("barWidthA", "Width A (mm)",   10f, 1000f, 1f, 80f),
      ParameterDef.FloatParam("barWidthB", "Width B (mm)",   10f, 1000f, 1f, 80f),
      ParameterDef.FloatParam("speed",     "Speed (mm/s)", -500f,  500f, 1f, 50f),
      ParameterDef.FloatParam("angle",     "Angle (°)",       0f,  360f, 1f,  0f),
   )

   private var colorA             = Color(255, 0,   0,   31)
   private var colorB             = Color(0,   0,   255, 31)
   private var barWidthAPixels   = 80f / MM_PER_LED
   private var barWidthBPixels   = 80f / MM_PER_LED
   private var speedPixelsPerSec = 50f / MM_PER_LED
   private var cosTheta          = 1f
   private var sinTheta          = 0f
   private var scrollOffset      = 0f

   override fun initialize(viewport: Viewport, params: PatternParameters) {
      colorA             = params.getColor("colorA", Color(255, 0, 0, 31))
      colorB             = params.getColor("colorB", Color(0, 0, 255, 31))
      val barWidthAMm   = params.get("barWidthA", 80f).coerceIn(10f, 1000f)
      val barWidthBMm   = params.get("barWidthB", 80f).coerceIn(10f, 1000f)
      val speedMmPerSec = params.get("speed",     50f).coerceIn(-500f, 500f)
      val angleDeg      = params.get("angle",      0f)
      val angleRad      = angleDeg * (PI.toFloat() / 180f)
      barWidthAPixels   = barWidthAMm / MM_PER_LED
      barWidthBPixels   = barWidthBMm / MM_PER_LED
      speedPixelsPerSec = speedMmPerSec / MM_PER_LED
      cosTheta          = cos(angleRad)
      sinTheta          = sin(angleRad)
      scrollOffset      = 0f
   }

   override fun update(deltaTime: Float, totalTime: Float) {
      scrollOffset += speedPixelsPerSec * deltaTime
   }

   override fun render(viewport: Viewport) {
      val period = barWidthAPixels + barWidthBPixels
      for(y in 0 until viewport.height) {
         for(x in 0 until viewport.width) {
            val projection = x * cosTheta + y * sinTheta
            val p          = projection + scrollOffset
            val phase      = ((p % period) + period) % period  // [0, period)

            // Find signed pixel distance to the nearest bar boundary and the
            // colors on each side of it. Positive = we are right of the boundary.
            val signedDist: Float
            val leftColor:  Color
            val rightColor: Color

            if(phase < barWidthAPixels) {
               val distToLeft  = phase
               val distToRight = barWidthAPixels - phase
               if(distToLeft <= distToRight) {
                  signedDist = distToLeft   // right of the B→A boundary
                  leftColor  = colorB
                  rightColor = colorA
               }
               else {
                  signedDist = -distToRight // left of the A→B boundary
                  leftColor  = colorA
                  rightColor = colorB
               }
            }
            else {
               val phaseInB    = phase - barWidthAPixels
               val distToLeft  = phaseInB
               val distToRight = barWidthBPixels - phaseInB
               if(distToLeft <= distToRight) {
                  signedDist = distToLeft   // right of the A→B boundary
                  leftColor  = colorA
                  rightColor = colorB
               }
               else {
                  signedDist = -distToRight // left of the B→A boundary
                  leftColor  = colorB
                  rightColor = colorA
               }
            }

            // Smoothstep: 0 at the boundary (50/50 blend), 1 in solid interior
            val raw    = (abs(signedDist) / TRANSITION_HALF_PX).coerceIn(0f, 1f)
            val smooth = raw * raw * (3f - 2f * raw)
            val bf     = if(signedDist <= 0f) 0.5f * (1f - smooth) else 0.5f + 0.5f * smooth
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
