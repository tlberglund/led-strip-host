## 1. Backend — Data Capture

- [x] 1.1 Refactor `buildStripFrames` in `Application.kt` to return `Map<Int, Pair<ByteArray, Array<Color>>>` (frame bytes paired with the color array for each strip)
- [x] 1.2 Update the call site in `onFrameRendered` to destructure the new return type; continue passing `frame` bytes to `bleManager.sendFrame` as before

## 2. Backend — Message & Broadcast

- [x] 2.1 Add `StripLedsMessage` serializable data class to `PreviewServer.kt` (`type = "strip_leds"`, `stripId: Int`, `rgb: String`)
- [x] 2.2 Add a `toRgbHex(leds: Array<Color>): String` helper that produces the compact lowercase hex string (`RRGGBB` per LED, no separator)
- [x] 2.3 Add `broadcast(message: StripLedsMessage)` overload to `StripsWsBroadcaster`
- [x] 2.4 Add `broadcastLeds(messages: List<StripLedsMessage>)` method to `PreviewServer`
- [x] 2.5 In `Application.kt`, inside the existing `broadcastIntervalMs` gate, call `previewServer?.broadcastLeds(...)` with one `StripLedsMessage` per strip using the color arrays from task 1.1

## 3. Frontend — TypeScript Types

- [x] 3.1 Add `StripLedsMessage` interface (`type: 'strip_leds'`, `stripId: number`, `rgb: string`) to `types.ts`
- [x] 3.2 Add `StripLedsMessage` to the `StripsWsMessage` union type in `types.ts`

## 4. Frontend — WebSocket Hook

- [x] 4.1 Add `stripLeds` state (`Record<number, number[]>`) to `useStripsWebSocket`, initialised to `{}`; the value is a flat array of RGB triples `[r, g, b, r, g, b, …]` decoded from the hex string
- [x] 4.2 In the `onmessage` handler, add a case for `strip_leds` that decodes `msg.rgb` into a flat `number[]` of RGB triples and updates `stripLeds[msg.stripId]`
- [x] 4.3 Return `stripLeds` from the hook

## 5. Frontend — LED Canvas Component

- [x] 5.1 Create `LedStrip.tsx` functional component accepting `rgbValues: number[]` (flat RGB triples), `ledCount: number`, and `cellSize?: number` (default 4) props
- [x] 5.2 Use a `useRef<HTMLCanvasElement>` and `useEffect` to draw filled `cellSize × cellSize` rectangles for each LED using `ctx.fillRect`; re-draw whenever `rgbValues` changes
- [x] 5.3 When `rgbValues` is empty or shorter than expected, fill missing LEDs with `#111` (dark placeholder)
- [x] 5.4 Set canvas `width = ledCount * cellSize` and `height = cellSize`; apply CSS `width: 100%; height: auto` so it scales within the card

## 6. Frontend — Strip Card Integration

- [x] 6.1 Destructure `stripLeds` from `useStripsWebSocket` in `StripManagerTab.tsx`
- [x] 6.2 Render `<LedStrip rgbValues={stripLeds[strip.id] ?? []} ledCount={strip.length} />` inside each strip card, below the strip meta line and above the telemetry section
- [x] 6.3 Add CSS for the LED strip canvas container (`.strip-led-canvas`) — a thin block with `overflow: hidden` and a dark background for the placeholder state
