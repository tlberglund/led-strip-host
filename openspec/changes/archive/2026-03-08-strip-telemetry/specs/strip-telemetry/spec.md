## ADDED Requirements

### Requirement: Backend polls each connected strip for telemetry at a configurable interval
The backend SHALL send a 2-byte telemetry request (first byte `0x03`, second byte `0x00`) to each connected strip's GATT characteristic at the interval configured by `telemetryIntervalSeconds` (default 5 s). Only strips whose `isConnected` is true SHALL be polled.

#### Scenario: Telemetry request sent to connected strip
- **WHEN** the telemetry polling interval elapses
- **THEN** a 2-byte write (`[0x03, 0x00]`) is issued to the GATT characteristic of every currently-connected strip

#### Scenario: Disconnected strip skipped
- **WHEN** the telemetry polling interval elapses and a strip is not connected
- **THEN** no telemetry request is sent to that strip and no error is logged

#### Scenario: Polling interval is configurable
- **WHEN** the `telemetryIntervalSeconds` setting is changed in the database
- **THEN** the new interval takes effect on the next application start (restart required)

### Requirement: Backend parses the BTReply binary payload
The backend SHALL parse each BLE notification received in response to a telemetry request as a `BTReply` binary struct in little-endian byte order. The struct SHALL be interpreted according to standard C alignment (32-byte form: status at offset 0, temperature at offset 4, current at offset 8, uptime_ms at offset 16, frames at offset 24) or packed form (26-byte form: status at offset 0, temperature at offset 2, current at offset 6, uptime_ms at offset 10, frames at offset 18), detected by payload size.

#### Scenario: Valid 32-byte reply received
- **WHEN** a BLE notification arrives with exactly 32 bytes after a telemetry request
- **THEN** the backend parses it as the padded struct and extracts all five fields correctly

#### Scenario: Valid 26-byte reply received
- **WHEN** a BLE notification arrives with exactly 26 bytes after a telemetry request
- **THEN** the backend parses it as the packed struct and extracts all five fields correctly

#### Scenario: Unexpected payload size received
- **WHEN** a BLE notification arrives with a size other than 26 or 32 bytes after a telemetry request
- **THEN** the backend logs a warning with the strip ID and payload size and discards the reading

### Requirement: Backend maintains a rolling telemetry history of 150 readings per strip
The backend SHALL store the last 150 `TelemetryReading` values for each strip in an in-memory ring buffer. When the buffer is full, the oldest reading SHALL be evicted before adding the newest.

#### Scenario: Buffer not yet full
- **WHEN** fewer than 150 readings have been received for a strip
- **THEN** all readings are retained in insertion order

#### Scenario: Buffer full and new reading arrives
- **WHEN** a 151st reading arrives for a strip
- **THEN** the oldest reading is evicted and the new reading is appended, keeping the buffer at exactly 150 entries

#### Scenario: Application restarts
- **WHEN** the application is restarted
- **THEN** telemetry history for all strips starts empty (history is not persisted)

### Requirement: Backend broadcasts telemetry to WebSocket clients after each successful reading
After parsing a valid `BTReply`, the backend SHALL broadcast a `strip_telemetry` WebSocket message to all connected `/ws/strips` clients. The message SHALL include the strip's ID, the latest reading's five fields, and the full temperature and current history arrays (oldest-first, up to 150 entries each).

#### Scenario: Telemetry reply received and parsed
- **WHEN** a valid telemetry reply is parsed for strip N
- **THEN** a `strip_telemetry` message is broadcast on `/ws/strips` within 500 ms containing `stripId`, `status`, `temperature`, `current`, `uptimeMs`, `frames`, `history.temperature`, and `history.current`

#### Scenario: No WebSocket clients connected
- **WHEN** a valid telemetry reply is parsed and no clients are subscribed to `/ws/strips`
- **THEN** the reading is still stored in the ring buffer and no error is raised

### Requirement: `telemetryIntervalSeconds` is stored and exposed as a configurable setting
The backend SHALL store `telemetryIntervalSeconds` in the `settings` database table with a default value of `5`. It SHALL be returned in the `GET /api/settings` response and be updatable via `PUT /api/settings`.

#### Scenario: Fresh database
- **WHEN** the application starts against an empty database
- **THEN** `telemetryIntervalSeconds` is seeded with the value `5`

#### Scenario: Setting retrieved
- **WHEN** a client calls `GET /api/settings`
- **THEN** the response includes `telemetryIntervalSeconds` as a numeric field
