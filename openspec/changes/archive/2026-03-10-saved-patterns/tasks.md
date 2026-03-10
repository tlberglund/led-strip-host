## 1. Backend — SavedPatternsRepository

- [x] 1.1 Create `src/main/kotlin/com/timberglund/ledhost/db/SavedPatternsRepository.kt`; define a `SavedPatternRow` data class with fields `id: Int`, `presetName: String`, `patternName: String`, `params: Map<String, Any>`, `updatedAt: Long`; inject or accept the same `DataSource` used by `SettingsRepository`
- [x] 1.2 Add `fun createTable()` to `SavedPatternsRepository` that runs `CREATE TABLE IF NOT EXISTS saved_patterns (id SERIAL PRIMARY KEY, preset_name VARCHAR(255) NOT NULL UNIQUE, pattern_name VARCHAR(255) NOT NULL, params JSONB NOT NULL, updated_at BIGINT NOT NULL)`
- [x] 1.3 Add `suspend fun createPreset(presetName: String, patternName: String, params: Map<String, Any>): SavedPatternRow` — INSERT with `updated_at = System.currentTimeMillis()`, return the inserted row; throw `IllegalArgumentException` if `presetName` is blank or already exists (catch `PSQLException` with unique-constraint `SQLState`)
- [x] 1.4 Add `suspend fun getAllPresets(): List<SavedPatternRow>` — SELECT all rows ordered by `preset_name ASC`; deserialize `params` JSONB column to `Map<String, Any>` using Jackson or kotlinx-serialization
- [x] 1.5 Add `suspend fun updatePreset(id: Int, presetName: String? = null, patternName: String? = null, params: Map<String, Any>? = null): SavedPatternRow?` — build a dynamic UPDATE for only the provided non-null fields plus `updated_at`; return the updated row or `null` if not found; throw `IllegalArgumentException` on unique-constraint violation
- [x] 1.6 Add `suspend fun deletePreset(id: Int): Boolean` — DELETE by id; return `true` if a row was deleted, `false` otherwise

## 2. Backend — Active Preset Persistence

- [x] 2.1 Add `suspend fun setActivePresetName(name: String)` to `SettingsRepository` — upsert the key `activePresetName` in the `settings` table
- [x] 2.2 Add `suspend fun getActivePresetName(): String?` to `SettingsRepository` — return the stored value or `null` if absent
- [x] 2.3 In `Application.kt`, after `settingsRepository.connect(...)` and `savedPatternsRepository.createTable()`, call `getActivePresetName()` in `runBlocking`; if a name is returned, look it up in `savedPatternsRepository.getAllPresets()`; if found, pass that preset's `patternName` and `params` into `PreviewServer` as the initial pattern (before the render loop starts); fall back to the first registered pattern with default parameters if absent or not found
- [x] 2.4 In `PreviewServer`, add a `loadPreset(presetName: String, patternName: String, params: Map<String, Any>)` method (or extend the load flow) that calls `setPattern(patternName, params)` and then calls `settingsRepository.setActivePresetName(presetName)` — this is the only code path that updates `activePresetName`; live `POST /api/pattern/:name` calls do NOT write to settings

## 3. Backend — Startup Wiring

- [x] 3.1 In `Application.kt`, instantiate `SavedPatternsRepository` after `settingsRepository.connect(...)` succeeds; call `savedPatternsRepository.createTable()` (blocking); pass the instance into `PreviewServer`

## 4. Backend — API Routes

