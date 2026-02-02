package com.timberglund.ledhost.pattern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PatternParametersTest {

   @Test
   fun `gets string parameter with default`() {
      val params = PatternParameters()
      params.set("name", "test")

      assertEquals("test", params.get("name", "default"))
   }

   @Test
   fun `returns default for missing parameter`() {
      val params = PatternParameters()

      assertEquals("default", params.get("missing", "default"))
   }

   @Test
   fun `gets integer parameter`() {
      val params = PatternParameters()
      params.set("count", 42)

      assertEquals(42, params.get("count", 0))
   }

   @Test
   fun `gets float parameter`() {
      val params = PatternParameters()
      params.set("speed", 1.5f)

      assertEquals(1.5f, params.get("speed", 1.0f))
   }

   @Test
   fun `gets boolean parameter`() {
      val params = PatternParameters()
      params.set("enabled", true)

      assertTrue(params.get("enabled", false))
   }

   @Test
   fun `returns default for wrong type`() {
      val params = PatternParameters()
      params.set("value", "string")

      // Trying to get as Int should return default
      assertEquals(0, params.get("value", 0))
   }

   @Test
   fun `has returns true for existing parameter`() {
      val params = PatternParameters()
      params.set("name", "test")

      assertTrue(params.has("name"))
   }

   @Test
   fun `has returns false for missing parameter`() {
      val params = PatternParameters()

      assertFalse(params.has("missing"))
   }

   @Test
   fun `keys returns all parameter names`() {
      val params = PatternParameters()
      params.set("param1", "value1")
      params.set("param2", 42)
      params.set("param3", true)

      val keys = params.keys()
      assertEquals(3, keys.size)
      assertTrue(keys.contains("param1"))
      assertTrue(keys.contains("param2"))
      assertTrue(keys.contains("param3"))
   }

   @Test
   fun `copy creates independent copy`() {
      val params = PatternParameters()
      params.set("original", "value")

      val copy = params.copy()
      copy.set("original", "modified")
      copy.set("new", "added")

      assertEquals("value", params.get("original", ""))
      assertFalse(params.has("new"))
      assertEquals("modified", copy.get("original", ""))
      assertTrue(copy.has("new"))
   }

   @Test
   fun `can update existing parameter`() {
      val params = PatternParameters()
      params.set("value", 10)
      assertEquals(10, params.get("value", 0))

      params.set("value", 20)
      assertEquals(20, params.get("value", 0))
   }

   @Test
   fun `empty parameters has no keys`() {
      val params = PatternParameters()

      assertTrue(params.keys().isEmpty())
   }
}
