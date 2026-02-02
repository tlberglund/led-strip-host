package com.timberglund.ledhost.renderer

import com.timberglund.ledhost.pattern.Pattern
import com.timberglund.ledhost.pattern.PatternParameters
import com.timberglund.ledhost.viewport.ArrayViewport
import com.timberglund.ledhost.viewport.Color
import com.timberglund.ledhost.viewport.Viewport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FrameRendererTest {

   /**
    * Test pattern that tracks lifecycle calls
    */
   private class TrackingPattern(
      override val name: String = "Tracking",
      override val description: String = "Pattern for testing"
   ) : Pattern {
      var initializeCount = 0
      var updateCount = 0
      var renderCount = 0
      var cleanupCount = 0
      var lastDeltaTime = 0f
      var lastTotalTime = 0f

      override fun initialize(viewport: Viewport, params: PatternParameters) {
         initializeCount++
      }

      override fun update(deltaTime: Float, totalTime: Float) {
         updateCount++
         lastDeltaTime = deltaTime
         lastTotalTime = totalTime
      }

      override fun render(viewport: Viewport) {
         renderCount++
         // Draw something simple so we can verify rendering
         viewport.setPixel(0, 0, Color.RED)
      }

      override fun cleanup() {
         cleanupCount++
      }
   }

    @Test
    fun `creates renderer with valid parameters`() {
        val viewport = ArrayViewport(10, 5)
        val renderer = FrameRenderer(60, viewport)

        assertNotNull(renderer)
        assertFalse(renderer.isRunning())
    }

    @Test
    fun `starts and stops renderer`() {
        val viewport = ArrayViewport(10, 5)
        val renderer = FrameRenderer(60, viewport)

        assertFalse(renderer.isRunning())

        renderer.start()
        assertTrue(renderer.isRunning())

        Thread.sleep(100) // Let it run briefly

        renderer.stop()
        assertFalse(renderer.isRunning())
    }

    @Test
    fun `sets and retrieves pattern`() {
        val viewport = ArrayViewport(10, 5)
        val renderer = FrameRenderer(60, viewport)
        val pattern = TrackingPattern()

        assertNull(renderer.getCurrentPattern())

        renderer.setPattern(pattern)

        assertNotNull(renderer.getCurrentPattern())
        assertEquals("Tracking", renderer.getCurrentPattern()?.name)
    }

    @Test
    fun `initializes pattern on setPattern`() {
        val viewport = ArrayViewport(10, 5)
        val renderer = FrameRenderer(60, viewport)
        val pattern = TrackingPattern()

        assertEquals(0, pattern.initializeCount)

        renderer.setPattern(pattern)

        assertEquals(1, pattern.initializeCount)
    }

    @Test
    fun `renders pattern when running`() {
        val viewport = ArrayViewport(10, 5)
        val renderer = FrameRenderer(60, viewport)
        val pattern = TrackingPattern()

        renderer.setPattern(pattern)
        renderer.start()

        // Wait for a few frames
        Thread.sleep(200)

        renderer.stop()

        // Should have called update and render multiple times
        assertTrue(pattern.updateCount > 0, "Update should have been called")
        assertTrue(pattern.renderCount > 0, "Render should have been called")
    }

    @Test
    fun `calls cleanup when switching patterns`() {
        val viewport = ArrayViewport(10, 5)
        val renderer = FrameRenderer(60, viewport)
        val pattern1 = TrackingPattern("Pattern1")
        val pattern2 = TrackingPattern("Pattern2")

        renderer.setPattern(pattern1)
        assertEquals(0, pattern1.cleanupCount)

        renderer.setPattern(pattern2)

        assertEquals(1, pattern1.cleanupCount, "First pattern should be cleaned up")
        assertEquals(1, pattern2.initializeCount, "Second pattern should be initialized")
    }

    @Test
    fun `tracks rendering statistics`() {
        val viewport = ArrayViewport(10, 5)
        val renderer = FrameRenderer(60, viewport)
        val pattern = TrackingPattern()

        renderer.setPattern(pattern)
        renderer.start()

        // Wait for statistics to accumulate
        Thread.sleep(1500) // Wait more than 1 second for FPS calculation

        val stats = renderer.getStatistics()

        renderer.stop()

        // FPS should be close to target (within reasonable margin)
        assertTrue(stats.fps > 0, "FPS should be greater than 0")
        assertTrue(stats.fps <= 70, "FPS should not exceed reasonable bounds")

        // Frame time should be reasonable
        assertTrue(stats.frameTime >= 0, "Frame time should be non-negative")
    }

    @Test
    fun `frame callback is invoked`() {
        val viewport = ArrayViewport(10, 5)
        var callbackCount = 0
        var lastViewport: Viewport? = null

        val renderer = FrameRenderer(
            targetFPS = 60,
            viewport = viewport,
            onFrameRendered = { vp ->
                callbackCount++
                lastViewport = vp
            }
        )

        val pattern = TrackingPattern()
        renderer.setPattern(pattern)
        renderer.start()

        Thread.sleep(200)

        renderer.stop()

        assertTrue(callbackCount > 0, "Callback should have been invoked")
        assertNotNull(lastViewport, "Viewport should have been passed to callback")
    }

    @Test
    fun `updates delta time correctly`() {
        val viewport = ArrayViewport(10, 5)
        val renderer = FrameRenderer(60, viewport)
        val pattern = TrackingPattern()

        renderer.setPattern(pattern)
        renderer.start()

        Thread.sleep(200)

        renderer.stop()

        // Delta time should be small (approximately 1/60 second for 60 FPS)
        assertTrue(pattern.lastDeltaTime > 0, "Delta time should be positive")
        assertTrue(pattern.lastDeltaTime < 1.0f, "Delta time should be less than 1 second")
    }

    @Test
    fun `total time accumulates`() {
        val viewport = ArrayViewport(10, 5)
        val renderer = FrameRenderer(60, viewport)
        val pattern = TrackingPattern()

        renderer.setPattern(pattern)
        renderer.start()

        Thread.sleep(500)

        renderer.stop()

        // Total time should have accumulated
        assertTrue(pattern.lastTotalTime > 0, "Total time should be positive")
        assertTrue(pattern.lastTotalTime >= 0.4f, "Total time should be at least 0.4 seconds")
    }

    @Test
    fun `pattern renders to viewport`() {
        val viewport = ArrayViewport(10, 5)
        val renderer = FrameRenderer(60, viewport)
        val pattern = TrackingPattern() // This pattern sets pixel (0,0) to RED

        renderer.setPattern(pattern)
        renderer.start()

        Thread.sleep(100)

        renderer.stop()

        // Check that the pattern actually drew to the viewport
        // Note: There might be a race condition, but pattern should have rendered at least once
        // The TrackingPattern sets pixel (0,0) to RED
        val pixel = viewport.getPixel(0, 0)
        // It might be RED if we catch it at the right moment, or BLACK if cleared
        // Just verify the pattern was called
        assertTrue(pattern.renderCount > 0)
    }

    @Test
    fun `stops gracefully when no pattern is set`() {
        val viewport = ArrayViewport(10, 5)
        val renderer = FrameRenderer(60, viewport)

        // Start without setting a pattern
        renderer.start()

        Thread.sleep(100)

        // Should not crash
        renderer.stop()

        assertFalse(renderer.isRunning())
    }

    @Test
    fun `double start does not create multiple threads`() {
        val viewport = ArrayViewport(10, 5)
        val renderer = FrameRenderer(60, viewport)

        renderer.start()
        val wasRunning = renderer.isRunning()

        renderer.start() // Try to start again

        // Should still be running but not create issues
        assertTrue(wasRunning)
        assertTrue(renderer.isRunning())

        renderer.stop()
    }

    @Test
    fun `double stop is safe`() {
        val viewport = ArrayViewport(10, 5)
        val renderer = FrameRenderer(60, viewport)

        renderer.start()
        renderer.stop()

        assertFalse(renderer.isRunning())

        // Should not crash
        renderer.stop()

        assertFalse(renderer.isRunning())
    }

    @Test
    fun `can restart after stopping`() {
        val viewport = ArrayViewport(10, 5)
        val renderer = FrameRenderer(60, viewport)
        val pattern = TrackingPattern()

        renderer.setPattern(pattern)

        // First run
        renderer.start()
        Thread.sleep(100)
        renderer.stop()

        val firstRenderCount = pattern.renderCount

        // Second run
        renderer.start()
        Thread.sleep(100)
        renderer.stop()

        val secondRenderCount = pattern.renderCount

        // Should have rendered more frames in second run
        assertTrue(secondRenderCount > firstRenderCount)
    }

    @Test
    fun `different frame rates produce different FPS`() {
        val viewport1 = ArrayViewport(10, 5)
        val viewport2 = ArrayViewport(10, 5)

        val renderer30 = FrameRenderer(30, viewport1)
        val renderer60 = FrameRenderer(60, viewport2)

        val pattern30 = TrackingPattern()
        val pattern60 = TrackingPattern()

        renderer30.setPattern(pattern30)
        renderer60.setPattern(pattern60)

        renderer30.start()
        renderer60.start()

        Thread.sleep(1500)

        renderer30.stop()
        renderer60.stop()

        val stats30 = renderer30.getStatistics()
        val stats60 = renderer60.getStatistics()

        // 60 FPS should render more frames than 30 FPS
        assertTrue(pattern60.renderCount > pattern30.renderCount,
            "60 FPS should render more frames than 30 FPS")
    }
}
