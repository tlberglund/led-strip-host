package com.timberglund.ledhost.web

import com.timberglund.ledhost.config.*
import com.timberglund.ledhost.mapper.LinearMapper
import com.timberglund.ledhost.pattern.DefaultPatternRegistry
import com.timberglund.ledhost.pattern.patterns.RainbowPattern
import com.timberglund.ledhost.viewport.ArrayViewport
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class PreviewServerTest {

    private lateinit var server: PreviewServer
    private lateinit var viewport: ArrayViewport
    private lateinit var registry: DefaultPatternRegistry
    private lateinit var config: Configuration

    @BeforeTest
    fun setup() {
        viewport = ArrayViewport(10, 5)
        registry = DefaultPatternRegistry()
        registry.register(RainbowPattern())

        config = Configuration(
            viewport = ViewportConfig(10, 5),
            strips = listOf(
                StripLayout(0, 50, StripPosition(PointConfig(0, 0), PointConfig(9, 4)))
            ),
            output = OutputConfig("preview"),
            targetFPS = 60
        )

        val mapper = LinearMapper(config.strips)

        server = PreviewServer(
            port = 8081, // Use different port to avoid conflicts
            viewport = viewport,
            patternRegistry = registry,
            renderer = null,
            configuration = config,
            mapper = mapper
        )
    }

    @AfterTest
    fun teardown() {
        server.stop()
    }

    @Test
    fun `server starts and stops without errors`() {
        // Should not throw
        server.start()
        Thread.sleep(100) // Give server time to start
        server.stop()
    }

    @Test
    fun `can fetch patterns list`() = runBlocking {
        server.start()
        Thread.sleep(100)

        val client = HttpClient(CIO)
        try {
            val response = client.get("http://localhost:8081/api/patterns")
            assertEquals(HttpStatusCode.OK, response.status)

            val body = response.bodyAsText()
            assertTrue(body.contains("Rainbow"), "Response should contain Rainbow pattern")
        } finally {
            client.close()
        }
    }

    @Test
    fun `can fetch configuration`() = runBlocking {
        server.start()
        Thread.sleep(100)

        val client = HttpClient(CIO)
        try {
            val response = client.get("http://localhost:8081/api/config")
            assertEquals(HttpStatusCode.OK, response.status)

            val body = response.bodyAsText()
            assertTrue(body.contains("viewport"), "Response should contain viewport config")
            assertTrue(body.contains("\"width\":10") || body.contains("\"width\": 10"),
                "Response should contain width:10")
        } finally {
            client.close()
        }
    }

    @Test
    fun `can fetch stats`() = runBlocking {
        server.start()
        Thread.sleep(100)

        val client = HttpClient(CIO)
        try {
            val response = client.get("http://localhost:8081/api/stats")
            assertEquals(HttpStatusCode.OK, response.status)

            val body = response.bodyAsText()
            assertTrue(body.contains("fps"), "Response should contain fps")
            assertTrue(body.contains("frameTime"), "Response should contain frameTime")
        } finally {
            client.close()
        }
    }

    @Test
    fun `can fetch client count`() = runBlocking {
        server.start()
        Thread.sleep(100)

        val client = HttpClient(CIO)
        try {
            val response = client.get("http://localhost:8081/api/clients")
            assertEquals(HttpStatusCode.OK, response.status)

            val body = response.bodyAsText()
            assertTrue(body.contains("count"), "Response should contain count")
        } finally {
            client.close()
        }
    }

    @Test
    fun `can post pattern change`() = runBlocking {
        server.start()
        Thread.sleep(100)

        var patternChanged = false
        server.setPatternChangeListener { name, _ ->
            if (name == "Rainbow") {
                patternChanged = true
            }
        }

        val client = HttpClient(CIO)
        try {
            val response = client.post("http://localhost:8081/api/pattern/Rainbow") {
                contentType(ContentType.Application.Json)
                setBody("{\"speed\":2.0,\"brightness\":0.5}")
            }
            assertEquals(HttpStatusCode.OK, response.status)

            Thread.sleep(100) // Give listener time to fire
            assertTrue(patternChanged, "Pattern change listener should have been called")
        } finally {
            client.close()
        }
    }

    @Test
    fun `WebSocket connection works`() = runBlocking {
        server.start()
        Thread.sleep(100)

        val client = HttpClient(CIO) {
            install(io.ktor.client.plugins.websocket.WebSockets)
        }

        try {
            var connected = false
            client.webSocket("ws://localhost:8081/viewport") {
                connected = true
                // Connection successful
                close()
            }

            assertTrue(connected, "Should have connected via WebSocket")
        } finally {
            client.close()
        }
    }

    @Test
    fun `broadcaster tracks client count`() = runBlocking {
        server.start()
        Thread.sleep(100)

        // Initially no clients
        val client1 = HttpClient(CIO)
        val response1 = client1.get("http://localhost:8081/api/clients")
        val body1 = response1.bodyAsText()
        assertTrue(body1.contains("\"count\":0") || body1.contains("\"count\": 0"),
            "Should have 0 clients initially")

        // Connect a WebSocket client
        val client2 = HttpClient(CIO) {
            install(io.ktor.client.plugins.websocket.WebSockets)
        }

        try {
            client2.webSocket("ws://localhost:8081/viewport") {
                Thread.sleep(100)

                // Check client count with a third HTTP client
                val client3 = HttpClient(CIO)
                val response = client3.get("http://localhost:8081/api/clients")
                val body = response.bodyAsText()

                // Should have 1 client now
                assertTrue(body.contains("\"count\":1") || body.contains("\"count\": 1"),
                    "Should have 1 client: $body")
                client3.close()

                close()
            }
        } finally {
            client1.close()
            client2.close()
        }
    }

    @Test
    fun `serves static HTML files`() = runBlocking {
        server.start()
        Thread.sleep(100)

        val client = HttpClient(CIO)
        try {
            val response = client.get("http://localhost:8081/")
            assertEquals(HttpStatusCode.OK, response.status)

            val body = response.bodyAsText()
            assertTrue(body.contains("LED Strip Host"), "Should serve index.html")
        } finally {
            client.close()
        }
    }
}
