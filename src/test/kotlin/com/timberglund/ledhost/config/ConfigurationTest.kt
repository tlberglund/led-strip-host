package com.timberglund.ledhost.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import java.io.File

class ConfigurationTest {

   @Test
   fun `creates valid configuration`() {
      val config = Configuration(
         viewport = ViewportConfig(60, 3),
         strips = listOf(
            StripLayout(
               id = 0,
               length = 60,
               position = StripPosition(
                  start = PointConfig(0, 0),
                  end = PointConfig(59, 0)
               )
            )
         ),
         output = OutputConfig("preview"),
         webServer = WebServerConfig(8080, true),
         targetFPS = 60
      )

      assertEquals(60, config.viewport.width)
      assertEquals(3, config.viewport.height)
      assertEquals(1, config.strips.size)
      assertEquals("preview", config.output.type)
      assertEquals(60, config.targetFPS)
   }

   @Test
   fun `rejects negative target FPS`() {
      assertFailsWith<IllegalArgumentException> {
         Configuration(
               viewport = ViewportConfig(60, 3),
               strips = listOf(
                  StripLayout(0, 60, StripPosition(PointConfig(0, 0), PointConfig(59, 0)))
               ),
               output = OutputConfig("preview"),
               targetFPS = -1
         )
      }
   }

   @Test
   fun `rejects zero target FPS`() {
      assertFailsWith<IllegalArgumentException> {
         Configuration(
            viewport = ViewportConfig(60, 3),
            strips = listOf(
               StripLayout(0, 60, StripPosition(PointConfig(0, 0), PointConfig(59, 0)))
            ),
            output = OutputConfig("preview"),
            targetFPS = 0
         )
      }
   }

   @Test
   fun `rejects empty strips list`() {
      assertFailsWith<IllegalArgumentException> {
         Configuration(
            viewport = ViewportConfig(60, 3),
            strips = emptyList(),
            output = OutputConfig("preview"),
            targetFPS = 60
         )
      }
   }

   @Test
   fun `loads configuration from YAML file`() {
      // Create a temporary YAML file
      val tempFile = File.createTempFile("test-config", ".yaml")
      tempFile.deleteOnExit()

      tempFile.writeText("""
         viewport:
            width: 60
            height: 3

         strips:
            - id: 0
              length: 60
              position:
                 start: {x: 0, y: 0}
                 end: {x: 59, y: 0}
              reverse: false

         output:
            type: "preview"

         targetFPS: 60
         webServer:
            port: 8080
      """.trimIndent())

      println(tempFile.absolutePath)
      val config = Configuration.load(tempFile.absolutePath)

      assertEquals(60, config.viewport.width)
      assertEquals(3, config.viewport.height)
      assertEquals(1, config.strips.size)
      assertEquals(0, config.strips[0].id)
      assertEquals(60, config.strips[0].length)
      assertFalse(config.strips[0].reverse)
      assertEquals("preview", config.output.type)
      assertEquals(60, config.targetFPS)
      assertEquals(8080, config.webServer.port)
   }

   @Test
   fun `throws exception for non-existent file`() {
      assertFailsWith<IllegalArgumentException> {
         Configuration.load("/non/existent/file.yaml")
      }
   }

   @Test
   fun `throws exception for invalid YAML`() {
      val tempFile = File.createTempFile("invalid-config", ".yaml")
      tempFile.deleteOnExit()

      tempFile.writeText("invalid: yaml: content: [[[")

      assertFailsWith<IllegalArgumentException> {
         Configuration.load(tempFile.absolutePath)
      }
   }

   @Test
   fun `uses default values when not specified`() {
      val tempFile = File.createTempFile("minimal-config", ".yaml")
      tempFile.deleteOnExit()

      tempFile.writeText("""
         viewport:
            width: 10
            height: 5

         strips:
            - id: 0
              length: 10
              position:
                 start: {x: 0, y: 0}
                 end: {x: 9, y: 0}

         output:
            type: "preview"
      """.trimIndent())

      val config = Configuration.load(tempFile.absolutePath)

      assertEquals(60, config.targetFPS) // Default value
      assertEquals(8080, config.webServer.port) // Default value
      assertTrue(config.webServer.enabled) // Default value
      assertEquals("linear", config.mapper.type) // Default value
   }

   @Test
   fun `viewport config rejects invalid dimensions`() {
      assertFailsWith<IllegalArgumentException> {
         ViewportConfig(0, 5)
      }

      assertFailsWith<IllegalArgumentException> {
         ViewportConfig(10, 0)
      }

      assertFailsWith<IllegalArgumentException> {
         ViewportConfig(-1, 5)
      }
   }

