# LED Strip Host Application Specification

## 1. Overview

A Kotlin-based host application for driving programmable LED strips through a virtual graphics viewport. The system provides a 2D canvas abstraction that can be driven by various pattern algorithms, with automatic mapping to physical LED strip layouts. Includes a web-based preview interface for real-time visualization and control.

**Implementation Tool**: This spec is designed for implementation with Claude Code, with clear phases and checkpoints.

**Key Goals**:
- Clean separation between pattern logic and LED hardware
- Real-time web-based preview and control
- Easy pattern development and testing
- Flexible LED strip layout configuration

## 2. System Architecture

### 2.1 Core Components

```
┌─────────────────────────────────────────────────────┐
│                  Application Layer                   │
│  (Pattern Selection, Configuration, Startup)        │
└────────────┬────────────────────────────────────────┘
             │
             ├──────────────────────────────────────┐
             │                                      │
┌────────────▼─────────────┐          ┌────────────▼──────────────┐
│     Pattern Engine       │          │      Web Server           │
│  - Pattern Registry      │          │  - HTTP Endpoints         │
│  - Pattern Instances     │          │  - WebSocket Server       │
│  - Parameter Management  │          │  - Static File Serving    │
└────────────┬─────────────┘          └────────────┬──────────────┘
             │                                     │
┌────────────▼─────────────────────────────────────┤
│              Graphics Viewport                   │
│  - 2D Pixel Buffer                               │◄──┐
│  - Drawing Primitives                            │   │
│  - Color Management                              │   │
└────────────┬─────────────────────────────────────┘   │
             │                                          │
┌────────────▼─────────────┐                           │
│      Pixel Mapper        │                           │
│  - Canvas → LED Mapping  │                           │
│  - Strip Layout Config   │                           │
└────────────┬─────────────┘                           │
             │                                          │
┌────────────▼─────────────┐                           │
│     Frame Renderer       │                           │
│  - Frame Rate Control    │                           │
│  - Render Pipeline       │                           │
└────────────┬─────────────┘                           │
             │                                          │
             ├──────────────────────────────────────────┤
             │                                          │
┌────────────▼─────────────┐          ┌────────────────▼─────────┐
│      Output Layer        │          │   WebSocket Broadcaster  │
│  - Hardware Interface    │          │   - Real-time Updates    │
│  - Protocol Encoding     │          │   - Client Management    │
│  - Serial/Network Comm   │          │   - Frame Encoding       │
└──────────────────────────┘          └──────────────────────────┘
                                                      │
                                      ┌───────────────▼──────────┐
                                      │   Browser Client         │
                                      │   - Canvas Renderer      │
                                      │   - UI Controls          │
                                      │   - Pattern Selection    │
                                      └──────────────────────────┘
```

## 3. Component Specifications

### 3.1 Graphics Viewport

**Purpose**: Provides a 2D pixel buffer that patterns can draw to.

**Key Classes**:

```kotlin
interface Viewport {
    val width: Int
    val height: Int
    
    fun setPixel(x: Int, y: Int, color: Color)
    fun getPixel(x: Int, y: Int): Color
    fun fill(color: Color)
    fun clear()
    
    // Drawing primitives
    fun drawLine(x1: Int, y1: Int, x2: Int, y2: Int, color: Color)
    fun drawRect(x: Int, y: Int, width: Int, height: Int, color: Color, filled: Boolean = false)
    fun drawCircle(cx: Int, cy: Int, radius: Int, color: Color, filled: Boolean = false)
    
    // Efficient batch operations
    fun setPixels(pixels: List<Pair<Point, Color>>)
    fun getBuffer(): Array<IntArray> // Returns copy of pixel buffer
}

data class Color(val r: Int, val g: Int, val b: Int) {
    init {
        require(r in 0..255 && g in 0..255 && b in 0..255)
    }
    
    fun toHSV(): HSVColor
    fun toInt(): Int = (r shl 16) or (g shl 8) or b
    
    companion object {
        fun fromHSV(h: Float, s: Float, v: Float): Color
        fun blend(c1: Color, c2: Color, ratio: Float): Color
    }
}

data class Point(val x: Int, val y: Int)
```

**Implementation Notes**:
- Use a 2D array or flat array for pixel storage
- Bounds checking on all pixel operations
- Consider coordinate wrapping modes (clamp, repeat, mirror)

### 3.2 Pixel Mapper

**Purpose**: Maps 2D viewport coordinates to physical LED strip positions.

**Key Classes**:

```kotlin
interface PixelMapper {
    fun mapViewportToLEDs(viewport: Viewport): Map<LEDAddress, Color>
    fun getMapping(): Map<Point, LEDAddress>
}

data class LEDAddress(
    val stripId: Int,
    val ledIndex: Int
)

data class StripLayout(
    val id: Int,
    val length: Int,
    val startPoint: Point,
    val endPoint: Point,
    val reverse: Boolean = false
)

class LinearMapper(private val layouts: List<StripLayout>) : PixelMapper {
    // Maps strips linearly along viewport dimensions
}

class GridMapper(
    private val layouts: List<StripLayout>,
    private val columns: Int,
    private val rows: Int
) : PixelMapper {
    // Maps strips in a grid pattern (e.g., serpentine layout)
}

class CustomMapper(private val mappingFunc: (Point) -> LEDAddress?) : PixelMapper {
    // Allows arbitrary mapping functions
}
```

**Configuration Format** (e.g., JSON/YAML):

```yaml
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
    reverse: true  # Serpentine layout

viewport:
  width: 60
  height: 5
```

### 3.3 Pattern Engine

**Purpose**: Manages and executes pattern algorithms that generate visual effects.

**Key Interfaces**:

```kotlin
interface Pattern {
    val name: String
    val description: String
    
    fun initialize(viewport: Viewport, params: PatternParameters)
    fun update(deltaTime: Float, totalTime: Float)
    fun render(viewport: Viewport)
    fun cleanup()
}

data class PatternParameters(
    private val params: MutableMap<String, Any> = mutableMapOf()
) {
    inline fun <reified T> get(key: String, default: T): T {
        return params[key] as? T ?: default
    }
    
    fun set(key: String, value: Any) {
        params[key] = value
    }
}

interface PatternRegistry {
    fun register(pattern: Pattern)
    fun get(name: String): Pattern?
    fun listPatterns(): List<String>
}
```

**Example Pattern Implementations**:

