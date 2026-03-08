## Context

The frame renderer in `Application.kt` already computes per-LED RGB colors for every strip on each render cycle via `buildStripFrames`, which calls `mapper.mapViewportToLEDs(viewport)`. These colors are currently only used to build the BLE wire frame (encoded as `brightness, R, G, B` bytes) and are discarded after the frame is sent. The WebSocket broadcaster already throttles viewport previews to 20 FPS using a timestamp comparison in the render callback. The `/ws/strips` channel is already established and connected to the `StripManagerTab` frontend.

## Goals / Non-Goals

**Goals:**
- Capture per-LED colors as part of the existing frame-build pass (zero additional mapper work)
- Broadcast colors to the frontend at the existing 20 FPS WebSocket throttle rate
- Render each strip's LEDs as a row of colored squares on a `<canvas>` element in the strip card, updating in real time

**Non-Goals:**
- Persisting LED history or replaying past frames
- Adjusting cell size per-strip or allowing user zoom
- Rendering LED positions spatially (this is a linear strip display, not a 2D canvas mapping)

## Decisions

### 1. Data capture: refactor `buildStripFrames` to also return colors

**Decision**: Change `buildStripFrames` to return `Map<Int, Pair<ByteArray, Array<Color>>>` (frame bytes + color array per strip). The existing caller uses the bytes; a new branch passes the color arrays to the broadcaster.

**Alternative considered**: A second mapper pass just for display. Rejected — `mapViewportToLEDs` is not free; running it twice per frame at 60 FPS wastes CPU. Reusing the single pass is the correct approach.

### 2. Throttle rate: share the existing 20 FPS gate

**Decision**: Reuse the `lastBroadcastTime` / `broadcastIntervalMs` check already in `Application.kt`. When the viewport broadcast fires, also broadcast the LED colors.

**Rationale**: A separate throttle would add state; the viewport and LED display are conceptually in sync. 20 FPS is smooth enough for a visual strip display.

### 3. Message format: per-strip JSON with packed RGB hex string

**Decision**: Send one `strip_leds` message per strip per broadcast cycle:

```json
{ "type": "strip_leds", "stripId": 1, "rgb": "ff0000ff00000000ff..." }
```

`rgb` is a lowercase hex string: 6 characters per LED (`RRGGBB`), concatenated with no separator.

**Rationale**: Compared to a JSON array of objects `[{"r":255,"g":0,"b":0},…]`, the hex string is ~3–4× smaller. For 144 LEDs, the array form is ~3 KB per message; the hex string is ~900 bytes. At 20 FPS with 4 strips that is ~72 KB/s vs ~240 KB/s — a meaningful difference. The frontend decodes it with a simple loop.

**Alternative considered**: Binary WebSocket frame (base64 or raw bytes). Rejected — the existing `/ws/strips` channel uses text frames throughout; switching message types on the same connection adds complexity.

**Alternative considered**: Sending all strips in a single message. Rejected — strips may update at different times in future; per-strip messages keep the protocol consistent with `strip_telemetry`.

### 4. Frontend rendering: `<canvas>` element per strip card

**Decision**: Each strip card renders a `<canvas>` whose width is `LED count × cell size` (clamped to the card width) and height is one cell. On each `strip_leds` message, decode the hex string and draw filled rectangles using `fillRect`.

**Rationale**: At 20 FPS with 144 LEDs, updating 144 DOM `<div>` elements would trigger layout thrashing. A canvas `fillRect` loop for 144 cells is a handful of microseconds and causes no layout work.

**Cell size**: 4 px per LED (configurable via a CSS variable or prop). At 144 LEDs this is 576 px; the canvas is capped at the card width and uses `overflow: hidden` so wider strips wrap or truncate gracefully.

### 5. Hex string decode location: inside the WebSocket hook

**Decision**: `useStripsWebSocket` decodes the hex string into a `Uint8ClampedArray` or plain number array and stores it by `stripId` in `stripLeds` state. The component receives a ready-to-draw `number[]` (flat RGB triples).

**Rationale**: Keeps component rendering simple — the canvas draw loop just iterates indices. Decoding in the hook centralizes the protocol detail.

## Risks / Trade-offs

- **Bandwidth at high strip count**: 4 strips × 144 LEDs × 6 chars × 20 FPS = ~69 KB/s of text. Acceptable on a local network; could be an issue over a slow tunnel. Mitigation: the hex encoding halves the JSON-array baseline; binary encoding can be added later if needed.
- **`buildStripFrames` return type change**: Any future caller of that private function needs updating. Mitigation: it's a private top-level function in `Application.kt` with exactly one call site.
- **Canvas not rendered when strip card is off-screen / tab inactive**: The `StripManagerTab` is already unmounted when inactive (returns `null`), so no wasted draws. The WebSocket also closes, so no messages arrive while inactive.

## Open Questions

- Should the LED display be collapsible per strip card (e.g. a toggle) to save vertical space when there are many strips? Not required for initial implementation; can be added as a follow-on.
