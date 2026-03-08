## Context

The backend already maintains persistent BLE connections to each strip via `BluetoothHost`, which uses a single shared GATT characteristic (`CHAR_UUID`) for both outbound frames and inbound notifications. The notification handler (`notificationHandler`) is currently a no-op stub. The frontend receives strip state via `StripsWsBroadcaster` over `/ws/strips`. Settings are stored in a PostgreSQL `settings` table (key/value) and read at startup or through the Settings API.

The telemetry protocol is one-shot request/response over BLE GATT notifications:
- **Request**: 2-byte write to `CHAR_UUID`, first byte = `0x03`
- **Reply**: BLE notification on `CHAR_UUID` carrying a `BTReply` binary struct

## Goals / Non-Goals

**Goals:**
- Poll each connected strip for telemetry at a configurable interval (default 5 s)
- Parse the `BTReply` binary payload and keep a 150-reading ring buffer per strip in memory
- Push telemetry to the frontend via the existing `/ws/strips` WebSocket as a new `strip_telemetry` message
- Render current values and temperature/current sparklines in each strip card on the Strips tab
- Store `telemetryIntervalSeconds` in the DB settings table alongside other scalar settings

**Non-Goals:**
- Persisting telemetry history to the database
- Alerting or thresholds based on telemetry values
- Exposing a REST endpoint for telemetry history (WebSocket push is sufficient)

## Decisions

### 1. Telemetry polling inside `BluetoothHost` vs. a separate service

**Decision**: Add a `startTelemetryPolling(scope, intervalMs, onReading)` method to `BluetoothHost`.

**Rationale**: `BluetoothHost` already owns the `clients` map and `writeGattCharacteristic`. Keeping telemetry polling inside it avoids exposing the `clients` map externally. A callback lambda (`onReading`) lets the caller (Application/PreviewServer layer) handle storage and broadcasting without coupling `BluetoothHost` to the WS layer.

**Alternative considered**: A separate `TelemetryService` that takes a `BluetoothHost` reference. Rejected because it would require either exposing the client map or adding a `sendCommand(stripId, data)` delegation method — effectively the same surface area, with more indirection.

### 2. Notification routing

**Decision**: Replace the no-op `notificationHandler` with a dispatch mechanism: store the most-recently-registered callback in a `MutableMap<String, (ByteArray) -> Unit>` keyed by sender address, set when a telemetry poll is in flight.

**Rationale**: The existing `startNotify` callback fires on every notification from the strip. Telemetry replies arrive on the same characteristic as (currently unused) other notifications. A simple sender-keyed callback map allows telemetry replies to be routed without breaking any future uses of the notification channel.

### 3. Ring buffer storage

**Decision**: Use an `ArrayDeque<TelemetryReading>` (max 150 entries) per strip, held in a `MutableMap<Int, ArrayDeque<TelemetryReading>>` inside the component responsible for telemetry collection (the Application/PreviewServer call site). When full, `removeFirst()` before `addLast()`.

**Rationale**: `ArrayDeque` gives O(1) head removal and tail append. 150 readings × ~6 fields × 8 bytes ≈ ~7 KB per strip — trivial in memory.

### 4. Binary parsing of `BTReply`

**Decision**: Parse using `java.nio.ByteBuffer` in little-endian order. Expected layout (default C struct alignment, assuming no `__attribute__((packed))`):

```
offset 0  — uint16 status     (2 bytes)
offset 2  — padding           (2 bytes, compiler-inserted for float alignment)
offset 4  — float temperature (4 bytes)
offset 8  — float current     (4 bytes)
offset 12 — padding           (4 bytes, for uint64 alignment)
offset 16 — uint64 uptime_ms  (8 bytes)
offset 24 — uint64 frames     (8 bytes)
total: 32 bytes
```

**Rationale**: Standard ARM Cortex-M default packing. If the firmware uses `__attribute__((packed))` the offsets will differ (status at 0, temp at 2, current at 6, uptime at 10, frames at 18 — 26 bytes total). Verify against actual firmware before shipping.

**Alternative**: Firmware could send a packed struct or a JSON text notification. Rejected — changing the firmware protocol is out of scope.

### 5. WebSocket message shape

**Decision**: Add a new `strip_telemetry` message type to the existing `/ws/strips` channel:

```json
{
  "type": "strip_telemetry",
  "stripId": 1,
  "status": 0,
  "temperature": 42.5,
  "current": 1.23,
  "uptimeMs": 123456789,
  "frames": 7200000,
  "history": {
    "temperature": [41.0, 41.5, 42.0, 42.5],
    "current": [1.20, 1.21, 1.22, 1.23]
  }
}
```

`history` arrays contain all buffered readings (up to 150), oldest first. Sending the full history on each update keeps the frontend stateless about history accumulation and avoids the need for a re-sync protocol on reconnect.

**Alternative**: Send only the latest reading and let the frontend accumulate history. Rejected because it complicates reconnect handling — the frontend would lose history on WebSocket reconnect.

### 6. Sparkline rendering

**Decision**: Render sparklines as inline SVG `<polyline>` elements computed directly in the React component from the `history` arrays. No charting library dependency.

**Rationale**: The history arrays are already normalised float sequences. A simple min/max scale to SVG viewport coordinates is ~10 lines of code and adds zero bundle weight.

### 7. `telemetryIntervalSeconds` configuration

**Decision**: Store in the DB `settings` table under key `telemetryIntervalSeconds`, default `5`. Expose via the existing `GET /api/settings` and `PUT /api/settings` endpoints so the Settings tab can manage it.

**Rationale**: Matches the pattern of `scanIntervalSeconds` already in the settings table. Seeded to `5` on first DB population.

## Risks / Trade-offs

- **Struct alignment mismatch** → If the firmware uses packed structs, the parser will silently produce garbage values. Mitigation: add a sanity-check on the `status` field (should be 0 for OK) and log a warning when parsing a payload that is neither 26 nor 32 bytes.
- **BLE write/notification race** → If a strip sends a frame notification and a telemetry reply arrives at the same time, the dispatcher may misroute. Mitigation: telemetry replies include a command discriminator in `status`; log unexpected payload sizes.
- **High-frequency WebSocket traffic** → With many strips, a telemetry broadcast per strip per interval could overwhelm slow clients. Mitigation: the default 5 s interval is conservative; the broadcast includes only one strip's data per message.
- **In-memory history loss on restart** → History is not persisted. Operators see empty sparklines after restart. Accepted trade-off; full history persistence is explicitly out of scope.

## Migration Plan

1. Deploy backend with telemetry polling disabled until DB migration adds `telemetryIntervalSeconds` setting (seeded automatically by the existing `seedFromConfig` logic on empty DB, or a one-time `setSetting` call on upgrade).
2. Deploy frontend — new fields in strip cards are hidden when no `strip_telemetry` messages have been received (graceful degradation).
3. No rollback risk: `strip_telemetry` messages are additive; old frontend versions ignore unknown message types.

## Open Questions

1. **Struct packing**: Does the firmware compile `BTReply` with default alignment or `__attribute__((packed))`? Determines whether total size is 26 or 32 bytes.
2. **Notification channel sharing**: Will the BLE characteristic carry non-telemetry notifications in the future? If so, a more robust message-type discriminator in the protocol would be warranted.
3. **Telemetry interval UI**: Should `telemetryIntervalSeconds` be editable in the Settings tab UI, or only via direct API/DB? (Proposal assumes Settings tab, but UI placement is unspecified.)