```kotlin
class RainbowPattern : Pattern {
    override val name = "Rainbow"
    override val description = "Scrolling rainbow effect"
    
    private var hueOffset = 0f
    private var speed = 1f
    
    override fun initialize(viewport: Viewport, params: PatternParameters) {
        speed = params.get("speed", 1f)
    }
    
    override fun update(deltaTime: Float, totalTime: Float) {
        hueOffset += speed * deltaTime
        if (hueOffset >= 360f) hueOffset -= 360f
    }
    
    override fun render(viewport: Viewport) {
        for (y in 0 until viewport.height) {
            for (x in 0 until viewport.width) {
                val hue = (hueOffset + (x * 360f / viewport.width)) % 360f
                viewport.setPixel(x, y, Color.fromHSV(hue, 1f, 1f))
            }
        }
    }
    
    override fun cleanup() {}
}

class FirePattern : Pattern {
    override val name = "Fire"
    override val description = "Flame simulation"
    
    private lateinit var heatMap: Array<FloatArray>
    
    override fun initialize(viewport: Viewport, params: PatternParameters) {
        heatMap = Array(viewport.height) { FloatArray(viewport.width) }
    }
    
    override fun update(deltaTime: Float, totalTime: Float) {
        // Update heat simulation
    }
    
    override fun render(viewport: Viewport) {
        // Render heat map as colors
    }
    
    override fun cleanup() {}
}

class StarfieldPattern : Pattern {
    // Scrolling stars implementation
}

class PlasmaPattern : Pattern {
    // Plasma effect using sine waves
}
```

### 3.4 Frame Renderer

**Purpose**: Controls frame timing and rendering pipeline.

**Key Classes**:

```kotlin
class FrameRenderer(
    private val targetFPS: Int = 60,
    private val viewport: Viewport,
    private val mapper: PixelMapper,
    private val outputs: List<OutputLayer>,
    private val onFrameRendered: ((Viewport) -> Unit)? = null
) {
    private var isRunning = false
    private var currentPattern: Pattern? = null
    private val stats = RenderStats()
    
    fun start() {
        isRunning = true
        Thread {
            renderLoop()
        }.start()
    }
    
    fun stop() {
        isRunning = false
    }
    
    fun setPattern(pattern: Pattern, params: PatternParameters = PatternParameters()) {
        currentPattern?.cleanup()
        currentPattern = pattern
        pattern.initialize(viewport, params)
    }
    
    private fun renderLoop() {
        val frameTime = 1000L / targetFPS
        var lastTime = System.currentTimeMillis()
        var totalTime = 0f
        var frameCount = 0
        var fpsTimer = 0L
        
        while (isRunning) {
            val frameStart = System.currentTimeMillis()
            val currentTime = frameStart
            val deltaTime = (currentTime - lastTime) / 1000f
            
            // Update pattern
            currentPattern?.update(deltaTime, totalTime)
            
            // Render to viewport
            viewport.clear()
            currentPattern?.render(viewport)
            
            // Notify callback (for web broadcast)
            onFrameRendered?.invoke(viewport)
            
            // Map and output to all outputs
            val ledData = mapper.mapViewportToLEDs(viewport)
            outputs.forEach { output ->
                try {
                    output.send(ledData)
                } catch (e: Exception) {
                    // Log error but continue
                    println("Output error: ${e.message}")
                }
            }
            
            // Update statistics
            frameCount++
            fpsTimer += System.currentTimeMillis() - frameStart
            if (fpsTimer >= 1000) {
                stats.fps = frameCount.toFloat()
                stats.frameTime = fpsTimer.toFloat() / frameCount
                frameCount = 0
                fpsTimer = 0
            }
            
            // Frame timing
            val elapsed = System.currentTimeMillis() - frameStart
            val sleepTime = frameTime - elapsed
            if (sleepTime > 0) {
                Thread.sleep(sleepTime)
            } else {
                stats.droppedFrames++
            }
            
            lastTime = currentTime
            totalTime += deltaTime
        }
    }
    
    fun getStatistics(): RenderStats = stats.copy()
}

data class RenderStats(
    var fps: Float = 0f,
    var frameTime: Float = 0f,
    var droppedFrames: Int = 0
)
```

### 3.5 Web Server & Preview

**Purpose**: Provides real-time web-based visualization and control interface.

**Key Classes**:

```kotlin
class PreviewServer(
    private val port: Int = 8080,
    private val viewport: Viewport
) {
    private val clients = mutableListOf<WebSocketSession>()
    private val server: ApplicationEngine
    
    init {
        server = embeddedServer(Netty, port = port) {
            install(WebSockets) {
                pingPeriod = Duration.ofSeconds(15)
                timeout = Duration.ofSeconds(15)
                maxFrameSize = Long.MAX_VALUE
                masking = false
            }
            
            routing {
                // Serve static files (HTML, CSS, JS)
                static("/") {
                    resources("web")
                    defaultResource("web/index.html")
                }
                
                // WebSocket endpoint for real-time updates
                webSocket("/viewport") {
                    clients.add(this)
                    try {
                        for (frame in incoming) {
                            // Handle incoming commands from client
                            when (frame) {
                                is Frame.Text -> handleCommand(frame.readText())
                                else -> {}
                            }
                        }
                    } finally {
                        clients.remove(this)
                    }
                }
                
                // REST API endpoints
                get("/api/patterns") {
                    call.respond(patternRegistry.listPatterns())
                }
                
                post("/api/pattern/{name}") {
                    val name = call.parameters["name"]!!
                    val params = call.receive<Map<String, Any>>()
                    setPattern(name, params)
                    call.respond(HttpStatusCode.OK)
                }
                
                get("/api/config") {
                    call.respond(configuration)
                }
                
                get("/api/stats") {
                    call.respond(renderer.getStatistics())
                }
            }
        }
    }
    
    fun start() {
        server.start(wait = false)
        println("Preview server running at http://localhost:$port")
    }
    
    fun stop() {
        server.stop(1000, 2000)
    }
    
    suspend fun broadcastViewport(viewport: Viewport) {
        val data = encodeViewport(viewport)
        clients.forEach { client ->
            try {
                client.send(Frame.Text(data))
            } catch (e: Exception) {
                // Client disconnected
            }
        }
    }
    
    private fun encodeViewport(viewport: Viewport): String {
        // Option 1: Send as JSON array
        val pixels = mutableListOf<Int>()
        for (y in 0 until viewport.height) {
            for (x in 0 until viewport.width) {
                pixels.add(viewport.getPixel(x, y).toInt())
            }
        }
        return Json.encodeToString(mapOf(
            "type" to "viewport",
            "width" to viewport.width,
            "height" to viewport.height,
            "pixels" to pixels
        ))
        
        // Option 2: Send as base64 PNG (more efficient for larger viewports)
        // return encodeAsPng(viewport)
    }
    
    private fun handleCommand(command: String) {
        // Parse and handle commands from web client
        val cmd = Json.decodeFromString<Command>(command)
        when (cmd.type) {
            "setPattern" -> setPattern(cmd.pattern, cmd.params)
            "setFPS" -> setTargetFPS(cmd.fps)
            else -> {}
        }
    }
}

data class Command(
    val type: String,
    val pattern: String = "",
    val params: Map<String, Any> = emptyMap(),
    val fps: Int = 60
)
```

