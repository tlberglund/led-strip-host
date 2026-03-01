// WebSocket viewport message (decoded from binary frame)
// Wire format: [flags:1B][width:2B][height:2B][RGB pixel data...]
export interface ViewportMessage {
   width: number;
   height: number;
   data: Uint8Array; // Raw RGB bytes (3 per pixel, row-major)
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

// Parameter definition types from GET /api/patterns
export interface FloatParamDef {
   type: 'float';
   name: string;
   label: string;
   min: number;
   max: number;
   step: number;
   default: number;
}

export interface IntParamDef {
   type: 'int';
   name: string;
   label: string;
   min: number;
   max: number;
   step: number;
   default: number;
}

export interface SelectParamDef {
   type: 'select';
   name: string;
   label: string;
   options: string[];
   default: string;
}

export interface ColorParamDef {
   type: 'color';
   name: string;
   label: string;
   default: string; // hex "#RRGGBB"
}

export type ParameterDef = FloatParamDef | IntParamDef | SelectParamDef | ColorParamDef;

// Strip connection status from GET /api/strips
export interface StripStatus {
   id: number;
   name: string;
   address: string;
   connected: boolean;
   length: number;
}

// Pattern info with metadata from GET /api/patterns
export interface PatternInfo {
   name: string;
   description: string;
   parameters: ParameterDef[];
}
