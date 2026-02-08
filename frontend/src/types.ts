// WebSocket viewport message from the server
export interface ViewportMessage {
   type: 'viewport';
   width: number;
   height: number;
   data: string; // Base64-encoded RGBA bitmap
}

// Stats from GET /api/stats
export interface RenderStats {
   fps: number;
   frameTime: number;
}

// Client count from GET /api/clients
export interface ClientCount {
   count: number;
}

// Single LED position and color from GET /api/led-strips
export interface LEDData {
   x: number;
   y: number;
   r: number;
   g: number;
   b: number;
}

// A strip with its LEDs from GET /api/led-strips
export interface LEDStripData {
   id: number;
   leds: LEDData[];
}

// Pattern parameters sent to POST /api/pattern/{name}
export interface PatternParams {
   speed: number;
   brightness: number;
}