**Frontend (HTML/JavaScript)**:

`src/main/resources/web/index.html`:
```html
<!DOCTYPE html>
<html>
<head>
    <title>LED Strip Preview</title>
    <style>
        body {
            margin: 0;
            padding: 20px;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
            background: #1a1a1a;
            color: #fff;
        }
        
        #container {
            display: flex;
            gap: 20px;
        }
        
        #preview {
            flex: 1;
            background: #000;
            border: 2px solid #333;
            border-radius: 8px;
            overflow: hidden;
        }
        
        #controls {
            width: 300px;
            background: #2a2a2a;
            padding: 20px;
            border-radius: 8px;
        }
        
        canvas {
            display: block;
            image-rendering: pixelated;
            image-rendering: crisp-edges;
        }
        
        .control-group {
            margin-bottom: 20px;
        }
        
        label {
            display: block;
            margin-bottom: 5px;
            color: #aaa;
            font-size: 12px;
            text-transform: uppercase;
        }
        
        select, input, button {
            width: 100%;
            padding: 8px;
            background: #1a1a1a;
            border: 1px solid #444;
            color: #fff;
            border-radius: 4px;
            font-size: 14px;
        }
        
        button {
            background: #4a9eff;
            border: none;
            cursor: pointer;
            margin-top: 10px;
        }
        
        button:hover {
            background: #3a8eef;
        }
        
        #stats {
            font-family: monospace;
            font-size: 12px;
            color: #888;
            margin-top: 20px;
            padding: 10px;
            background: #1a1a1a;
            border-radius: 4px;
        }
        
        .stat-row {
            display: flex;
            justify-content: space-between;
            margin-bottom: 5px;
        }
        
        #connection-status {
            padding: 8px;
            border-radius: 4px;
            text-align: center;
            font-size: 12px;
            margin-bottom: 20px;
        }
        
        .connected {
            background: #2d5f2d;
            color: #90ee90;
        }
        
        .disconnected {
            background: #5f2d2d;
            color: #ff9090;
        }
    </style>
</head>
<body>
    <div id="container">
        <div id="preview">
            <canvas id="canvas"></canvas>
        </div>
        
        <div id="controls">
            <div id="connection-status" class="disconnected">Disconnected</div>
            
            <div class="control-group">
                <label>Pattern</label>
                <select id="pattern-select">
                    <option value="">Loading...</option>
                </select>
            </div>
            
            <div class="control-group">
                <label>Speed</label>
                <input type="range" id="speed" min="0.1" max="5" step="0.1" value="1">
                <span id="speed-value">1.0x</span>
            </div>
            
            <div class="control-group">
                <label>Brightness</label>
                <input type="range" id="brightness" min="0" max="100" value="100">
                <span id="brightness-value">100%</span>
            </div>
            
            <button onclick="applyPattern()">Apply Pattern</button>
            
            <div id="stats">
                <div class="stat-row">
                    <span>FPS:</span>
                    <span id="fps">--</span>
                </div>
                <div class="stat-row">
                    <span>Frame Time:</span>
                    <span id="frame-time">--</span>
                </div>
                <div class="stat-row">
                    <span>Resolution:</span>
                    <span id="resolution">--</span>
                </div>
            </div>
        </div>
    </div>
    
    <script src="preview.js"></script>
</body>
</html>
```

`src/main/resources/web/preview.js`:
```javascript
class LEDPreview {
    constructor() {
        this.canvas = document.getElementById('canvas');
        this.ctx = this.canvas.getContext('2d');
        this.ws = null;
        this.connected = false;
        this.pixelSize = 10; // Size of each LED in pixels
        this.width = 0;
        this.height = 0;
        
        this.connect();
        this.setupControls();
        this.loadPatterns();
        this.startStatsUpdate();
    }
    
    connect() {
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        this.ws = new WebSocket(`${protocol}//${window.location.host}/viewport`);
        
        this.ws.onopen = () => {
            this.connected = true;
            this.updateConnectionStatus();
            console.log('Connected to LED server');
        };
        
        this.ws.onclose = () => {
            this.connected = false;
            this.updateConnectionStatus();
            console.log('Disconnected from LED server');
            setTimeout(() => this.connect(), 2000); // Reconnect after 2s
        };
        
        this.ws.onmessage = (event) => {
            const data = JSON.parse(event.data);
            if (data.type === 'viewport') {
                this.renderViewport(data);
            }
        };
    }
    
    renderViewport(data) {
        if (this.width !== data.width || this.height !== data.height) {
            this.width = data.width;
            this.height = data.height;
            this.canvas.width = this.width * this.pixelSize;
            this.canvas.height = this.height * this.pixelSize;
            document.getElementById('resolution').textContent = 
                `${this.width}x${this.height}`;
        }
        
        const imageData = this.ctx.createImageData(
            this.width * this.pixelSize,
            this.height * this.pixelSize
        );
        
        for (let y = 0; y < this.height; y++) {
            for (let x = 0; x < this.width; x++) {
                const color = data.pixels[y * this.width + x];
                const r = (color >> 16) & 0xFF;
                const g = (color >> 8) & 0xFF;
                const b = color & 0xFF;
                
                // Draw pixelSize x pixelSize block
                for (let py = 0; py < this.pixelSize; py++) {
                    for (let px = 0; px < this.pixelSize; px++) {
                        const idx = ((y * this.pixelSize + py) * this.canvas.width + 
                                     (x * this.pixelSize + px)) * 4;
                        imageData.data[idx] = r;
                        imageData.data[idx + 1] = g;
                        imageData.data[idx + 2] = b;
                        imageData.data[idx + 3] = 255;
                    }
                }
            }
        }
        
        this.ctx.putImageData(imageData, 0, 0);
    }
    
    updateConnectionStatus() {
        const status = document.getElementById('connection-status');
        if (this.connected) {
            status.textContent = 'Connected';
            status.className = 'connected';
        } else {
            status.textContent = 'Disconnected';
            status.className = 'disconnected';
        }
    }
    
    async loadPatterns() {
        try {
            const response = await fetch('/api/patterns');
            const patterns = await response.json();
            const select = document.getElementById('pattern-select');
            select.innerHTML = patterns.map(p => 
                `<option value="${p}">${p}</option>`
            ).join('');
        } catch (e) {
            console.error('Failed to load patterns:', e);
        }
    }
    
    setupControls() {
        document.getElementById('speed').addEventListener('input', (e) => {
            document.getElementById('speed-value').textContent = 
                `${parseFloat(e.target.value).toFixed(1)}x`;
        });
        
        document.getElementById('brightness').addEventListener('input', (e) => {
            document.getElementById('brightness-value').textContent = 
                `${e.target.value}%`;
        });
    }
    
    async applyPattern() {
        const pattern = document.getElementById('pattern-select').value;
        const speed = parseFloat(document.getElementById('speed').value);
        const brightness = parseInt(document.getElementById('brightness').value) / 100;
        
        try {
            await fetch(`/api/pattern/${pattern}`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({speed, brightness})
            });
        } catch (e) {
            console.error('Failed to apply pattern:', e);
        }
    }
    
    async startStatsUpdate() {
        setInterval(async () => {
            if (!this.connected) return;
            
            try {
                const response = await fetch('/api/stats');
                const stats = await response.json();
                document.getElementById('fps').textContent = 
                    stats.fps.toFixed(1);
                document.getElementById('frame-time').textContent = 
                    `${stats.frameTime.toFixed(2)}ms`;
            } catch (e) {
                // Ignore errors
            }
        }, 1000);
    }
}

