## Requirements

### Requirement: Restore active pattern on load
The frontend SHALL, on initial page load, fetch the active pattern from `GET /api/active-pattern` and restore the UI controls to match the returned pattern parameters.

#### Scenario: Active pattern available
- **WHEN** the page loads and `GET /api/active-pattern` returns a pattern
- **THEN** all pattern parameter controls are set to the values from the active pattern

#### Scenario: No active pattern
- **WHEN** the page loads and `GET /api/active-pattern` returns null or empty
- **THEN** controls remain at their default values

### Requirement: Saved Patterns panel displayed
The frontend SHALL display a "Saved Patterns" panel that lists all saved patterns fetched from `GET /api/saved-patterns`.

#### Scenario: Patterns available
- **WHEN** the Saved Patterns panel is rendered and patterns exist
- **THEN** each saved pattern is displayed with its name and action controls (load, rename, delete)

#### Scenario: No patterns saved
- **WHEN** the Saved Patterns panel is rendered and no patterns exist
- **THEN** the panel displays an empty-state message indicating no patterns have been saved yet

### Requirement: Load saved pattern
The user SHALL be able to load a saved pattern, applying its parameters to the active pattern controls.

#### Scenario: User loads a pattern
- **WHEN** the user clicks the load/apply action for a saved pattern
- **THEN** all pattern parameter controls are updated to match the saved pattern's values and the pattern is sent to the strips

### Requirement: Save current pattern
The user SHALL be able to save the current pattern parameters under a new name.

#### Scenario: User saves a new pattern
- **WHEN** the user enters a name and confirms the save action
- **THEN** the frontend sends `POST /api/saved-patterns` with the current parameters and the new pattern appears in the Saved Patterns panel

#### Scenario: Empty name rejected
- **WHEN** the user attempts to save without entering a name
- **THEN** the save action is disabled or an inline validation message is shown

### Requirement: Update saved pattern
The user SHALL be able to overwrite an existing saved pattern with the current parameter values.

#### Scenario: User updates an existing pattern
- **WHEN** the user selects an existing pattern and confirms an update/overwrite action
- **THEN** the frontend sends `PUT /api/saved-patterns/{id}` with the current parameters and the panel reflects the updated pattern

### Requirement: Rename saved pattern
The user SHALL be able to rename an existing saved pattern without changing its parameters.

#### Scenario: User renames a pattern
- **WHEN** the user edits the name of an existing pattern and confirms
- **THEN** the frontend sends `PUT /api/saved-patterns/{id}` with the new name and the panel updates to show the new name

#### Scenario: Empty name rejected
- **WHEN** the user attempts to confirm a rename with an empty name
- **THEN** the rename is cancelled or an inline validation message is shown

### Requirement: Delete saved pattern
The user SHALL be able to delete a saved pattern from the panel.

#### Scenario: User deletes a pattern
- **WHEN** the user confirms the delete action for a saved pattern
- **THEN** the frontend sends `DELETE /api/saved-patterns/{id}` and the pattern is removed from the panel

#### Scenario: Deletion requires confirmation
- **WHEN** the user clicks the delete action for a saved pattern
- **THEN** a confirmation prompt or destructive-action affordance is shown before the delete request is sent

### Requirement: Operation feedback
The frontend SHALL provide visible feedback to the user after save, update, rename, and delete operations.

#### Scenario: Successful operation
- **WHEN** a save, update, rename, or delete operation completes successfully
- **THEN** a brief success message or visual indicator is shown to the user

#### Scenario: Failed operation
- **WHEN** a save, update, rename, or delete operation fails (e.g., network error or server error)
- **THEN** an error message is displayed and no optimistic UI change is retained
