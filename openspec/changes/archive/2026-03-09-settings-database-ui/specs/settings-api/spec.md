## ADDED Requirements

### Requirement: GET /api/settings returns all scalar settings
The system SHALL expose `GET /api/settings` returning a JSON object with all scalar settings: `viewportWidth`, `viewportHeight`, `targetFPS`, and `scanIntervalSeconds`.

#### Scenario: Settings exist in database
- **WHEN** a client sends `GET /api/settings`
- **THEN** the response is `200 OK` with a JSON object containing the current values of all scalar settings

#### Scenario: No settings in database yet
- **WHEN** a client sends `GET /api/settings` and the database has no scalar settings rows
- **THEN** the response is `200 OK` with hardcoded default values (`viewportWidth: 240`, `viewportHeight: 135`, `targetFPS: 25`, `scanIntervalSeconds: 15`)

### Requirement: PUT /api/settings updates scalar settings
The system SHALL expose `PUT /api/settings` accepting a JSON body with any subset of scalar settings keys and persisting the provided values.

#### Scenario: Partial update
- **WHEN** a client sends `PUT /api/settings` with `{"targetFPS": 30}`
- **THEN** only `targetFPS` is updated in the database; other settings are unchanged, and the response is `200 OK`

#### Scenario: Invalid value
- **WHEN** a client sends `PUT /api/settings` with `{"targetFPS": -5}`
- **THEN** the response is `400 Bad Request` with an error message and no database changes are made

### Requirement: GET /api/settings/strips returns all strips
The system SHALL expose `GET /api/settings/strips` returning a JSON array of all strip rows.

#### Scenario: Strips exist
- **WHEN** a client sends `GET /api/settings/strips`
- **THEN** the response is `200 OK` with a JSON array where each element has `id`, `btName`, `length`, `startX`, `startY`, `endX`, `endY`, and `reverse` fields

#### Scenario: No strips configured
- **WHEN** the strips table is empty
- **THEN** the response is `200 OK` with an empty JSON array

### Requirement: POST /api/settings/strips creates a strip
The system SHALL expose `POST /api/settings/strips` accepting a JSON body and creating a new strip row.

#### Scenario: Valid strip creation
- **WHEN** a client sends `POST /api/settings/strips` with `{"btName":"strip02","length":40,"startX":0,"startY":0,"endX":39,"endY":0,"reverse":false}`
- **THEN** the strip is inserted, and the response is `201 Created` with the new row including its assigned `id`

#### Scenario: Missing required field
- **WHEN** a client sends `POST /api/settings/strips` without a `btName` field
- **THEN** the response is `400 Bad Request` and no row is inserted

### Requirement: PUT /api/settings/strips/{id} updates a strip
The system SHALL expose `PUT /api/settings/strips/{id}` accepting a JSON body and updating the strip with that `id`.

#### Scenario: Valid update
- **WHEN** a client sends `PUT /api/settings/strips/3` with updated fields
- **THEN** the strip row is updated and the response is `200 OK` with the updated row

#### Scenario: Strip not found
- **WHEN** a client sends `PUT /api/settings/strips/99` and no strip with id 99 exists
- **THEN** the response is `404 Not Found`

### Requirement: DELETE /api/settings/strips/{id} deletes a strip
The system SHALL expose `DELETE /api/settings/strips/{id}` which removes the strip from the database.

#### Scenario: Successful deletion
- **WHEN** a client sends `DELETE /api/settings/strips/3` and the strip exists
- **THEN** the strip is removed and the response is `204 No Content`

#### Scenario: Strip not found
- **WHEN** a client sends `DELETE /api/settings/strips/99` and no strip with id 99 exists
- **THEN** the response is `404 Not Found`

### Requirement: POST /api/settings/background-image uploads an image
The system SHALL expose `POST /api/settings/background-image` accepting a `multipart/form-data` body with an image file and storing it in the database and cache.

#### Scenario: Valid image upload
- **WHEN** a client sends a multipart POST with a PNG or JPEG file
- **THEN** the image is stored in the database, the cache file is updated, and the response is `200 OK`

#### Scenario: File too large
- **WHEN** a client sends an image larger than 10 MB
- **THEN** the response is `413 Payload Too Large` and the database is unchanged

### Requirement: DELETE /api/settings/background-image removes the image
The system SHALL expose `DELETE /api/settings/background-image` which removes the image from the database and deletes the cache file.

#### Scenario: Image exists
- **WHEN** a client sends `DELETE /api/settings/background-image` and an image is stored
- **THEN** the image row is deleted, the cache file is removed, and the response is `204 No Content`

#### Scenario: No image stored
- **WHEN** a client sends `DELETE /api/settings/background-image` and no image is stored
- **THEN** the response is `404 Not Found`
