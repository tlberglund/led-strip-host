## Context

The app already has a live pattern editor (pattern type selector + parameter controls that auto-apply to the viewport) and a PostgreSQL database with `SettingsRepository` managing schema creation and CRUD. The `PreviewServer` exposes REST endpoints consumed by the React frontend. Patterns are identified by a string name (e.g., `"Solid Color"`, `"Rainbow"`) and carry a `Map<String, Any>` of parameter values.

## Goals / Non-Goals

**Goals:**
- Persist the active pattern (type + params) automatically on every change; restore it on startup.
- Expose the active pattern via a REST endpoint so the frontend can sync on page load.
- Persist named pattern presets to the database.
- Let users save the current live editor state as a new preset or overwrite an existing one.
- Let users load a preset into the live editor (and apply it immediately).
- Let users rename and delete presets.
- Present saved presets in a clean list on the Pattern tab.

**Non-Goals:**
- Ordering/sorting presets (alphabetical by name is sufficient).
- Tagging, categorizing, or sharing presets.
- Versioning or history of preset changes.
- Exporting/importing presets as files.

## Decisions

### Decision 1: Store params as JSONB, not TEXT

**Choice**: `params JSONB NOT NULL` column.

**Rationale**: The existing live-editor flow already serializes params as a JSON object sent to the backend (`POST /api/pattern/:name` body). Storing JSONB allows future querying of params without reparsing, and round-trips cleanly to/from Kotlin's `Map<String, Any>` via kotlinx-serialization or Jackson.

**Alternative considered**: TEXT (JSON string). Simpler DDL but loses type fidelity and adds an extra parse step on every read.

### Decision 2: `preset_name` is user-visible; `id` is internal

**Choice**: A serial `id` is the primary key; `preset_name VARCHAR(255) NOT NULL UNIQUE` is what users see and search by.

**Rationale**: Using a surrogate key lets the user rename presets without changing references. The UI passes `id` for load/delete/rename operations.

### Decision 3: Save = upsert by preset_name from client perspective

**Choice**: `POST /api/saved-patterns` creates if `preset_name` is new; a `PUT /api/saved-patterns/{id}` updates name or params. The frontend uses POST for "Save As New" and PUT for "Update".

**Rationale**: Cleaner than a single "upsert by name" endpoint; lets the user deliberately choose to overwrite vs. create new. The UI can offer an "Update" button alongside "Save As New" when the user loaded an existing preset.

### Decision 4: Active pattern is a reference to a saved preset, not a duplicated params copy

**Choice**: One `settings` row — `activePresetName` — stores the `preset_name` of the last-loaded saved preset. On startup, `Application.kt` reads this key, looks up the named preset in `saved_patterns` to get its pattern type and params, and starts the engine. If the named preset no longer exists, the system falls back to the first registered pattern with defaults.

**Rationale**: Storing only the name avoids duplicating params across two tables. The authoritative copy of a preset's params is always in `saved_patterns`; if the user updated the preset after loading it, the startup restore picks up the latest version automatically. Live param tweaks between save operations are intentionally ephemeral — they don't need to survive a restart.

**Alternative considered**: Store params JSON separately in settings alongside the name. Adds redundancy, creates a stale-data risk if the preset is later edited, and adds write overhead on every live param change.

**Implication**: `setActivePattern()` is called only when the user explicitly loads a saved preset (not on every live param tweak). The backend's in-memory current pattern state is what `GET /api/active-pattern` reflects.

### Decision 5: New `SavedPatternsRepository` rather than extending `SettingsRepository`

**Choice**: A dedicated `SavedPatternsRepository` class handles the `saved_patterns` table DDL and CRUD.

**Rationale**: `SettingsRepository` is already large; keeping concerns separate makes both files more maintainable. The new repository can share the same JDBC `DataSource` created by `SettingsRepository.connect()`.

### Decision 6: Frontend — new `SavedPatternsPanel` component

**Choice**: A self-contained `SavedPatternsPanel` component rendered below the existing `ControlsSidebar` on the Pattern tab. It owns its own API calls via a `useSavedPatterns` hook.

**Rationale**: Keeps `App.tsx` from growing further; the panel can manage its own list state and optimistic updates independently of the live-editor state. The panel communicates with the live editor only via a single `onLoad(patternName, params)` callback prop.

## Risks / Trade-offs

- [Risk] `JSONB` requires PostgreSQL 9.4+; already a hard dependency so no additional constraint.
- [Risk] `preset_name UNIQUE` means two users on the same server can't have presets with the same name. → Acceptable: this is a single-user local tool.
- [Risk] If `SettingsRepository.connect()` has already run before `SavedPatternsRepository.createTable()`, the new table won't exist. → Mitigation: call `SavedPatternsRepository.createTable()` inside the same startup sequence, after `SettingsRepository.connect()`.

## Migration Plan

1. Add `SettingsRepository.setActivePresetName(name: String)` and `getActivePresetName(): String?` writing/reading the `activePresetName` key.
2. Add `SavedPatternsRepository` with `createTable()` that runs `CREATE TABLE IF NOT EXISTS saved_patterns (...)`.
3. In `Application.kt` startup, after DB connect and `savedPatternsRepository.createTable()`, call `getActivePresetName()` in `runBlocking`; look up the returned name in `SavedPatternsRepository.getAllPresets()`; if found, initialize the pattern engine with that preset's pattern type and params before the render loop starts; fall back to the first registered pattern with defaults if absent or the named preset no longer exists.
4. In `PreviewServer`, when a saved preset is loaded (via the load mechanism in the API), call `settingsRepository.setActivePresetName(presetName)`. Live param tweaks via `POST /api/pattern/:name` do NOT update `activePresetName`.
5. Add `GET /api/active-pattern` route returning `{"patternName": "...", "params": {...}}` from in-memory state.
6. Add CRUD routes for saved patterns to `PreviewServer`.
7. Add `SavedPatternsPanel` component and `useSavedPatterns` hook to the frontend.
8. In `App.tsx`, fetch `GET /api/active-pattern` on mount and initialize `selectedPattern` + `paramValues` from the response.
9. Wire `onLoad` callback in `App.tsx` to `handlePatternSelect` + `setParamValues` + immediate apply.

No rollback concerns: `CREATE TABLE IF NOT EXISTS` is idempotent; removing the feature just means leaving the table unused.
