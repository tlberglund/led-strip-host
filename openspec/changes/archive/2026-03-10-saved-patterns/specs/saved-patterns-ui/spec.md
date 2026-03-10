## ADDED Requirements

### Requirement: Frontend restores active pattern on page load
On mount, the Pattern tab SHALL fetch `GET /api/active-pattern` and initialize the pattern selector and parameter controls to match the running pattern, so the UI reflects what the backend is rendering without the user needing to re-select anything.

#### Scenario: Active pattern known on load
- **WHEN** the user opens the app and `GET /api/active-pattern` returns `{"patternName":"Rainbow","params":{"speed":1.5}}`
- **THEN** the pattern selector shows "Rainbow", the parameter controls show the saved values, and no extra apply call is made (the backend is already running the right pattern)

#### Scenario: No active pattern on load
- **WHEN** the user opens the app and `GET /api/active-pattern` returns `{"patternName":"","params":{}}`
- **THEN** the pattern selector shows the default empty/unselected state, same as the current behavior

### Requirement: Saved Patterns panel is shown on the Pattern tab
The Pattern tab SHALL include a "Saved Patterns" section that lists all presets returned by `GET /api/saved-patterns`, displayed below the live pattern editor controls.

#### Scenario: Presets load when Pattern tab opens
- **WHEN** the user navigates to the Pattern tab
- **THEN** the Saved Patterns panel fetches `GET /api/saved-patterns` and renders one row per preset showing the preset name and the pattern type

#### Scenario: No presets saved yet
- **WHEN** `GET /api/saved-patterns` returns an empty array
- **THEN** the panel shows a placeholder message (e.g., "No saved patterns yet")

### Requirement: User can load a saved preset into the live editor
Each preset row SHALL have a Load button that populates the live editor with the preset's pattern type and parameters and immediately applies it to the viewport.

#### Scenario: User clicks Load on a preset
- **WHEN** the user clicks "Load" on a saved preset row
- **THEN** the pattern selector changes to the preset's pattern type, all parameter controls update to the preset's saved values, and the pattern is applied to the viewport as if the user had selected and configured it manually

### Requirement: User can save the current live editor state as a new preset
The live editor SHALL include a "Save As…" control that prompts for a preset name and calls `POST /api/saved-patterns` with the current pattern type and parameters.

#### Scenario: User saves a new preset
- **WHEN** the user enters a preset name in the Save As field and confirms
- **THEN** `POST /api/saved-patterns` is called with the current pattern name and params, the new preset appears in the Saved Patterns list, and the input is cleared

#### Scenario: Duplicate name entered
- **WHEN** the user enters a preset name that already exists and confirms
- **THEN** an inline error message is shown (e.g., "A preset with that name already exists") and no API call is made until a different name is entered

### Requirement: User can update a loaded preset in place
When a preset has been loaded into the live editor and the user subsequently changes parameters, a "Update" button SHALL appear (or be enabled) that calls `PUT /api/saved-patterns/{id}` to overwrite the preset with the current state.

#### Scenario: User updates a loaded preset
- **WHEN** a preset is loaded and the user clicks "Update"
- **THEN** `PUT /api/saved-patterns/{id}` is called with the current pattern name and params, the preset row in the list reflects the updated values, and a brief success indicator is shown

### Requirement: User can rename a preset
Each preset row SHALL have a rename action that allows the user to edit the preset name in place and save it via `PUT /api/saved-patterns/{id}`.

#### Scenario: User renames a preset
- **WHEN** the user clicks the rename action on a preset row, edits the name, and confirms
- **THEN** `PUT /api/saved-patterns/{id}` is called with the new `presetName`, the list row updates to the new name, and the input returns to display mode

#### Scenario: User cancels rename
- **WHEN** the user clicks the rename action but then presses Escape or clicks Cancel
- **THEN** the original name is restored with no API call made

### Requirement: User can delete a preset
Each preset row SHALL have a delete action that calls `DELETE /api/saved-patterns/{id}` after a confirmation prompt and removes the row from the list.

#### Scenario: User deletes a preset
- **WHEN** the user clicks Delete on a preset row and confirms the prompt
- **THEN** `DELETE /api/saved-patterns/{id}` is called and the row is removed from the list

#### Scenario: User cancels delete
- **WHEN** the user clicks Delete but dismisses the confirmation
- **THEN** no API call is made and the row remains in the list

### Requirement: Saved Patterns panel shows inline feedback
The panel SHALL display transient success or error messages after each save, update, rename, or delete operation, clearing automatically after 4 seconds.

#### Scenario: Operation succeeds
- **WHEN** any save, update, rename, or delete API call returns a success status
- **THEN** a brief success message is shown near the action

#### Scenario: Operation fails
- **WHEN** any API call returns an error status
- **THEN** a red error message is shown with a description, and the user's input is preserved for correction