// Global function for button onclick
function applyPattern() {
    window.preview.applyPattern();
}

// Initialize when page loads
window.addEventListener('load', () => {
    window.preview = new LEDPreview();
});
```

### 3.6 Output Layer

**Purpose**: Abstracts hardware communication and protocol encoding.

**Key Interfaces**:

```kotlin
interface OutputLayer {
    fun connect()
    fun disconnect()
    fun send(ledData: Map<LEDAddress, Color>)
    fun isConnected(): Boolean
}

// Example implementations:
class SerialOutput(
    private val portName: String,
    private val baudRate: Int = 115200
) : OutputLayer {
    // Implement serial communication
}

class NetworkOutput(
    private val host: String,
    private val port: Int
) : OutputLayer {
    // Implement network communication (e.g., for ESP32)
}

class SimulatorOutput(
    private val window: SimulatorWindow
) : OutputLayer {
    // Visual simulator for testing
}

// Protocol encoders
interface ProtocolEncoder {
    fun encode(ledData: Map<LEDAddress, Color>): ByteArray
}

class FastLEDEncoder : ProtocolEncoder {
    // Encode for FastLED protocol
}

class WS2812BEncoder : ProtocolEncoder {
    // Encode for WS2812B protocol
}
```

## 4. Application Architecture

### 4.1 Main Application

```kotlin
class LEDHostApplication(configPath: String) {
    private val config: Configuration
    private val viewport: Viewport
    private val mapper: PixelMapper
    private val outputs: List<OutputLayer>
    private val renderer: FrameRenderer
    private val patternRegistry: PatternRegistry
    private val webServer: PreviewServer
    
    init {
        config = Configuration.load(configPath)
        viewport = ArrayViewport(config.viewport.width, config.viewport.height)
        mapper = createMapper(config)
        outputs = createOutputs(config)
        
        // Create frame renderer with broadcast hook
        renderer = FrameRenderer(
            targetFPS = config.targetFPS,
            viewport = viewport,
            mapper = mapper,
            outputs = outputs,
            onFrameRendered = { viewport ->
                // Broadcast to web clients
                runBlocking { webServer.broadcastViewport(viewport) }
            }
        )
        
        patternRegistry = DefaultPatternRegistry()
        webServer = PreviewServer(
            port = config.webServer.port,
            viewport = viewport,
            patternRegistry = patternRegistry,
            renderer = renderer
        )
        
        registerDefaultPatterns()
    }
    
    fun start() {
        // Start web server first
        webServer.start()
        
        // Connect outputs
        outputs.forEach { it.connect() }
        
        // Start renderer
        renderer.start()
        
        println("LED Host Application started")
        println("Web preview: http://localhost:${config.webServer.port}")
    }
    
    fun stop() {
        renderer.stop()
        outputs.forEach { it.disconnect() }
        webServer.stop()
        println("LED Host Application stopped")
    }
    
    fun setPattern(patternName: String, params: Map<String, Any> = emptyMap()) {
        val pattern = patternRegistry.get(patternName)
            ?: throw IllegalArgumentException("Pattern not found: $patternName")
        
        renderer.setPattern(pattern, PatternParameters(params.toMutableMap()))
    }
    
    private fun createMapper(config: Configuration): PixelMapper {
        return when (config.mapper.type) {
            "linear" -> LinearMapper(config.strips)
            "grid" -> GridMapper(config.strips, config.mapper.columns, config.mapper.rows)
            else -> throw IllegalArgumentException("Unknown mapper type: ${config.mapper.type}")
        }
    }
    
    private fun createOutputs(config: Configuration): List<OutputLayer> {
        val outputs = mutableListOf<OutputLayer>()
        
        // Add hardware output if configured
        when (config.output.type) {
            "serial" -> {
                outputs.add(SerialOutput(
                    portName = config.output.parameters["port"] ?: "/dev/ttyUSB0",
                    baudRate = config.output.parameters["baudRate"]?.toInt() ?: 115200
                ))
            }
            "network" -> {
                outputs.add(NetworkOutput(
                    host = config.output.parameters["host"] ?: "192.168.1.100",
                    port = config.output.parameters["port"]?.toInt() ?: 7890
                ))
            }
        }
        
        return outputs
    }
    
    private fun registerDefaultPatterns() {
        patternRegistry.register(RainbowPattern())
        patternRegistry.register(FirePattern())
        patternRegistry.register(StarfieldPattern())
        patternRegistry.register(PlasmaPattern())
        patternRegistry.register(SolidColorPattern())
    }
}

// Main entry point
fun main(args: Array<String>) {
    val configPath = args.firstOrNull() ?: "config.yaml"
    
    val app = LEDHostApplication(configPath)
    
    // Graceful shutdown
    Runtime.getRuntime().addShutdownHook(Thread {
        println("\nShutting down...")
        app.stop()
    })
    
    app.start()
    
    // Start with rainbow pattern
    app.setPattern("Rainbow", mapOf("speed" to 1.0f))
    
    // Keep main thread alive
    Thread.currentThread().join()
}
```

### 4.2 Configuration

```kotlin
data class Configuration(
    val viewport: ViewportConfig,
    val strips: List<StripLayout>,
    val mapper: MapperConfig,
    val output: OutputConfig,
    val webServer: WebServerConfig,
    val targetFPS: Int = 60
) {
    companion object {
        fun load(path: String): Configuration {
            // Load from YAML using kaml
            val yaml = Yaml.default
            val file = File(path)
            return yaml.decodeFromString(serializer(), file.readText())
        }
    }
}

@Serializable
data class ViewportConfig(
    val width: Int,
    val height: Int
)

@Serializable
data class MapperConfig(
    val type: String = "linear", // "linear", "grid", "custom"
    val columns: Int = 1,
    val rows: Int = 1
)

@Serializable
data class OutputConfig(
    val type: String, // "serial", "network", "preview"
    val parameters: Map<String, String> = emptyMap()
)

