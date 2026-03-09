## ADDED Requirements

### Requirement: Background scan loop runs continuously
The system SHALL run a background coroutine that periodically scans for BLE devices whose names start with "strip" and updates the discovered-device registry for the duration of the server process.

#### Scenario: New device found during background scan
- **WHEN** a BLE scan iteration completes and a device is found whose address is not already in the registry
- **THEN** the device is added to the registry and a `discovery_event` is emitted with the device name and address

#### Scenario: Known device found again
- **WHEN** a BLE scan iteration finds a device whose address is already in the registry
- **THEN** no duplicate entry is created and no redundant `discovery_event` is emitted

#### Scenario: No devices found during scan
- **WHEN** a BLE scan iteration completes and no matching devices are found
- **THEN** a `discovery_event` is emitted indicating the scan completed with zero results

### Requirement: Background scan interval is configurable
The system SHALL read the background scan interval (in seconds) from the settings database under the key `scanIntervalSeconds`, defaulting to 15 if absent. The `scanIntervalSeconds` field in `config.yaml` is no longer consulted after the initial database seed.

#### Scenario: Custom interval set in database
- **WHEN** the `scanIntervalSeconds` setting in the database is 60
- **THEN** the background scanner waits 60 seconds between scan iterations

#### Scenario: Default interval used when setting absent
- **WHEN** no `scanIntervalSeconds` row exists in the settings database
- **THEN** the background scanner uses a 15-second interval

### Requirement: Discovery events are published via SharedFlow
The system SHALL expose a `SharedFlow<StripDiscoveryEvent>` on `BluetoothTester` that emits one event per meaningful discovery action (new device found, scan completed, scan error).

#### Scenario: WebSocket broadcaster subscribes to events
- **WHEN** the `/ws/strips` WebSocket server starts
- **THEN** it collects from the SharedFlow and forwards each event as a JSON `discovery_event` message to all connected clients

#### Scenario: No subscribers does not block emission
- **WHEN** the SharedFlow emits an event and no WebSocket clients are connected
- **THEN** the event is dropped without blocking the background scan coroutine

### Requirement: Background scanner uses a short scan timeout
The system SHALL use a scan timeout of 5000 ms for background scan iterations (distinct from the 10000 ms timeout used during startup `scanAndConnect()`).

#### Scenario: Background scan timeout expires
- **WHEN** a background scan iteration runs and 5000 ms elapse without finding all devices
- **THEN** the scan returns whatever devices were found and the coroutine continues to the next wait interval

### Requirement: Strip registry seeds from database on startup
The system SHALL load the list of configured strips from the settings database (`strips` table) at startup, making them available to the BLE scanner and frame renderer immediately without waiting for a BLE scan.

#### Scenario: Strips present in database at startup
- **WHEN** the application starts and the `strips` table has rows
- **THEN** the strip registry is pre-populated with those strips and `getStripInfos()` returns them (with `connected: false`) before any BLE scan completes

#### Scenario: New strip added via Settings UI
- **WHEN** a user creates a new strip via `POST /api/settings/strips`
- **THEN** the strip is available in the registry for the next background scan cycle to attempt a BLE connection

#### Scenario: Strip deleted via Settings UI
- **WHEN** a user deletes a strip via `DELETE /api/settings/strips/{id}`
- **THEN** the strip is removed from the registry; if a BLE client is connected to it, it is disconnected first

### Requirement: Strip list snapshot is pushed on client connect
The system SHALL send a `strips_update` message containing the full current strip list to any WebSocket client immediately upon connection to `/ws/strips`.

#### Scenario: Client connects after strips already discovered
- **WHEN** a browser client opens `/ws/strips` and strips have already been discovered
- **THEN** the client receives a `strips_update` message with all currently known strips before any other message

#### Scenario: Client connects before any strips discovered
- **WHEN** a browser client opens `/ws/strips` and no strips have been discovered yet
- **THEN** the client receives a `strips_update` message with an empty strips array
