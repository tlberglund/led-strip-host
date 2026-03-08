## Why

The Strips tab currently shows connection status and telemetry metrics but gives no visual feedback about what each strip is actually showing. Adding a real-time per-LED color display lets operators instantly confirm that patterns are rendering correctly on each physical strip without looking at the hardware.

## What Changes

- After each rendered frame, capture the per-LED RGB colors for each strip (already computed during frame building) and broadcast them to the frontend via the existing `/ws/strips` WebSocket channel as a new `strip_leds` message type, throttled to match the existing 20 FPS WebSocket broadcast rate.
- Each strip card in the Strips tab gains a canvas-based LED color strip — a row of small colored squares, one per LED, reflecting the most recently broadcast colors.
- The LED display updates in real time at the WebSocket broadcast rate.

## Capabilities

### New Capabilities

- `strip-led-display`: Real-time per-LED color broadcast from backend and canvas-based rendering in each strip card in the Strips tab.

### Modified Capabilities

(none — the strip card additions are purely additive display elements and do not change existing requirements)

## Impact

- **Backend**: `Application.kt` captures per-strip LED color arrays alongside existing frame building; throttled broadcast of `strip_leds` messages via `PreviewServer`; new `StripLedsMessage` data class in `PreviewServer.kt`; new `broadcast(StripLedsMessage)` overload in `StripsWsBroadcaster`.
- **Frontend**: New `StripLedsMessage` type in `types.ts`; `useStripsWebSocket` hook handles the new message type and maintains a `stripLeds` state map; `StripManagerTab` renders a `<canvas>` LED strip in each strip card.
- **No new dependencies** — canvas rendering requires no additional libraries.