@Serializable
data class WebServerConfig(
    val port: Int = 8080,
    val enabled: Boolean = true
)
```

## 5. Usage Examples

### 5.1 Basic Usage

```kotlin
fun main() {
    val app = LEDHostApplication("config.yaml")
    
    // Start with rainbow pattern
    app.setPattern("Rainbow", mapOf("speed" to 2.0f))
    app.start()
    
    // Change pattern after 10 seconds
    Thread.sleep(10000)
    app.setPattern("Fire")
    
    // Run for 30 more seconds
    Thread.sleep(30000)
    app.stop()
}
```

### 5.2 CLI Application

```kotlin
class LEDHostCLI {
    fun main(args: Array<String>) {
        val parser = ArgParser("led-host")
        val configPath by parser.option(ArgType.String, shortName = "c", description = "Config file path")
            .default("config.yaml")
        val pattern by parser.option(ArgType.String, shortName = "p", description = "Pattern name")
            .default("Rainbow")
        
        parser.parse(args)
        
        val app = LEDHostApplication(configPath)
        
        // Set up shutdown hook
        Runtime.getRuntime().addShutdownHook(Thread {
            app.stop()
        })
        
        app.setPattern(pattern)
        app.start()
        
        // Interactive mode
        println("LED Host running. Commands: pattern <name>, quit")
        while (true) {
            val input = readLine() ?: break
            when {
                input.startsWith("pattern ") -> {
                    val patternName = input.substringAfter("pattern ").trim()
                    app.setPattern(patternName)
                }
                input == "quit" -> break
            }
        }
        
        app.stop()
    }
}
```

## 6. Advanced Features

### 6.1 Pattern Transitions

```kotlin
interface Transition {
    fun blend(from: Viewport, to: Viewport, progress: Float): Viewport
}

class FadeTransition : Transition {
    override fun blend(from: Viewport, to: Viewport, progress: Float): Viewport {
        // Alpha blend between viewports
    }
}

class WipeTransition : Transition {
    // Directional wipe effect
}
```

### 6.2 Pattern Composition

```kotlin
class CompositePattern(
    private val patterns: List<Pattern>,
    private val blendMode: BlendMode = BlendMode.ADD
) : Pattern {
    // Combine multiple patterns
}

enum class BlendMode {
    ADD, MULTIPLY, SCREEN, OVERLAY
}
```

### 6.3 Audio Reactivity

```kotlin
interface AudioAnalyzer {
    fun getBeat(): Boolean
    fun getFrequencyBands(): FloatArray
    fun getAmplitude(): Float
}

class AudioReactivePattern(
    private val basePattern: Pattern,
    private val analyzer: AudioAnalyzer
) : Pattern {
    // Modulate pattern based on audio
}
```

## 7. Testing Strategy

### 7.1 Unit Tests
- Viewport operations (pixel set/get, drawing primitives)
- Color conversions and blending
- Pixel mapping accuracy
- Pattern parameter handling

### 7.2 Integration Tests
- Full render pipeline
- Pattern transitions
- Configuration loading
- Output layer communication

### 7.3 Visual Testing
- Simulator output for visual verification
- Reference pattern comparisons
- Performance profiling

## 8. Performance Considerations

1. **Memory Efficiency**
   - Use primitive arrays for pixel buffers
   - Pool color objects where possible
   - Minimize allocations in render loop

2. **CPU Optimization**
   - Batch pixel operations
   - Use lookup tables for color conversions
   - Consider parallel processing for complex patterns

3. **Frame Timing**
   - Accurate frame rate control
   - Handle rendering slower than target FPS gracefully
   - Monitor and report frame statistics

4. **Communication Optimization**
   - Delta compression (only send changed pixels)
   - Buffer outgoing data
   - Protocol-specific optimizations

## 9. Extension Points

1. **Custom Patterns**: Implement `Pattern` interface
2. **Custom Mappers**: Implement `PixelMapper` interface
3. **Custom Output**: Implement `OutputLayer` interface
4. **Custom Transitions**: Implement `Transition` interface
5. **Pattern Effects**: Modifiers that can be applied to patterns

## 10. Dependencies

### Recommended Libraries

```kotlin
// build.gradle.kts
plugins {
    kotlin("jvm") version "1.9.20"
    kotlin("plugin.serialization") version "1.9.20"
    application
}

dependencies {
    // Core
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.20")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // Web Server (Ktor)
    val ktorVersion = "2.3.7"
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-websockets:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    
    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    
    // Configuration
    implementation("com.charleskorn.kaml:kaml:0.55.0") // YAML
    
    // CLI (optional, for command-line interface)
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.6")
    
    // Serial Communication
    implementation("com.fazecast:jSerialComm:2.10.4")
    
    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.14")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    
    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("io.ktor:ktor-server-tests:$ktorVersion")
}

application {
    mainClass.set("com.timberglund.ledhost.ApplicationKt")
}
```

## 11. Project Structure

```
led-host-application/
├── src/
│   ├── main/
│   │   ├── kotlin/
│   │   │   └── com/timberglund/ledhost/
│   │   │       ├── Application.kt
│   │   │       ├── viewport/
│   │   │       │   ├── Viewport.kt
│   │   │       │   ├── ArrayViewport.kt
│   │   │       │   └── Color.kt
│   │   │       ├── mapper/
│   │   │       │   ├── PixelMapper.kt
│   │   │       │   ├── LinearMapper.kt
│   │   │       │   └── GridMapper.kt
│   │   │       ├── pattern/
│   │   │       │   ├── Pattern.kt
│   │   │       │   ├── PatternRegistry.kt
│   │   │       │   └── patterns/
│   │   │       │       ├── RainbowPattern.kt
│   │   │       │       ├── FirePattern.kt
│   │   │       │       └── ...
│   │   │       ├── renderer/
│   │   │       │   └── FrameRenderer.kt
│   │   │       ├── output/
│   │   │       │   ├── OutputLayer.kt
│   │   │       │   ├── SerialOutput.kt
│   │   │       │   └── NetworkOutput.kt
│   │   │       ├── web/
│   │   │       │   ├── PreviewServer.kt
│   │   │       │   └── WebSocketBroadcaster.kt
│   │   │       └── config/
│   │   │           └── Configuration.kt
│   │   └── resources/
│   │       ├── web/
│   │       │   ├── index.html
│   │       │   └── preview.js
│   │       └── logback.xml
│   └── test/
│       └── kotlin/
│           └── com/timberglund/ledhost/
│               └── ...
├── config.yaml
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## 12. Implementation Guide for Claude Code

This section provides a structured approach for implementing the LED Host Application using Claude Code. Each phase builds on the previous, with clear checkpoints.

### Phase 0: Project Setup

**Objective**: Create the project structure and configure dependencies.

