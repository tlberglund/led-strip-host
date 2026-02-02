package com.timberglund.ledhost.pattern

import com.timberglund.ledhost.viewport.Viewport

/**
 * Pattern interface for LED animation algorithms.
 * Patterns are responsible for generating visual effects by drawing to a viewport.
 */
interface Pattern {
   /**
    * Unique name for this pattern
    */
   val name: String

   /**
    * Human-readable description of the pattern
    */
   val description: String

   /**
    * Initializes the pattern with a viewport and parameters.
    * Called once before the pattern starts rendering.
    *
    * @param viewport The viewport to render to
    * @param params Configuration parameters for the pattern
    */
   fun initialize(viewport: Viewport, params: PatternParameters)

   /**
    * Updates the pattern state based on time.
    * Called once per frame before render().
    *
    * @param deltaTime Time elapsed since last update in seconds
    * @param totalTime Total time elapsed since pattern started in seconds
    */
   fun update(deltaTime: Float, totalTime: Float)

   /**
    * Renders the pattern to the viewport.
    * Called once per frame after update().
    *
    * @param viewport The viewport to draw to
    */
   fun render(viewport: Viewport)

   /**
    * Cleanup method called when the pattern is being stopped.
    * Use this to release any resources.
    */
   fun cleanup()
}

/**
 * Container for pattern configuration parameters.
 * Provides type-safe access to parameter values with defaults.
 */
data class PatternParameters(
   @PublishedApi
   internal val params: MutableMap<String, Any> = mutableMapOf()
) {
   /**
    * Gets a parameter value with a default fallback.
    *
    * @param T The expected type of the parameter
    * @param key Parameter name
    * @param default Default value if parameter is not set or wrong type
    * @return The parameter value or default
    */
   inline fun <reified T> get(key: String, default: T): T {
      return params[key] as? T ?: default
   }

   /**
    * Sets a parameter value.
    *
    * @param key Parameter name
    * @param value Parameter value
    */
   fun set(key: String, value: Any) {
      params[key] = value
   }

   /**
    * Checks if a parameter exists.
    *
    * @param key Parameter name
    * @return true if the parameter is set
    */
   fun has(key: String): Boolean = params.containsKey(key)

   /**
    * Returns all parameter keys.
    */
   fun keys(): Set<String> = params.keys

   /**
    * Creates a copy of these parameters.
    */
   fun copy(): PatternParameters = PatternParameters(params.toMutableMap())
}
