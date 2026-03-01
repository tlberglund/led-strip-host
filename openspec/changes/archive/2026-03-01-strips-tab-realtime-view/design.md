## Context

The app already has `BluetoothTester`, which can scan for BLE strip devices and connect/disconnect them. Currently `scanAndConnect()` is called once at startup; after that the device registry is static. The REST endpoint `GET /api/strips` is polled every 3 seconds by the frontend. There is no mechanism for the backend to push discovery events to the browser, and there is no continuous background scan loop.

The existing WebSocket endpoint (`/viewport`) is used exclusively for binary viewport frames. Adding strip events to that channel would mix protocols and complicate the client.

## Goals / Non-Goals

**Goals:**
- Background coroutine that rescans for strips on a configurable interval and updates the registry when new devices appear or known devices reconnect
- New WebSocket endpoint (`/ws/strips`) that pushes strip-list snapshots and discovery activity log entries to all connected browser clients
- Strips tab upgraded to show live data via the new WebSocket, with a scrolling activity feed below the strip list
- Separate Disconnect / Reconnect buttons per strip (distinct, clearly labeled)
-  Automatic reconnect for strips that disconnected

**Non-Goals:**
- Removing or changing existing REST endpoints (they remain for external consumers)
- Scanning for non-"strip" prefixed BLE devices
- Persistent history of discovery events across server restarts

## Decisions

### 1 — Dedicated WebSocket endpoint for strip events

**Decision**: Add `/ws/strips` as a separate WebSocket endpoint rather than multiplexing onto `/viewport`.

**Rationale**: The viewport channel sends high-frequency binary frames; mixing JSON text frames into it would complicate both the server broadcaster and the client parser. A dedicated channel keeps protocols clean and lets the Strips tab connect independently of viewport state.

**Alternatives considered**: Server-Sent Events (SSE) — simpler for push-only, but the existing codebase uses Ktor WebSockets and SSE would require an additional plugin. Long-polling — too much overhead and latency.

### 2 — Coroutine-based background scanner in `BluetoothTester`

**Decision**: Add a `startBackgroundScanning(scope, intervalMs)` method to `BluetoothTester` that launches a coroutine loop. Each iteration calls the platform scanner, merges newly discovered devices into the existing registry, and emits events via a `SharedFlow`.

**Rationale**: Coroutines are already used in this codebase (Ktor, existing suspend functions). A `SharedFlow` decouples the scanner from its consumers; the WebSocket broadcaster simply collects the flow.

**Alternatives considered**: A dedicated `Thread` — more overhead, harder to cancel cleanly. A JVM `ScheduledExecutorService` — no coroutine integration, harder to unit-test.

### 3 — JSON message envelope on `/ws/strips`

**Decision**: Messages over `/ws/strips` use a JSON envelope:

```json
{ "type": "strips_update", "strips": [...] }
{ "type": "discovery_event", "message": "Found strip02 (AA:BB:CC:DD:EE:FF)" }
```

Two message types: `strips_update` (full snapshot of current strip list) and `discovery_event` (free-text activity log entry). The client holds the latest snapshot in state and appends discovery events to a bounded ring buffer (max 50 entries).

**Rationale**: A full snapshot on every change is simpler than delta updates and safe to deliver on reconnect. Free-text discovery events avoid schema versioning complexity for a logging concern.

### 4 — Frontend hook replaces polling

**Decision**: Replace `useStrips.ts` interval polling with a new `useStripsWebSocket.ts` hook that opens `/ws/strips`, parses the two message types, and exposes `{ strips, activityLog }`.

**Rationale**: Push updates are lower-latency and eliminate the 3-second polling delay. The hook reconnects with exponential backoff on connection loss, matching the pattern in `useWebSocket.ts`.

## Risks / Trade-offs

- **BLE scan blocking**: `BleScanner.discover()` blocks for up to `timeout` ms. Running it in a coroutine on `Dispatchers.IO` prevents it from stalling the main thread, but long timeouts (10 s) mean the activity log update is delayed. → Mitigation: Use a short scan timeout (5 s) for background rescans; the initial `scanAndConnect()` at startup retains its 10 s timeout.
- **Duplicate connect on discovery**: If the background scanner finds a device that was already discovered, it must not attempt to reconnect. → Mitigation: Merge by address; only emit events for genuinely new devices.
- **WebSocket client disconnect during scan**: Clients that disconnect mid-scan will miss events. → Acceptable: on reconnect the client immediately receives a `strips_update` snapshot sent to all new joiners.
- **macOS BLE scan performance**: CoreBluetooth scanning on macOS is already used in the existing code path. Repeated short scans should not stress the adapter, but need smoke-testing on the target hardware.

## Migration Plan

1. Deploy updated backend (new coroutine, new WebSocket endpoint) — REST endpoints unchanged, frontend polling still works
2. Deploy updated frontend (new hook, new tab UI) — switches to WebSocket; falls back gracefully if WebSocket fails (shows last known strip list)
3. No database migrations or config file changes required
4. Rollback: revert frontend to polling hook; background scanner can be disabled by not calling `startBackgroundScanning()`

## Open Questions

- What interval should background rescans use? Proposed default: 30 s (configurable via `config.yaml`). Needs confirmation from operator experience.
- Should the activity log persist in `localStorage` between page reloads, or always start fresh? Proposed: always fresh (simpler, avoids stale data).