**Tasks**:
1. Create project directory structure (see section 11)
2. Create `build.gradle.kts` with all dependencies (see section 10)
3. Create `settings.gradle.kts`:
   ```kotlin
   rootProject.name = "led-host-application"
   ```
4. Create basic `config.yaml`:
   ```yaml
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
     type: "preview"  # Start with web preview only
   
   targetFPS: 60
   webServer:
     port: 8080
   ```

**Checkpoint**: Project builds successfully with `./gradlew build`

---

### Phase 1: Core Data Structures

**Objective**: Implement the foundational viewport and color classes.

**Files to Create**:
1. `src/main/kotlin/com/timberglund/ledhost/viewport/Color.kt`
   - Implement `Color` data class with RGB values
   - Add `toInt()`, `toHSV()`, `fromHSV()` methods
   - Add `blend()` function for color interpolation

2. `src/main/kotlin/com/timberglund/ledhost/viewport/Viewport.kt`
   - Define `Viewport` interface (see section 3.1)
   
3. `src/main/kotlin/com/timberglund/ledhost/viewport/ArrayViewport.kt`
   - Implement `Viewport` using 2D integer array
   - Implement `setPixel()`, `getPixel()`, `fill()`, `clear()`
   - Add basic drawing primitives (line, rect, circle)

**Tests to Create**:
- Test color conversions (RGB ↔ HSV)
- Test viewport pixel operations
- Test drawing primitives

**Checkpoint**: 
- All tests pass
- Can create a viewport and set/get pixels
- Color conversions work correctly

---

### Phase 2: Simple Pattern System

**Objective**: Create the pattern interface and one working pattern.

**Files to Create**:
1. `src/main/kotlin/com/timberglund/ledhost/pattern/Pattern.kt`
   - Define `Pattern` interface (see section 3.3)
   - Define `PatternParameters` class

2. `src/main/kotlin/com/timberglund/ledhost/pattern/PatternRegistry.kt`
   - Implement `PatternRegistry` interface
   - Create `DefaultPatternRegistry` implementation

3. `src/main/kotlin/com/timberglund/ledhost/pattern/patterns/RainbowPattern.kt`
   - Implement a simple rainbow scroll pattern (see section 3.3)
   - Use HSV color space for smooth rainbow

**Tests to Create**:
- Test pattern registration
- Test pattern initialization
- Test rainbow pattern renders correctly

**Checkpoint**:
- Can register and retrieve patterns
- Rainbow pattern can render to viewport
- Viewport shows expected rainbow colors

---

### Phase 3: Frame Renderer

**Objective**: Implement the rendering loop with accurate timing.

**Files to Create**:
1. `src/main/kotlin/com/timberglund/ledhost/renderer/FrameRenderer.kt`
   - Implement frame timing loop (see section 3.4)
   - Add FPS control and statistics
   - Make it thread-safe

**Tests to Create**:
- Test frame timing accuracy
- Test pattern switching
- Test statistics collection

**Checkpoint**:
- Renderer runs at target FPS
- Can switch patterns while running
- Statistics are accurate

---

### Phase 4: Configuration System

**Objective**: Load configuration from YAML files.

**Files to Create**:
1. `src/main/kotlin/com/timberglund/ledhost/config/Configuration.kt`
   - Implement configuration data classes (see section 4.2)
   - Add YAML loading with kaml library
   - Add validation

**Tests to Create**:
- Test configuration loading
- Test invalid configuration handling
- Test default values

**Checkpoint**:
- Can load `config.yaml` successfully
- Invalid configs throw helpful errors
- All config fields are accessible

---

### Phase 5: Web Server & Preview

**Objective**: Create the web-based preview interface.

**Files to Create**:
1. `src/main/kotlin/com/timberglund/ledhost/web/PreviewServer.kt`
   - Implement Ktor server (see section 3.5)
   - Add WebSocket endpoint
   - Add REST API endpoints
   - Implement viewport encoding

2. `src/main/resources/web/index.html`
   - Create preview UI (see section 3.5)

3. `src/main/resources/web/preview.js`
   - Implement WebSocket client (see section 3.5)
   - Add canvas rendering
   - Add pattern controls

4. `src/main/kotlin/com/timberglund/ledhost/web/WebSocketBroadcaster.kt`
   - Manage WebSocket connections
   - Broadcast viewport updates efficiently

**Tests to Create**:
- Test WebSocket connection
- Test REST API endpoints
- Test viewport encoding/decoding

**Checkpoint**:
- Web server starts on port 8080
- Can connect via browser to http://localhost:8080
- Viewport renders in browser canvas
- Pattern selection works

---

### Phase 6: Pixel Mapper

**Objective**: Map 2D viewport to LED strip addresses.

**Files to Create**:
1. `src/main/kotlin/com/timberglund/ledhost/mapper/PixelMapper.kt`
   - Define `PixelMapper` interface (see section 3.2)
   - Define `LEDAddress` and `StripLayout` data classes

2. `src/main/kotlin/com/timberglund/ledhost/mapper/LinearMapper.kt`
   - Implement linear strip mapping
   - Support reverse direction

3. `src/main/kotlin/com/timberglund/ledhost/mapper/GridMapper.kt`
   - Implement grid/serpentine layout
   - Support multi-row configurations

**Tests to Create**:
- Test linear mapping accuracy
- Test grid mapping with serpentine
- Test reverse direction handling

**Checkpoint**:
- Mapper correctly translates viewport coordinates to LED addresses
- Configuration-based strip layouts work
- Can visualize mapping in web preview

---

### Phase 7: Output Layer

**Objective**: Send data to physical LED hardware.

**Files to Create**:
1. `src/main/kotlin/com/timberglund/ledhost/output/OutputLayer.kt`
   - Define `OutputLayer` interface (see section 3.6)

2. `src/main/kotlin/com/timberglund/ledhost/output/SerialOutput.kt`
   - Implement serial communication
   - Add protocol encoding (FastLED/WS2812B)
   - Handle connection errors

3. `src/main/kotlin/com/timberglund/ledhost/output/MultiOutput.kt`
   - Support multiple simultaneous outputs
   - Allows web preview + hardware simultaneously

**Tests to Create**:
- Test serial port enumeration
- Test protocol encoding
- Test error handling

**Checkpoint**:
- Can connect to serial port
- Data is correctly formatted
- Both web preview and hardware work simultaneously

---

### Phase 8: Main Application

**Objective**: Tie everything together with a runnable application.

**Files to Create**:
1. `src/main/kotlin/com/timberglund/ledhost/Application.kt`
   - Implement `LEDHostApplication` class (see section 4.1)
   - Add startup/shutdown logic
   - Integrate all components

2. `src/main/kotlin/com/timberglund/ledhost/Main.kt`
   - Create main entry point
   - Parse command-line arguments (optional)
   - Add graceful shutdown

