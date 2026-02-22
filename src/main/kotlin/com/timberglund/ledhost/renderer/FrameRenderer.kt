package com.timberglund.ledhost.renderer

import com.timberglund.ledhost.pattern.Pattern
import com.timberglund.ledhost.pattern.PatternParameters
import com.timberglund.ledhost.viewport.Viewport
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

private val logger = KotlinLogging.logger {}

/**
 * Statistics about rendering performance.
 *
 * @property fps Current frames per second
 * @property frameTime Average frame time in milliseconds
 * @property droppedFrames Total number of dropped frames
 */
@Serializable
data class RenderStats(
   var fps: Float = 0f,
   var frameTime: Float = 0f,
   var droppedFrames: Int = 0
)

/**
 * Controls frame timing and rendering pipeline.
 * Manages the render loop, pattern updates, and frame rate control.
 *
 * @property targetFPS Target frames per second (default: 60)
 * @property viewport The viewport to render to
 * @property onFrameRendered Optional callback invoked after each frame is rendered
 */
class FrameRenderer(
   private val targetFPS: Int = 60,
   private val viewport: Viewport,
   private val onFrameRendered: ((Viewport) -> Unit)? = null
) {
    private val isRunning = AtomicBoolean(false)
    private var renderThread: Thread? = null
    private var currentPattern: Pattern? = null
    private val stats = RenderStats()
    private val statsLock = Any()

   init {
      require(targetFPS > 0) { "Target FPS must be positive, got $targetFPS" }
   }

   /**
    * Starts the rendering loop in a separate thread.
    * Does nothing if already running.
    */
   fun start() {
      if(isRunning.getAndSet(true)) {
         return // Already running
      }

      renderThread = thread(name = "FrameRenderer") {
         renderLoop()
      }
   }

   /**
    * Stops the rendering loop.
    * Blocks until the render thread has finished.
    */
   fun stop() {
      if(!isRunning.getAndSet(false)) {
         return // Already stopped
      }

      renderThread?.join(5000) // Wait up to 5 seconds for thread to finish
      renderThread = null
   }

   /**
    * Sets the active pattern.
    * Cleans up the previous pattern and initializes the new one.
    *
    * @param pattern The pattern to activate
    * @param params Parameters for pattern initialization
    */
   fun setPattern(pattern: Pattern, params: PatternParameters = PatternParameters()) {
      synchronized(this) {
         currentPattern?.cleanup()
         currentPattern = pattern
         pattern.initialize(viewport, params)
      }
   }

   /**
    * Gets a copy of the current rendering statistics.
    *
    * @return Current render statistics
    */
   fun getStatistics(): RenderStats {
      synchronized(statsLock) {
         return stats.copy()
      }
   }

   /**
    * Main render loop that runs in a separate thread.
    * Handles timing, pattern updates, rendering, and statistics.
    */
   private fun renderLoop() {
      val frameTimeMs = 1000L / targetFPS
      var lastTime = System.currentTimeMillis()
      var totalTime = 0f
      var frameCount = 0
      var fpsTimerStart = System.currentTimeMillis()

      while(isRunning.get()) {
         val frameStart = System.currentTimeMillis()
         val currentTime = frameStart
         val deltaTime = (currentTime - lastTime) / 1000f

         // Update pattern
         synchronized(this) {
            currentPattern?.update(deltaTime, totalTime)
         }

         // Render to viewport
         viewport.clear()
         synchronized(this) {
            currentPattern?.render(viewport)
         }

         // Notify callback (for web broadcast)
         try {
            onFrameRendered?.invoke(viewport)
         }
         catch (e: Exception) {
            // Log error but continue rendering
            logger.error(e) { "Error in frame callback" }
         }

         // Update statistics
         frameCount++
         val frameElapsed = System.currentTimeMillis() - frameStart
         val wallClockElapsed = System.currentTimeMillis() - fpsTimerStart

         if(wallClockElapsed >= 1000) {
            synchronized(statsLock) {
               stats.fps = frameCount.toFloat() / (wallClockElapsed / 1000f)
               stats.frameTime = wallClockElapsed.toFloat() / frameCount
            }
            frameCount = 0
            fpsTimerStart = System.currentTimeMillis()
         }

         // Frame timing control
         val sleepTime = frameTimeMs - frameElapsed
         if(sleepTime > 0) {
            try {
               Thread.sleep(sleepTime)
            }
            catch(e: InterruptedException) {
               break // Thread was interrupted, exit loop
            }
         }
         else {
            // Frame took longer than target time
            synchronized(statsLock) {
               stats.droppedFrames++
            }
         }

         lastTime = currentTime
         totalTime += deltaTime
      }
   }

   /**
    * Checks if the renderer is currently running.
    *
    * @return true if the render loop is active
    */
   fun isRunning(): Boolean = isRunning.get()

   /**
    * Gets the current pattern.
    *
    * @return The active pattern, or null if none is set
    */
   fun getCurrentPattern(): Pattern? {
      synchronized(this) {
         return currentPattern
      }
   }
}
