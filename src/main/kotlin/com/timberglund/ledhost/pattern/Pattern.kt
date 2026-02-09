package com.timberglund.ledhost.pattern

import com.timberglund.ledhost.viewport.Viewport
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Describes a configurable parameter that a pattern accepts.
 * Used by the frontend to dynamically render controls.
 */
@Serializable
sealed class ParameterDef {
   abstract val name: String
   abstract val label: String

   @Serializable
   @SerialName("float")
   data class FloatParam(
      override val name: String,
      override val label: String,
      val min: Float,
      val max: Float,
      val step: Float,
      val default: Float
   ) : ParameterDef()

   @Serializable
   @SerialName("int")
   data class IntParam(
      override val name: String,
      override val label: String,
      val min: Int,
      val max: Int,
      val step: Int,
      val default: Int
   ) : ParameterDef()

   @Serializable
   @SerialName("select")
   data class SelectParam(
      override val name: String,
      override val label: String,
      val options: List<String>,
      val default: String
   ) : ParameterDef()

   @Serializable
   @SerialName("color")
   data class ColorParam(
      override val name: String,
      override val label: String,
      val default: String // hex "#RRGGBB"
   ) : ParameterDef()
}

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
    * Declares the configurable parameters this pattern accepts.
    * The frontend uses this metadata to dynamically render controls.
    * Default is empty (no declared parameters).
    */
   val parameters: List<ParameterDef> get() = emptyList()

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