**Checkpoint**:
- Application starts cleanly
- All components initialize
- Web preview shows pattern
- Hardware receives data (if connected)
- Ctrl+C shuts down gracefully

---

### Phase 9: Additional Patterns

**Objective**: Expand the pattern library.

**Files to Create**:
1. `src/main/kotlin/com/timberglund/ledhost/pattern/patterns/SolidColorPattern.kt`
2. `src/main/kotlin/com/timberglund/ledhost/pattern/patterns/PlasmaPattern.kt`
3. `src/main/kotlin/com/timberglund/ledhost/pattern/patterns/FirePattern.kt`
4. `src/main/kotlin/com/timberglund/ledhost/pattern/patterns/StarfieldPattern.kt`
5. `src/main/kotlin/com/timberglund/ledhost/pattern/patterns/WavePattern.kt`

**Pattern Ideas**:
- Solid Color: Single static color
- Plasma: Sine wave interference patterns
- Fire: Heat simulation with flame colors
- Starfield: Twinkling stars
- Wave: Scrolling sine waves
- Sparkle: Random sparkles
- Breathing: Smooth fade in/out
- Theater Chase: Classic LED chase effect

**Checkpoint**:
- All patterns work in web preview
- Patterns have configurable parameters
- Can switch between patterns smoothly

---

### Phase 10: Polish & Testing

**Objective**: Finalize and test the complete system.

**Tasks**:
1. Add comprehensive error handling
2. Add logging with logback
3. Optimize performance (profile and fix bottlenecks)
4. Add README.md with usage instructions
5. Create example configurations
6. Add keyboard shortcuts to web UI (optional)
7. Add pattern presets/favorites (optional)
8. Performance testing with multiple strips

**Checkpoint**:
- No crashes or errors in normal operation
- Logging is helpful for debugging
- Performance is smooth (consistent FPS)
- Documentation is clear

---

### Implementation Tips for Claude Code

1. **Build Incrementally**: After each file, run `./gradlew build` to catch errors early

2. **Test As You Go**: Write tests alongside implementation, not after

3. **Use the Web Preview Early**: Phase 5 should be done relatively early so you can visually verify everything

4. **Start Simple**: Begin with a small viewport (e.g., 10x3) and one strip before scaling up

5. **Commit Often**: Use git to save progress after each phase

6. **Debug with Web Interface**: The web preview is invaluable for debugging patterns and mappings

7. **Test Without Hardware**: Use web preview and MultiOutput to develop without physical LEDs

### Common Issues & Solutions

**Issue**: WebSocket disconnects frequently
- **Solution**: Check ping/pong settings in Ktor WebSocket config

**Issue**: Viewport updates are choppy
- **Solution**: Reduce viewport size or increase pixel size in web client

**Issue**: Colors don't match between preview and hardware
- **Solution**: Check gamma correction, LED strip color order (RGB vs GRB)

**Issue**: High CPU usage
- **Solution**: Profile render loop, optimize pattern algorithms, reduce FPS

---

### Testing Strategy Summary

**Unit Tests** (run after each phase):
```bash
./gradlew test
```

**Integration Test** (manual, after Phase 8):
1. Start application
2. Open browser to http://localhost:8080
3. Verify rainbow pattern displays
4. Change patterns via UI
5. Verify FPS is stable
6. Test with hardware (if available)

**Performance Test** (after Phase 10):
1. Monitor CPU usage
2. Check frame timing consistency
3. Test with maximum viewport size
4. Profile hot paths if needed

---

### Quick Start Commands

```bash
# Initial setup
mkdir led-host-application
cd led-host-application

# Run application
./gradlew run

# Run with custom config
./gradlew run --args="--config=/path/to/config.yaml"

# Build distributable
./gradlew installDist

# Run tests
./gradlew test

# Clean build
./gradlew clean build
```

---

## ADDENDUM: Web-Based Preview Integration

### Overview

The LED Host Application includes a web-based preview system that allows you to visualize patterns in real-time through a browser. This is particularly useful for:
- Developing patterns without hardware connected
- Remote monitoring from phones/tablets
- Demonstrating patterns to others
- Debugging pixel mapping issues

### Quick Start for Web Preview

When you run the application with web preview enabled (default), you'll see:
```
LED Host Application started
Web preview: http://localhost:8080
```

Simply open that URL in any browser on your network to see the live preview.

### Configuration

In your `config.yaml`, add:
```yaml
webServer:
  port: 8080
  enabled: true
```

### Accessing from Other Devices

To access from your phone or tablet while standing near your physical LED strips:
1. Find your computer's IP address:
   - Linux/Mac: `ip addr show` or `ifconfig`
   - Windows: `ipconfig`
2. Open browser on your device
3. Navigate to `http://<your-computer-ip>:8080`

Example: `http://192.168.1.50:8080`

### Implementation Priority for Claude Code

When implementing with Claude Code, **prioritize the web preview early** (Phase 5 in the implementation guide). Here's why:

1. **Visual Feedback**: You can see immediately if patterns work correctly
2. **No Hardware Needed**: Develop and test without LED strips connected
3. **Faster Iteration**: Change code, reload browser, see results
4. **Better Debugging**: Visualize what the mapper is doing

### Integration Points

The web preview integrates at these key points:

```kotlin
// In FrameRenderer - after each frame is rendered
private fun renderLoop() {
    // ... update and render pattern ...
    
    // Broadcast to web clients
    onFrameRendered?.invoke(viewport)
    
    // ... continue with hardware output ...
}
```

```kotlin
// In Application - setup callback
renderer = FrameRenderer(
    targetFPS = config.targetFPS,
    viewport = viewport,
    mapper = mapper,
    outputs = outputs,
    onFrameRendered = { viewport ->
        runBlocking { webServer.broadcastViewport(viewport) }
    }
)
```

### Testing Without Hardware

You can develop the entire system without LED strips:

1. Set output type to `"preview"` in config.yaml:
   ```yaml
   output:
     type: "preview"
   ```

2. Focus on these components first:
   - Viewport and Color classes
   - Pattern implementations  
   - Pixel mapper
   - Web server

3. Add hardware output later once patterns work in the web preview

### Performance Considerations

**Browser Rendering**: The web preview is lightweight. For a typical 60x5 viewport at 60 FPS:
- Data per frame: ~1.2 KB (300 pixels × 4 bytes)
- Bandwidth: ~72 KB/s
- CPU: Minimal (canvas rendering is GPU-accelerated)

**Network**: WebSocket over localhost has <1ms latency

**Best Practices**:
- Keep viewport size reasonable (<1000 pixels total)
- Limit web clients to 5-10 simultaneous connections
- Pixel size in web UI can be adjusted for better visibility

### Troubleshooting Web Preview

**Browser shows "Disconnected"**:
- Check if application is running
- Verify port is not blocked by firewall
- Check console for WebSocket errors

