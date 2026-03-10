## Requirements

### Requirement: Active pattern endpoint
The system SHALL expose a `GET /api/active-pattern` endpoint that returns the currently active pattern (the pattern currently running on the LED strips) as a JSON object, including all pattern parameters.

#### Scenario: Active pattern exists
- **WHEN** a client sends `GET /api/active-pattern`
- **THEN** the server responds with `200 OK` and a JSON body representing the current pattern name and all its parameters

#### Scenario: No active pattern
- **WHEN** a client sends `GET /api/active-pattern` and no pattern is currently active
- **THEN** the server responds with `200 OK` and a JSON body with a null or empty pattern indicator

### Requirement: List saved patterns
The system SHALL expose a `GET /api/saved-patterns` endpoint that returns all saved patterns as a JSON array.

#### Scenario: Patterns exist
- **WHEN** a client sends `GET /api/saved-patterns`
- **THEN** the server responds with `200 OK` and a JSON array of all saved pattern objects, each including `id`, `name`, and pattern parameters

#### Scenario: No patterns saved
- **WHEN** a client sends `GET /api/saved-patterns` and no patterns have been saved
- **THEN** the server responds with `200 OK` and an empty JSON array

### Requirement: Create saved pattern
The system SHALL expose a `POST /api/saved-patterns` endpoint that saves a new named pattern.

#### Scenario: Successful creation
- **WHEN** a client sends `POST /api/saved-patterns` with a JSON body containing a `name` and pattern parameters
- **THEN** the server persists the pattern, responds with `201 Created`, and returns the new pattern object including its generated `id`

#### Scenario: Missing required fields
- **WHEN** a client sends `POST /api/saved-patterns` with a body missing the `name` field
- **THEN** the server responds with `400 Bad Request` and a descriptive error message

### Requirement: Get saved pattern by ID
The system SHALL expose a `GET /api/saved-patterns/{id}` endpoint that returns a single saved pattern by its ID.

#### Scenario: Pattern found
- **WHEN** a client sends `GET /api/saved-patterns/{id}` for an existing pattern
- **THEN** the server responds with `200 OK` and the matching pattern object

#### Scenario: Pattern not found
- **WHEN** a client sends `GET /api/saved-patterns/{id}` for an ID that does not exist
- **THEN** the server responds with `404 Not Found`

### Requirement: Update saved pattern
The system SHALL expose a `PUT /api/saved-patterns/{id}` endpoint that updates the name and/or parameters of an existing saved pattern.

#### Scenario: Successful update
- **WHEN** a client sends `PUT /api/saved-patterns/{id}` with a valid JSON body for an existing pattern
- **THEN** the server updates the pattern, responds with `200 OK`, and returns the updated pattern object

#### Scenario: Pattern not found
- **WHEN** a client sends `PUT /api/saved-patterns/{id}` for an ID that does not exist
- **THEN** the server responds with `404 Not Found`

### Requirement: Delete saved pattern
The system SHALL expose a `DELETE /api/saved-patterns/{id}` endpoint that removes a saved pattern by its ID.

#### Scenario: Successful deletion
- **WHEN** a client sends `DELETE /api/saved-patterns/{id}` for an existing pattern
- **THEN** the server deletes the pattern and responds with `204 No Content`

#### Scenario: Pattern not found
- **WHEN** a client sends `DELETE /api/saved-patterns/{id}` for an ID that does not exist
- **THEN** the server responds with `404 Not Found`
