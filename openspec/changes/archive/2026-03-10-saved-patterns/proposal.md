## Why

The pattern editor currently lets users configure and preview patterns live, but all configuration is ephemeral — switching patterns or restarting the app loses any tuned parameters. Users need the active pattern to survive restarts automatically, and need to save named pattern presets, recall them instantly, and manage them over time without re-entering values.

## What Changes

- The name of the active saved preset is persisted to the database whenever the user loads a preset, and restored automatically on startup so the backend resumes rendering the last-used preset (by looking up its current params from `saved_patterns`).
- A new `GET /api/active-pattern` endpoint lets the frontend initialize its selector and parameter controls to match the running pattern on page load.
- A new `saved_patterns` database table stores pattern presets (name, pattern type, serialized parameters).
- New REST endpoints allow CRUD operations on saved patterns.
- The Pattern tab gains a "Saved Patterns" section listing all presets, with controls to load, edit, rename, and delete them.
- A "Save" action in the live pattern editor saves the current pattern type and parameters as a new or updated preset.
- Loading a saved pattern populates the live editor and immediately applies it to the viewport.

## Capabilities

### New Capabilities
- `saved-patterns-api`: REST API for creating, reading, updating, and deleting saved pattern presets in the database.
- `saved-patterns-ui`: Frontend UI on the Pattern tab for listing, loading, saving, renaming, and deleting pattern presets.

### Modified Capabilities
- `settings-database`: The `saved_patterns` table is added alongside the existing `settings`, `strips`, and `background_image` tables.

## Impact

- **Backend**: `SettingsRepository` gains one new key (`activePresetName`) written when a saved preset is loaded; on startup, that preset name is looked up in `saved_patterns` to restore params. New `SavedPatternsRepository` handles the `saved_patterns` table; new API routes added to `PreviewServer`.
- **Frontend**: Fetches active pattern on mount to pre-populate the editor; `PatternTab` gains a saved-patterns panel.
- **Database**: One new `settings` row (`activePresetName`) tracking which saved preset is active; one new table `saved_patterns(id SERIAL PRIMARY KEY, preset_name VARCHAR NOT NULL UNIQUE, pattern_name VARCHAR NOT NULL, params JSONB NOT NULL, updated_at BIGINT)`.
- **No breaking changes**: existing live pattern editor behavior is unchanged.
