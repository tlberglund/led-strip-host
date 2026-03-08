## ADDED Requirements

### Requirement: Settings database connects on startup
The system SHALL connect to a PostgreSQL database using the `databaseUrl`, `databaseUser`, and `databasePassword` fields from `config.yaml` (defaulting to `jdbc:postgresql://localhost:5432/ledstrip`, `ledstrip`, and `ledstrip` respectively) and create all required tables if they do not exist when the application starts.

#### Scenario: Successful connection and schema creation
- **WHEN** the application starts and the PostgreSQL instance is reachable
- **THEN** the `settings`, `strips`, and `background_image` tables are created if absent, and the application proceeds

#### Scenario: Database unreachable at startup
- **WHEN** the application starts and the PostgreSQL instance cannot be reached
- **THEN** the application logs a clear error including the configured URL and exits with a non-zero status code

#### Scenario: Tables already exist
- **WHEN** the application starts and all tables already exist
- **THEN** no DDL is executed and existing data is preserved

### Requirement: Settings key-value store persists scalar configuration
The system SHALL store scalar settings (viewport width, viewport height, target FPS, scan interval seconds) as rows in a `settings(key VARCHAR PRIMARY KEY, value TEXT)` table and expose typed read/write operations via `SettingsRepository`.

#### Scenario: Reading a setting that exists
- **WHEN** `SettingsRepository.getSetting(key)` is called for a key that has a stored value
- **THEN** the stored string value is returned

#### Scenario: Reading a setting that does not exist
- **WHEN** `SettingsRepository.getSetting(key)` is called for a key with no stored value
- **THEN** `null` is returned

#### Scenario: Writing a setting
- **WHEN** `SettingsRepository.setSetting(key, value)` is called
- **THEN** the value is upserted (INSERT ... ON CONFLICT DO UPDATE) in the `settings` table and is immediately readable

### Requirement: Strip table persists strip layouts
The system SHALL store strip layouts in a `strips` table with columns `id SERIAL PRIMARY KEY`, `bt_name VARCHAR NOT NULL`, `length INT`, `start_x INT`, `start_y INT`, `end_x INT`, `end_y INT`, `reverse BOOLEAN DEFAULT false`.

#### Scenario: Creating a strip
- **WHEN** `SettingsRepository.createStrip(btName, length, startX, startY, endX, endY, reverse)` is called
- **THEN** a new row is inserted and the generated `id` is returned

#### Scenario: Deleting a strip
- **WHEN** `SettingsRepository.deleteStrip(id)` is called
- **THEN** the row with that `id` is removed from the `strips` table

#### Scenario: Listing all strips
- **WHEN** `SettingsRepository.getAllStrips()` is called
- **THEN** all rows from the `strips` table are returned ordered by `id`

#### Scenario: Updating a strip
- **WHEN** `SettingsRepository.updateStrip(id, ...)` is called with new field values
- **THEN** the corresponding row is updated and the changes are immediately readable

### Requirement: Background image stored as bytea
The system SHALL store the background image in a `background_image(id INT PRIMARY KEY DEFAULT 1, data BYTEA, mime_type VARCHAR, updated_at BIGINT)` table, using a single fixed-id row that is upserted on each upload.

#### Scenario: Uploading an image
- **WHEN** `SettingsRepository.setBackgroundImage(bytes, mimeType)` is called
- **THEN** the bytes and mime type are upserted (replacing any previous image) and `updated_at` is set to the current epoch milliseconds

#### Scenario: No image stored
- **WHEN** `SettingsRepository.getBackgroundImage()` is called and no row exists
- **THEN** `null` is returned

#### Scenario: Deleting the image
- **WHEN** `SettingsRepository.deleteBackgroundImage()` is called
- **THEN** the row is deleted and `getBackgroundImage()` subsequently returns `null`

### Requirement: Filesystem cache for background image
The system SHALL write the background image bytes to a cache file at a configurable path (default: `./bg-image-cache.<ext>`, where `<ext>` is derived from the MIME type) after each upload, and SHALL regenerate the cache file on startup if the database has an image but the cache file is absent or older than the database row's `updated_at` timestamp.

#### Scenario: Cache file written after upload
- **WHEN** a new background image is stored via `SettingsRepository.setBackgroundImage`
- **THEN** the image bytes are written atomically to the cache file path, replacing any existing cache file

#### Scenario: Cache regenerated on startup
- **WHEN** the application starts and the database has a background image but the cache file is absent or stale
- **THEN** the image bytes are read from the database and written to the cache file before the web server starts

### Requirement: Seed from config.yaml on first run
The system SHALL, on first startup with empty `settings` and `strips` tables, read any strip layouts, viewport dimensions, FPS, scan interval, and background image path from `config.yaml` and insert them as the initial database rows.

#### Scenario: First run with existing config.yaml
- **WHEN** both `settings` and `strips` tables are empty and `config.yaml` contains strip and viewport configuration
- **THEN** those values are inserted into the database and a warning is logged that `config.yaml` fields (except `webServer.port` and database connection fields) will be ignored on future starts

#### Scenario: Subsequent run after seeding
- **WHEN** the `strips` table already has rows
- **THEN** no seeding occurs and `config.yaml` non-connection fields are silently ignored

### Requirement: Docker Compose file provided for local development
The project SHALL include a `docker-compose.yml` at the repository root that starts a `postgres:16-alpine` container with a named volume, credentials matching the `config.yaml` defaults, and a health check.

#### Scenario: Developer runs docker compose up
- **WHEN** a developer runs `docker compose up -d` from the project root
- **THEN** a PostgreSQL 16 instance starts on port 5432, accessible with the default credentials, with data persisted in a named Docker volume