   @Test
   fun `strip layout rejects invalid values`() {
      assertFailsWith<IllegalArgumentException> {
         StripLayout(
               id = -1,
               length = 60,
               position = StripPosition(PointConfig(0, 0), PointConfig(59, 0))
         )
      }

      assertFailsWith<IllegalArgumentException> {
         StripLayout(
               id = 0,
               length = 0,
               position = StripPosition(PointConfig(0, 0), PointConfig(59, 0))
         )
      }
   }

   @Test
   fun `mapper config rejects invalid type`() {
      assertFailsWith<IllegalArgumentException> {
         MapperConfig(type = "invalid")
      }
   }

   @Test
   fun `mapper config accepts valid types`() {
      // Should not throw
      MapperConfig(type = "linear")
      MapperConfig(type = "grid")
      MapperConfig(type = "custom")
   }

   @Test
   fun `mapper config rejects invalid dimensions`() {
      assertFailsWith<IllegalArgumentException> {
         MapperConfig(type = "grid", columns = 0, rows = 1)
      }

      assertFailsWith<IllegalArgumentException> {
         MapperConfig(type = "grid", columns = 1, rows = -1)
      }
   }

   @Test
   fun `web server config rejects invalid port`() {
      assertFailsWith<IllegalArgumentException> {
         WebServerConfig(port = 0)
      }

      assertFailsWith<IllegalArgumentException> {
         WebServerConfig(port = 65536)
      }

      assertFailsWith<IllegalArgumentException> {
         WebServerConfig(port = -1)
      }
   }

   @Test
   fun `web server config accepts valid ports`() {
      // Should not throw
      WebServerConfig(port = 1)
      WebServerConfig(port = 8080)
      WebServerConfig(port = 65535)
   }

   @Test
   fun `output config gets parameters`() {
      val output = OutputConfig(
         type = "serial",
         parameters = mapOf(
               "port" to "/dev/ttyUSB0",
               "baudRate" to "115200"
         )
      )

      assertEquals("/dev/ttyUSB0", output.getParameter("port"))
      assertEquals("", output.getParameter("missing"))
      assertEquals("default", output.getParameter("missing", "default"))
      assertEquals(115200, output.getIntParameter("baudRate"))
      assertEquals(0, output.getIntParameter("missing"))
   }

   @Test
   fun `point config converts to Point`() {
      val pointConfig = PointConfig(10, 20)
      val point = pointConfig.toPoint()

      assertEquals(10, point.x)
      assertEquals(20, point.y)
   }

   @Test
   fun `loads complex configuration with multiple strips`() {
      val tempFile = File.createTempFile("complex-config", ".yaml")
      tempFile.deleteOnExit()

      tempFile.writeText("""
         viewport:
            width: 60
            height: 5

         strips:
            - id: 0
              length: 60
              position:
                 start: {x: 0, y: 0}
                 end: {x: 59, y: 0}
              reverse: false

            - id: 1
              length: 60
              position:
                 start: {x: 0, y: 1}
                 end: {x: 59, y: 1}
              reverse: true

            - id: 2
              length: 60
              position:
                 start: {x: 0, y: 2}
                 end: {x: 59, y: 2}
              reverse: false

         mapper:
            type: "grid"
            columns: 60
            rows: 5

         output:
            type: "serial"
            parameters:
               port: "/dev/ttyUSB0"
               baudRate: "115200"

         targetFPS: 30

         webServer:
            port: 9090
            enabled: true
      """.trimIndent())

      val config = Configuration.load(tempFile.absolutePath)

      assertEquals(60, config.viewport.width)
      assertEquals(5, config.viewport.height)
      assertEquals(3, config.strips.size)
      assertEquals(0, config.strips[0].id)
      assertEquals(1, config.strips[1].id)
      assertEquals(2, config.strips[2].id)
      assertFalse(config.strips[0].reverse)
      assertTrue(config.strips[1].reverse)
      assertFalse(config.strips[2].reverse)
      assertEquals("grid", config.mapper.type)
      assertEquals(60, config.mapper.columns)
      assertEquals(5, config.mapper.rows)
      assertEquals("serial", config.output.type)
      assertEquals("/dev/ttyUSB0", config.output.getParameter("port"))
      assertEquals(115200, config.output.getIntParameter("baudRate"))
      assertEquals(30, config.targetFPS)
      assertEquals(9090, config.webServer.port)
      assertTrue(config.webServer.enabled)
   }

   @Test
   fun `output config rejects blank type`() {
      assertFailsWith<IllegalArgumentException> {
         OutputConfig(type = "")
      }

      assertFailsWith<IllegalArgumentException> {
         OutputConfig(type = "   ")
      }
   }
}
