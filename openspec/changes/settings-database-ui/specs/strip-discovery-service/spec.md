## MODIFIED Requirements

### Requirement: Background scan interval is configurable
The system SHALL read the background scan interval (in seconds) from the settings database under the key `scanIntervalSeconds`, defaulting to 15 if absent. The `scanIntervalSeconds` field in `config.yaml` is no longer consulted after the initial database seed.

#### Scenario: Custom interval set in database
- **WHEN** the `scanIntervalSeconds` setting in the database is 60
- **THEN** the background scanner waits 60 seconds between scan iterations

#### Scenario: Default interval used when setting absent
- **WHEN** no `scanIntervalSeconds` row exists in the settings database
- **THEN** the background scanner uses a 15-second interval

## ADDED Requirements

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