- [x] 4.1 Add `GET /api/active-pattern` to `PreviewServer` — return `{"patternName": currentPatternName, "params": currentParamValues}` from in-memory state (no DB round-trip needed)
- [x] 4.2 In `PreviewServer` (or a new `savedPatternsRoutes` extension function called from `PreviewServer`), add `GET /api/saved-patterns` — fetch all presets from `SavedPatternsRepository`, serialize to JSON array, respond `200 OK`
- [x] 4.3 Add `POST /api/saved-patterns` — parse body for `presetName`, `patternName`, `params`; return `400` if required fields missing; call `createPreset`; return `201 Created` with new row on success, `409 Conflict` on duplicate name
- [x] 4.4 Add `PUT /api/saved-patterns/{id}` — parse id and body; call `updatePreset`; return `200 OK` with updated row, `404 Not Found` if no row, `409 Conflict` on name collision
- [x] 4.5 Add `DELETE /api/saved-patterns/{id}` — call `deletePreset`; return `204 No Content` if deleted, `404 Not Found` otherwise

## 5. Frontend — Data Hook

- [x] 5.1 Create `frontend/src/hooks/useSavedPatterns.ts`; export a hook that fetches `GET /api/saved-patterns` on mount and after any mutation, and exposes `presets`, `savePreset(presetName, patternName, params)`, `updatePreset(id, patch)`, `deletePreset(id)`, `renamePreset(id, newName)`, `loading`, and `error` state

## 6. Frontend — Active Pattern Sync

- [x] 6.1 In `App.tsx`, add a `useEffect` on mount that calls `GET /api/active-pattern`; if the response has a non-empty `patternName`, call `handlePatternSelect(patternName)` and `setParamValues(params)` without triggering an additional apply (the backend is already running it)

## 7. Frontend — SavedPatternsPanel Component

- [x] 7.1 Create `frontend/src/components/SavedPatternsPanel.tsx`; accept props `onLoad: (patternName: string, params: Record<string, unknown>) => void` and `currentPatternName: string` and `currentParams: Record<string, unknown>`
- [x] 7.2 Render the preset list: one row per preset showing `presetName`, `patternName`, a **Load** button, a rename **Edit** icon, and a **Delete** icon
- [x] 7.3 Implement Load: clicking Load calls `onLoad(preset.patternName, preset.params)` and highlights the loaded row as "active"
- [x] 7.4 Implement inline rename: clicking the edit icon replaces the preset name cell with an `<input>` pre-filled with the current name; Enter/blur calls `renamePreset(id, newName)`; Escape restores the original name without an API call
- [x] 7.5 Implement delete: clicking Delete shows a brief inline confirm prompt ("Delete?  Yes / No"); confirming calls `deletePreset(id)` and removes the row; cancelling dismisses without API call
- [x] 7.6 Render the "Save As…" control: a text input for a new preset name and a **Save** button; clicking Save calls `savePreset(presetName, currentPatternName, currentParams)`; on success clear the input; on duplicate-name error show inline "Name already in use"
- [x] 7.7 When a preset is loaded (tracked by `loadedPresetId` state), show an **Update** button near the Save control; clicking Update calls `updatePreset(loadedPresetId, { patternName: currentPatternName, params: currentParams })`; clear `loadedPresetId` if the loaded preset is deleted
- [x] 7.8 Show a placeholder "No saved patterns yet" when the preset list is empty
- [x] 7.9 Show transient inline success/error feedback (green/red, auto-clears after 4 s) after each mutating operation

## 8. Frontend — Wire into App

- [x] 8.1 In `App.tsx`, import `SavedPatternsPanel` and render it inside the `pattern` tab section (below `ControlsSidebar`); pass `onLoad` as a callback that calls `handlePatternSelect(patternName)` then `setParamValues(params)` then triggers an immediate `fetch` apply; pass `currentPatternName={selectedPattern}` and `currentParams={paramValues}`

## 9. Frontend — CSS

- [x] 9.1 Add styles to `App.css` for `.saved-patterns-panel` (section heading, list layout), `.saved-preset-row` (flex row with name, pattern type badge, and action buttons), `.save-as-row` (input + button inline), `.preset-name-input` (inline rename input), `.preset-active` (highlight for currently loaded preset)

## 10. Verification

- [x] 10.1 Run `./gradlew test` and confirm all tests pass
- [x] 10.2 Run `npm run build` in `frontend/` and confirm no TypeScript errors
