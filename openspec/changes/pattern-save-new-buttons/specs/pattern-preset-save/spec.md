## ADDED Requirements

### Requirement: Save button overwrites loaded preset
When the user has loaded an existing saved preset into the editor, the Save button SHALL overwrite that preset's parameters with the current parameter values without prompting for a name.

#### Scenario: Save with loaded preset
- **WHEN** `activePresetId` is non-null (a saved preset is currently loaded) and the user clicks Save
- **THEN** the frontend sends `PUT /api/saved-patterns/{activePresetId}` with the current `patternName` and `params`, and the Saved Patterns panel reflects the updated preset

#### Scenario: Save with loaded preset — API failure
- **WHEN** the user clicks Save with a loaded preset and the PUT request fails
- **THEN** an error message is displayed and the preset is not changed

### Requirement: Save button prompts for name when in unsaved state
When the user is in unsaved state (`activePresetId` is null), clicking the Save button SHALL reveal an inline name input instead of immediately saving.

#### Scenario: Inline name input appears
- **WHEN** `activePresetId` is null and the user clicks Save
- **THEN** an inline text input and a confirm action appear, allowing the user to enter a name for the new preset

#### Scenario: Confirm saves new preset
- **WHEN** the user has entered a non-empty name in the inline input and confirms
- **THEN** the frontend sends `POST /api/saved-patterns` with the current `patternName`, `params`, and the entered `presetName`; `activePresetId` is set to the returned preset's id; and the new preset appears in the Saved Patterns panel

#### Scenario: Empty name rejected
- **WHEN** the inline name input is empty and the user attempts to confirm
- **THEN** the confirm action is disabled or an inline validation message is shown and no request is sent

#### Scenario: Cancel dismisses input
- **WHEN** the inline name input is visible and the user cancels (via Escape or a cancel button)
- **THEN** the inline input is hidden and no request is sent

#### Scenario: Duplicate name rejected
- **WHEN** the user confirms a name that already exists and the server returns 409 Conflict
- **THEN** an error message is displayed indicating the name is already taken and the input remains open

### Requirement: Active preset ID tracks loaded preset
The frontend SHALL maintain an `activePresetId: number | null` state. This value is set when a preset is loaded and is cleared only when the user clicks New or manually selects a different pattern from the dropdown.

#### Scenario: Preset loaded sets activePresetId
- **WHEN** the user loads a saved preset from the Saved Patterns panel
- **THEN** `activePresetId` is set to that preset's `id`

#### Scenario: New button clears activePresetId
- **WHEN** the user clicks the New button
- **THEN** `activePresetId` is set to null

#### Scenario: Pattern dropdown change clears activePresetId
- **WHEN** the user selects a different pattern from the PatternSelector dropdown
- **THEN** `activePresetId` is set to null

#### Scenario: Parameter edits do not clear activePresetId
- **WHEN** the user changes a parameter value while a preset is loaded
- **THEN** `activePresetId` remains unchanged
