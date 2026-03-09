## ADDED Requirements

### Requirement: Active preset name is persisted in the settings table
The system SHALL store the `preset_name` of the last explicitly loaded saved preset in the `settings` table under the key `activePresetName`. On startup, the system SHALL read this key, look up the named preset in `saved_patterns`, and restore the pattern engine from that preset's stored `pattern_name` and `params` before serving any requests. The params are NOT duplicated into `settings`; the `saved_patterns` table is the single source of truth for params.

#### Scenario: User loads a saved preset
- **WHEN** a saved preset is loaded via the API
- **THEN** the `settings` row `activePresetName` is upserted with that preset's `preset_name`

#### Scenario: Active preset restored on startup
- **WHEN** the application starts and `activePresetName` exists in `settings` and a matching row exists in `saved_patterns`
- **THEN** the pattern engine initializes with that preset's `pattern_name` and `params` before any render loop begins

#### Scenario: Active preset name in settings but preset was deleted
- **WHEN** the application starts and `activePresetName` exists in `settings` but no matching row exists in `saved_patterns`
- **THEN** the pattern engine falls back to the first registered pattern with default parameters

#### Scenario: No active preset in settings on startup
- **WHEN** the application starts and `activePresetName` is absent from the `settings` table
- **THEN** the pattern engine initializes with a default pattern (first registered pattern with default parameters)

### Requirement: saved_patterns table persists pattern presets
The system SHALL create a `saved_patterns` table on startup with columns `id SERIAL PRIMARY KEY`, `preset_name VARCHAR(255) NOT NULL UNIQUE`, `pattern_name VARCHAR(255) NOT NULL`, `params JSONB NOT NULL`, and `updated_at BIGINT NOT NULL`. The table SHALL be created via `CREATE TABLE IF NOT EXISTS` so existing data is preserved across restarts.

#### Scenario: Table created on first startup
- **WHEN** the application starts and the `saved_patterns` table does not exist
- **THEN** the table is created with the correct schema before any API request is served

#### Scenario: Table already exists
- **WHEN** the application starts and the `saved_patterns` table already exists with data
- **THEN** no DDL is executed and existing preset rows are preserved

### Requirement: SavedPatternsRepository provides CRUD for presets
The system SHALL expose a `SavedPatternsRepository` class with methods: `createPreset(presetName, patternName, params)`, `getAllPresets()`, `updatePreset(id, presetName?, patternName?, params?)`, and `deletePreset(id)`. All database operations SHALL run on `Dispatchers.IO`.

#### Scenario: Creating a preset
- **WHEN** `SavedPatternsRepository.createPreset("My Red", "Solid Color", mapOf("color" to "#ff00001f"))` is called
- **THEN** a row is inserted into `saved_patterns` with the given values and `updated_at` set to the current epoch milliseconds, and the new row's `id` is returned

#### Scenario: Listing all presets
- **WHEN** `SavedPatternsRepository.getAllPresets()` is called
- **THEN** all rows from `saved_patterns` are returned as a list ordered by `preset_name` ascending

#### Scenario: Updating a preset's params
- **WHEN** `SavedPatternsRepository.updatePreset(id=3, params=mapOf("color" to "#0000ff0f"))` is called
- **THEN** the `params` column for row 3 is updated and `updated_at` is refreshed

#### Scenario: Deleting a preset
- **WHEN** `SavedPatternsRepository.deletePreset(id=3)` is called
- **THEN** the row with `id=3` is removed and `true` is returned; if no such row exists, `false` is returned
