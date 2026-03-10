## ADDED Requirements

### Requirement: New button resets to first alphabetical pattern
The New button SHALL reset the editor to the first pattern in the alphabetically sorted patterns list with all parameters set to their default values.

#### Scenario: New button clicked
- **WHEN** the user clicks the New button
- **THEN** `selectedPattern` is set to `patterns[0].name` (first entry in the alphabetically sorted list), `paramValues` are set to the default values for that pattern, and the new pattern is applied to the strips

#### Scenario: New button enters unsaved state
- **WHEN** the user clicks the New button
- **THEN** `activePresetId` is set to null and the PatternSelector displays the unsaved-state label

### Requirement: PatternSelector shows unsaved state label
When `activePresetId` is null, the PatternSelector SHALL display a label indicating no preset is currently active (e.g., `<unsaved pattern>`).

#### Scenario: Unsaved state shown in dropdown
- **WHEN** `activePresetId` is null
- **THEN** the PatternSelector label area displays `<unsaved pattern>` or equivalent placeholder text rather than a preset name

#### Scenario: Loaded preset shown in dropdown
- **WHEN** `activePresetId` is non-null
- **THEN** the PatternSelector label area displays the name of the currently loaded preset
