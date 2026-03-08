## Why

The Strips tab currently shows only connection status and static configuration for each strip, giving operators no insight into the health or behavior of running strips. Adding real-time telemetry (temperature, current draw, uptime, and frame count) lets operators detect overheating, power issues, and stalled strips without inspecting hardware directly.

## What Changes

- Add a backend telemetry polling loop that sends a 2-byte telemetry request (command byte `0x03`) to each connected strip at a configurable interval (default 5 s).
- Parse the binary `BTReply` struct returned by each strip: `status` (uint16), `temperature` (float), `current` (float), `uptime_ms` (uint64), `frames` (uint64).
- Store the last 150 telemetry readings per strip in memory.
- Push telemetry updates to the frontend via the existing `/ws/strips` WebSocket channel as a new `strip_telemetry` message type.
- Extend each strip's display card in the Strips tab to show the most recent temperature, current, uptime, and frame count values.
- Render a sparkline chart of temperature and current for each strip using the last 150 readings.
- Expose a `telemetryIntervalSeconds` configuration key (default `5`) in application config.

## Capabilities

### New Capabilities

- `strip-telemetry`: Periodic telemetry collection from BLE-connected strip controllers, including backend polling, binary protocol parsing, history storage, WebSocket broadcast, and frontend display with sparklines.

### Modified Capabilities

- `strips-realtime-tab`: The strip display card gains telemetry fields (temperature, current, uptime, frames) and sparkline charts; the WebSocket message set gains a `strip_telemetry` message type.

## Impact

- **Backend**: New telemetry scheduler in the strip management service; binary protocol handler for command `0x03` and `BTReply` struct; in-memory ring buffer (150 readings) per strip; new WebSocket message emission.
- **Frontend**: Strip card component updated with telemetry section and sparkline; WebSocket message handler extended for `strip_telemetry`.
- **Configuration**: New `telemetryIntervalSeconds` config key.
- **Dependencies**: A lightweight sparkline/chart library may be needed on the frontend (e.g., a small SVG-based utility or an existing chart dependency already in the project).
