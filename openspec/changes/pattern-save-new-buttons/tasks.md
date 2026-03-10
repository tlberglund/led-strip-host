## 1. State Management

- [x] 1.1 Add `activePresetId: number | null` state to `App.tsx` (initialized to null)
- [x] 1.2 Set `activePresetId` when a preset is loaded via `handleLoadPreset` in `App.tsx`
- [x] 1.3 Clear `activePresetId` when the user selects a different pattern via `handlePatternSelect`
- [x] 1.4 Ensure parameter edits (`handleParamChange`) do NOT clear `activePresetId`

## 2. Remove Apply Pattern Button

- [x] 2.1 Delete or empty `ApplyPatternButton.tsx` (or remove the component entirely)
- [x] 2.2 Remove `handleApplyPattern` from `App.tsx`
- [x] 2.3 Remove `ApplyPatternButton` from `ControlsSidebar.tsx` (or wherever it is rendered)

## 3. Save Button

- [x] 3.1 Create a `SavePatternButton` component (or add inline to `ControlsSidebar`) that accepts `activePresetId`, `onSave`, and `onSaveAs` props
- [x] 3.2 When `activePresetId` is non-null, clicking Save calls `onSave` which issues `PUT /api/saved-patterns/{activePresetId}` via `useSavedPatterns`
- [x] 3.3 When `activePresetId` is null, clicking Save reveals an inline name input
- [x] 3.4 Implement inline name input with: text field, confirm button (disabled when empty), and cancel (Escape or cancel button)
- [x] 3.5 On confirm with non-empty name, call `onSaveAs` which issues `POST /api/saved-patterns`; on success set `activePresetId` to the returned preset's `id`
- [x] 3.6 Handle 409 Conflict from POST — display "Name already taken" error inline, keep input open
- [x] 3.7 Display error message on PUT failure

## 4. New Button

- [x] 4.1 Add a `New` button to `ControlsSidebar` (adjacent to Save)
- [x] 4.2 On click, set `selectedPattern` to `patterns[0].name`, set `paramValues` to defaults for that pattern, clear `activePresetId`, and POST to apply the pattern
- [x] 4.3 Add a tooltip to the New button: "Start a new unsaved pattern"

## 5. Unsaved State Label in PatternSelector

- [x] 5.1 Pass `activePresetId` (or an `isUnsaved` boolean) to `PatternSelector` (or `ControlsSidebar`)
- [x] 5.2 When `activePresetId` is null, display `<unsaved pattern>` as a status label near or within the pattern selector area
- [x] 5.3 When `activePresetId` is non-null, display the loaded preset's name as the status label

## 6. Verification

- [ ] 6.1 Load a preset, edit a parameter, click Save — verify PUT is called and panel updates
- [ ] 6.2 Click New, verify pattern resets to first alphabetical, `<unsaved pattern>` label shown
- [ ] 6.3 In unsaved state, click Save, enter a name, confirm — verify POST is called and `activePresetId` is set
- [ ] 6.4 In unsaved state, click Save, enter a duplicate name — verify 409 error shown
- [ ] 6.5 Click Save with empty name — verify confirm button is disabled
- [ ] 6.6 Verify Apply Pattern button is completely gone from the UI
