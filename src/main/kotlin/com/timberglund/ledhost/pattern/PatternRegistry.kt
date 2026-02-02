package com.timberglund.ledhost.pattern

/**
 * Registry for managing available patterns.
 * Allows registration and retrieval of patterns by name.
 */
interface PatternRegistry {
   /**
    * Registers a pattern in the registry.
    * If a pattern with the same name exists, it will be replaced.
    *
    * @param pattern The pattern to register
    */
   fun register(pattern: Pattern)

   /**
    * Retrieves a pattern by name.
    *
    * @param name The pattern name
    * @return The pattern instance, or null if not found
    */
   fun get(name: String): Pattern?

   /**
    * Lists all registered pattern names.
    *
    * @return List of pattern names sorted alphabetically
    */
   fun listPatterns(): List<String>

   /**
    * Checks if a pattern is registered.
    *
    * @param name The pattern name
    * @return true if the pattern exists
    */
   fun has(name: String): Boolean

   /**
    * Unregisters a pattern.
    *
    * @param name The pattern name to remove
    * @return true if the pattern was removed, false if it didn't exist
    */
   fun unregister(name: String): Boolean

   /**
    * Clears all registered patterns.
    */
   fun clear()

   /**
    * Gets the number of registered patterns.
    */
   fun count(): Int
}

/**
 * Default implementation of PatternRegistry using a thread-safe map.
 */
class DefaultPatternRegistry : PatternRegistry {
   private val patterns = mutableMapOf<String, Pattern>()

   @Synchronized
   override fun register(pattern: Pattern) {
      patterns[pattern.name] = pattern
   }

   @Synchronized
   override fun get(name: String): Pattern? {
      return patterns[name]
   }

   @Synchronized
   override fun listPatterns(): List<String> {
      return patterns.keys.sorted()
   }

   @Synchronized
   override fun has(name: String): Boolean {
      return patterns.containsKey(name)
   }

   @Synchronized
   override fun unregister(name: String): Boolean {
      return patterns.remove(name) != null
   }

   @Synchronized
   override fun clear() {
      patterns.clear()
   }

   @Synchronized
   override fun count(): Int {
      return patterns.size
   }
}
