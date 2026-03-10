## ADDED Requirements

### Requirement: GET /api/active-pattern returns the currently running pattern
The system SHALL expose `GET /api/active-pattern` returning a JSON object with the current pattern name and parameters so the frontend can synchronize its editor state on page load.

#### Scenario: Active pattern is set
- **WHEN** a client sends `GET /api/active-pattern` and a pattern is running
- **THEN** the response is `200 OK` with `{"patternName": "Solid Color", "params": {"color": "#ff00001f"}}`

#### Scenario: No active pattern yet
- **WHEN** a client sends `GET /api/active-pattern` and no pattern has been set since startup
- **THEN** the response is `200 OK` with `{"patternName": "", "params": {}}`

### Requirement: GET /api/saved-patterns returns all presets
The system SHALL expose `GET /api/saved-patterns` returning a JSON array of all saved pattern presets ordered by `preset_name` ascending.

#### Scenario: Presets exist
- **WHEN** a client sends `GET /api/saved-patterns`
- **THEN** the response is `200 OK` with a JSON array where each element has `id` (integer), `presetName` (string), `patternName` (string), `params` (object), and `updatedAt` (epoch milliseconds)

#### Scenario: No presets saved yet
- **WHEN** the `saved_patterns` table is empty
- **THEN** the response is `200 OK` with an empty JSON array `[]`

### Requirement: POST /api/saved-patterns creates a new preset
The system SHALL expose `POST /api/saved-patterns` accepting a JSON body with `presetName`, `patternName`, and `params` fields, and inserting a new row.

#### Scenario: Valid creation
- **WHEN** a client sends `POST /api/saved-patterns` with `{"presetName":"My Red","patternName":"Solid Color","params":{"color":"#ff00001f"}}`
- **THEN** the preset is inserted, and the response is `201 Created` with the new row including its assigned `id` and `updatedAt`

#### Scenario: Duplicate preset name
- **WHEN** a client sends `POST /api/saved-patterns` with a `presetName` that already exists in the table
- **THEN** the response is `409 Conflict` and no row is inserted

#### Scenario: Missing required field
- **WHEN** a client sends `POST /api/saved-patterns` without a `patternName` field
- **THEN** the response is `400 Bad Request` and no row is inserted

### Requirement: PUT /api/saved-patterns/{id} updates a preset
The system SHALL expose `PUT /api/saved-patterns/{id}` accepting a JSON body with any subset of `presetName`, `patternName`, and `params`, updating the matching row and setting `updated_at` to the current epoch milliseconds.

#### Scenario: Valid update of params
- **WHEN** a client sends `PUT /api/saved-patterns/3` with `{"params":{"color":"#0000ff0f"}}`
- **THEN** only the `params` column is updated, `updated_at` is refreshed, and the response is `200 OK` with the full updated row

#### Scenario: Rename preset
- **WHEN** a client sends `PUT /api/saved-patterns/3` with `{"presetName":"Bright Blue"}`
- **THEN** the `preset_name` is updated and the response is `200 OK`

#### Scenario: Rename conflicts with existing name
- **WHEN** a client sends `PUT /api/saved-patterns/3` with a `presetName` already used by a different row
- **THEN** the response is `409 Conflict` and no changes are made

#### Scenario: Preset not found
- **WHEN** a client sends `PUT /api/saved-patterns/999` and no row with that id exists
- **THEN** the response is `404 Not Found`

### Requirement: DELETE /api/saved-patterns/{id} deletes a preset
The system SHALL expose `DELETE /api/saved-patterns/{id}` which removes the preset from the database.

#### Scenario: Successful deletion
- **WHEN** a client sends `DELETE /api/saved-patterns/3` and the preset exists
- **THEN** the preset is removed and the response is `204 No Content`

#### Scenario: Preset not found
- **WHEN** a client sends `DELETE /api/saved-patterns/999` and no row with that id exists
- **THEN** the response is `404 Not Found`
