## 1. Configuration & Settings

- [x] 1.1 Add `telemetryIntervalSeconds` to `SettingsRepository.seedFromConfig` with default value `5`
- [x] 1.2 Add `telemetryIntervalSeconds` to the `GET /api/settings` response in `PreviewServer`
- [x] 1.3 Handle `telemetryIntervalSeconds` in the `PUT /api/settings` request handler

## 2. Backend — Telemetry Protocol

- [x] 2.1 Add `TelemetryReading` data class (`timestamp`, `status`, `temperature`, `current`, `uptimeMs`, `frames`) in the BLE package
- [x] 2.2 Add `parseTelemetryReply(data: ByteArray): TelemetryReading?` function that handles both 32-byte (padded) and 26-byte (packed) struct layouts using `ByteBuffer` in little-endian order; returns `null` and logs a warning for unexpected sizes

## 3. Backend — `BluetoothHost` Changes

- [x] 3.1 Replace the no-op `notificationHandler` stub with a dispatcher that routes received notification bytes to a registered per-strip callback (`Map<Int, (ByteArray) -> Unit>`)
- [x] 3.2 Add `startTelemetryPolling(scope: CoroutineScope, intervalMs: Long, onReading: (stripId: Int, reading: TelemetryReading) -> Unit)` method that launches a coroutine, iterates over connected strips each interval, writes the 2-byte telemetry request `[0x03, 0x00]` to `CHAR_UUID`, and registers a one-shot callback for the reply
- [x] 3.3 Ensure `startTelemetryPolling` skips strips where `isConnected` is false at poll time

## 4. Backend — Telemetry History & Broadcasting

- [x] 4.1 Create a `TelemetryStore` class (or equivalent in-process store) holding a `MutableMap<Int, ArrayDeque<TelemetryReading>>` with a `MAX_HISTORY = 150` cap; expose `record(stripId, reading)` and `getHistory(stripId): List<TelemetryReading>`
- [x] 4.2 Add `StripTelemetryMessage` serializable data class (`type = "strip_telemetry"`, `stripId`, `status`, `temperature`, `current`, `uptimeMs`, `frames`, `history: TelemetryHistory`) and `TelemetryHistory` data class (`temperature: List<Float>`, `current: List<Float>`)
- [x] 4.3 Add `broadcast(message: StripTelemetryMessage)` overload to `StripsWsBroadcaster`
- [x] 4.4 In `Application` (or `PreviewServer`), wire `onReading` callback from `startTelemetryPolling` to: store reading in `TelemetryStore`, then broadcast `StripTelemetryMessage` with full history arrays

## 5. Backend — Startup Wiring

- [x] 5.1 In `Application` (or `PreviewServer.start`), read `telemetryIntervalSeconds` from the settings repository after DB connect
- [x] 5.2 Call `bleManager.startTelemetryPolling(scope, intervalMs, onReading)` after BLE scan completes (in the same scope as `startBackgroundScanning`)

## 6. Frontend — TypeScript Types

- [x] 6.1 Add `TelemetryHistory` interface (`temperature: number[]`, `current: number[]`) to `types.ts`
- [x] 6.2 Add `StripTelemetryMessage` interface (`type: 'strip_telemetry'`, `stripId: number`, `status: number`, `temperature: number`, `current: number`, `uptimeMs: number`, `frames: number`, `history: TelemetryHistory`) to `types.ts`
- [x] 6.3 Update `StripsWsMessage` union type to include `StripTelemetryMessage`
- [x] 6.4 Add `telemetryIntervalSeconds` field to `ScalarSettings` interface

## 7. Frontend — State Management

- [x] 7.1 In `StripManagerTab.tsx`, add a `stripTelemetry` state map (`Record<number, StripTelemetryMessage>`) initialised to `{}`
- [x] 7.2 In the WebSocket message handler, add a case for `strip_telemetry` that updates `stripTelemetry[msg.stripId]` with the new message

## 8. Frontend — Sparkline Component

- [x] 8.1 Create `Sparkline.tsx` functional component accepting `values: number[]` and `color: string` props; renders an inline SVG `<polyline>` scaled to the component's bounding box; renders a flat horizontal line when fewer than 2 values are present; renders a flat midpoint line when all values are identical

## 9. Frontend — Strip Card Telemetry UI

- [x] 9.1 In `StripManagerTab.tsx`, add a telemetry section to each strip card that shows "Awaiting telemetry…" when no data is available
- [x] 9.2 Display latest `temperature` (1 decimal, °C), `current` (2 decimals, A), `uptime` (formatted `Xd Xh Xm Xs` from `uptimeMs`), and `frames` (integer with `toLocaleString()` thousands separator)
- [x] 9.3 Render `<Sparkline values={telemetry.history.temperature} color="orange" />` and `<Sparkline values={telemetry.history.current} color="cyan" />` below the numeric values with labels
- [x] 9.4 Show stale telemetry values (last received) when a strip transitions to disconnected state