**Preview is choppy**:
- Reduce viewport size
- Increase pixel size in web UI
- Check FPS stats - might be frame drops

**Can't connect from phone**:
- Ensure computer and phone on same network
- Check firewall allows incoming connections on port 8080
- Try using computer's IP address instead of localhost

### Example: Complete Workflow

1. **Start Development**:
   ```bash
   ./gradlew run
   ```

2. **Open Preview**:
   Open http://localhost:8080 in browser

3. **Develop Pattern**:
   - Write pattern code
   - Save file
   - Restart application (or use hot reload if implemented)
   - See changes immediately in browser

4. **Test on Hardware**:
   - Add serial/network output to config
   - Run application
   - Both web preview and hardware show same pattern

5. **Remote Control**:
   - Open preview on phone
   - Stand near physical LEDs
   - Change patterns via web UI
   - See changes on both phone and LEDs

### Advanced: Custom Web UI

The web frontend is just HTML/JS in `src/main/resources/web/`. You can customize:

- **Colors/Theme**: Edit CSS in `index.html`
- **Layout**: Modify HTML structure
- **Controls**: Add sliders, buttons in `index.html`
- **Features**: Extend `preview.js` for new functionality

Example: Add a color picker:
```html
<div class="control-group">
    <label>Base Color</label>
    <input type="color" id="color-picker" value="#ff0000">
</div>
```

```javascript
document.getElementById('color-picker').addEventListener('change', (e) => {
    // Send to backend
    fetch('/api/pattern/SolidColor', {
        method: 'POST',
        body: JSON.stringify({ color: e.target.value })
    });
});
```

## 13. React Frontend Conversion

### Overview

Convert the vanilla JS frontend (`index.html` + `preview.js`) into a React + TypeScript application using Vite. The backend stays unchanged. Build output replaces current files in `src/main/resources/web/`.

### Architecture

- **Build tool**: Vite with React + TypeScript
- **Source location**: `frontend/` at project root
- **State management**: `useState` in App + custom hooks (no Redux/Zustand)
- **High-frequency rendering**: `useRef` + `requestAnimationFrame` to bypass React re-renders
- **CSS**: Single `App.css` porting existing styles
- **Components**: 11 components + 6 custom hooks

### Performance Strategy

WebSocket viewport data (20 FPS) bypasses React entirely:
1. `useWebSocket` stores messages in `useRef` (no `useState`)
2. `useViewportRenderer` reads the ref in a `requestAnimationFrame` loop
3. Canvas is painted directly via `canvasRef.current.getContext('2d')`

React re-renders only for: connection status, user interactions, stats (1/sec), view toggles.

### Project Structure

```
frontend/
   package.json
   tsconfig.json
   vite.config.ts
   index.html
   src/
      main.tsx
      App.tsx
      App.css
      types.ts
      components/
         ConnectionStatus.tsx
         ControlsSidebar.tsx
         PatternSelector.tsx
         SpeedSlider.tsx
         BrightnessSlider.tsx
         ApplyPatternButton.tsx
         ViewToggles.tsx
         StatsDisplay.tsx
         PreviewArea.tsx
         ViewportCanvas.tsx
         LEDStripsCanvas.tsx
      hooks/
         useWebSocket.ts
         usePatterns.ts
         useStats.ts
         useLEDStrips.ts
         useViewportRenderer.ts
         useLEDStripsRenderer.ts
```

### Implementation Phases

#### Phase R1: Scaffolding
1. `npm create vite@latest frontend -- --template react-ts`
2. Configure `vite.config.ts` (outDir → `../src/main/resources/web`, proxy for `/api` and `/viewport`)
3. Create `types.ts` (ViewportMessage, RenderStats, ClientCount, LEDStripData, PatternParams)
4. Update `.gitignore` (add `frontend/node_modules/`)

**Checkpoint**: `npm run dev` starts Vite dev server, proxies to Ktor backend

#### Phase R2: Custom Hooks
5. `useWebSocket.ts` — WebSocket connection, auto-reconnect, stores viewport data in useRef
6. `usePatterns.ts` — Fetch `/api/patterns` on mount
7. `useStats.ts` — Poll `/api/stats` + `/api/clients` every 1s
8. `useLEDStrips.ts` — Poll `/api/led-strips` at 50ms when enabled, stores in useRef
9. `useViewportRenderer.ts` — RAF loop, Base64 decode, canvas putImageData
10. `useLEDStripsRenderer.ts` — RAF loop, LED squares on overlay canvas

**Checkpoint**: Hooks are importable and TypeScript compiles

#### Phase R3: Leaf Components
11. `ConnectionStatus.tsx` — green/red indicator
12. `PatternSelector.tsx` — dropdown from patterns list
13. `SpeedSlider.tsx` — range 0.1-5.0 with formatted display
14. `BrightnessSlider.tsx` — range 0-100 with formatted display
15. `ApplyPatternButton.tsx` — calls onApply
16. `ViewToggles.tsx` — viewport/strips checkboxes
17. `StatsDisplay.tsx` — monospace stats box

**Checkpoint**: Components render in isolation

#### Phase R4: Canvas & Composite Components
18. `ViewportCanvas.tsx` — `React.memo`, canvas ref, wired to `useViewportRenderer`
19. `LEDStripsCanvas.tsx` — `React.memo`, canvas ref, wired to `useLEDStripsRenderer`
20. `PreviewArea.tsx` — container with both canvases stacked
21. `ControlsSidebar.tsx` — layout wrapper for all control components

**Checkpoint**: Canvas renders viewport data from WebSocket

#### Phase R5: Assembly & Styling
22. `App.css` — port all CSS from `index.html` (lines 7-223)
23. `App.tsx` — wire state, hooks, and components together
24. `main.tsx` — React entry point
25. `frontend/index.html` — minimal Vite entry

**Checkpoint**: Full app works in `npm run dev` against running Ktor backend

#### Phase R6: Build & Deploy
26. `npm run build` → verify output in `src/main/resources/web/`
27. Start Ktor server alone → verify React app loads from built static files
28. Delete old `index.html` and `preview.js` from git (now build artifacts)

**Checkpoint**: Ktor serves the React app identically to the old vanilla JS version

### Vite Configuration

```typescript
export default defineConfig({
   plugins: [react()],
   base: '/',
   build: {
      outDir: '../src/main/resources/web',
      emptyOutDir: true,
   },
   server: {
      proxy: {
         '/api': 'http://localhost:8080',
         '/viewport': { target: 'ws://localhost:8080', ws: true },
      },
   },
});
```

### Key Files to Reference During Implementation

- `src/main/resources/web/preview.js` — All logic to port (WebSocket, rendering, API calls)
- `src/main/resources/web/index.html` — CSS styles and HTML structure to decompose
- `src/main/kotlin/com/timberglund/ledhost/web/PreviewServer.kt` — API contract

