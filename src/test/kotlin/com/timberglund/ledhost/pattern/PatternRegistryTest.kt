package com.timberglund.ledhost.pattern

import com.timberglund.ledhost.viewport.Viewport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PatternRegistryTest {

   // Test pattern implementation
   private class TestPattern(
      override val name: String,
      override val description: String = "Test pattern"
   ) : Pattern {
      var initialized = false
      var updated = false
      var rendered = false
      var cleanedUp = false

      override fun initialize(viewport: Viewport, params: PatternParameters) {
         initialized = true
      }

      override fun update(deltaTime: Float, totalTime: Float) {
         updated = true
      }

      override fun render(viewport: Viewport) {
         rendered = true
      }

      override fun cleanup() {
         cleanedUp = true
      }
   }

   @Test
   fun `registers and retrieves pattern`() {
      val registry = DefaultPatternRegistry()
      val pattern = TestPattern("Test1")

      registry.register(pattern)

      val retrieved = registry.get("Test1")
      assertNotNull(retrieved)
      assertEquals("Test1", retrieved.name)
   }

   @Test
   fun `returns null for non-existent pattern`() {
      val registry = DefaultPatternRegistry()

      val retrieved = registry.get("NonExistent")
      assertNull(retrieved)
   }

   @Test
   fun `lists registered patterns`() {
      val registry = DefaultPatternRegistry()
      registry.register(TestPattern("Pattern1"))
      registry.register(TestPattern("Pattern2"))
      registry.register(TestPattern("Pattern3"))

      val patterns = registry.listPatterns()
      assertEquals(3, patterns.size)
      assertEquals(listOf("Pattern1", "Pattern2", "Pattern3"), patterns)
   }

   @Test
   fun `list is sorted alphabetically`() {
      val registry = DefaultPatternRegistry()
      registry.register(TestPattern("Zebra"))
      registry.register(TestPattern("Apple"))
      registry.register(TestPattern("Middle"))

      val patterns = registry.listPatterns()
      assertEquals(listOf("Apple", "Middle", "Zebra"), patterns)
   }

   @Test
   fun `has returns true for existing pattern`() {
      val registry = DefaultPatternRegistry()
      registry.register(TestPattern("Test1"))

      assertTrue(registry.has("Test1"))
   }

   @Test
   fun `has returns false for non-existent pattern`() {
      val registry = DefaultPatternRegistry()

      assertFalse(registry.has("NonExistent"))
   }

   @Test
   fun `replaces pattern with same name`() {
      val registry = DefaultPatternRegistry()
      val pattern1 = TestPattern("Test1", "Description 1")
      val pattern2 = TestPattern("Test1", "Description 2")

      registry.register(pattern1)
      registry.register(pattern2)

      val retrieved = registry.get("Test1")
      assertNotNull(retrieved)
      assertEquals("Description 2", retrieved.description)
      assertEquals(1, registry.count())
   }

   @Test
   fun `unregisters pattern`() {
      val registry = DefaultPatternRegistry()
      registry.register(TestPattern("Test1"))

      assertTrue(registry.unregister("Test1"))
      assertFalse(registry.has("Test1"))
      assertEquals(0, registry.count())
   }

   @Test
   fun `unregister returns false for non-existent pattern`() {
      val registry = DefaultPatternRegistry()

      assertFalse(registry.unregister("NonExistent"))
   }

   @Test
   fun `clears all patterns`() {
      val registry = DefaultPatternRegistry()
      registry.register(TestPattern("Test1"))
      registry.register(TestPattern("Test2"))
      registry.register(TestPattern("Test3"))

      assertEquals(3, registry.count())

      registry.clear()

      assertEquals(0, registry.count())
      assertEquals(emptyList(), registry.listPatterns())
   }

   @Test
   fun `count returns correct number of patterns`() {
      val registry = DefaultPatternRegistry()

      assertEquals(0, registry.count())

      registry.register(TestPattern("Test1"))
      assertEquals(1, registry.count())

      registry.register(TestPattern("Test2"))
      assertEquals(2, registry.count())

      registry.unregister("Test1")
      assertEquals(1, registry.count())
   }
}
