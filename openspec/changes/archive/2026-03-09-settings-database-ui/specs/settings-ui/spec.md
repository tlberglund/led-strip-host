## ADDED Requirements

### Requirement: Settings tab is accessible from the top navigation
The system SHALL add a "Settings" tab to the top navigation bar alongside the existing "Pattern" and "Strips" tabs.

#### Scenario: User clicks Settings tab
- **WHEN** the user clicks the "Settings" tab
- **THEN** the Settings tab content is displayed and the other tab content is hidden

### Requirement: Viewport and FPS section displays and saves scalar settings
The Settings tab SHALL include a "Viewport & Performance" section showing editable fields for viewport width, viewport height, and target FPS, with a Save button that persists changes via `PUT /api/settings`.

#### Scenario: Settings load on tab open
- **WHEN** the user opens the Settings tab
- **THEN** the Viewport & Performance fields are populated with values from `GET /api/settings`

#### Scenario: User saves changed values
- **WHEN** the user edits one or more fields and clicks "Save"
- **THEN** `PUT /api/settings` is called with the updated values and a success confirmation is shown

#### Scenario: Invalid input
- **WHEN** the user enters a non-positive integer in a numeric field
- **THEN** the field is highlighted with an error and the Save button is disabled

### Requirement: Background image section allows upload and removal
The Settings tab SHALL include a "Background Image" section with a drag-and-drop upload zone and a file picker button, a preview of the current image (if any), and a Remove button.

#### Scenario: No image currently set
- **WHEN** the Settings tab opens and no background image is stored
- **THEN** the upload zone shows a placeholder prompt ("Drag & drop an image here, or click to browse")

#### Scenario: Image currently set
- **WHEN** the Settings tab opens and a background image is stored
- **THEN** a thumbnail preview of the current image is shown alongside a "Remove" button

#### Scenario: User drags and drops an image file
- **WHEN** the user drags an image file over the upload zone and drops it
- **THEN** the file is uploaded via `POST /api/settings/background-image`, a loading indicator is shown during upload, and the thumbnail preview updates on success

#### Scenario: User selects an image via file picker
- **WHEN** the user clicks the upload zone or a "Browse" button and selects an image file
- **THEN** the same upload flow as drag-and-drop is triggered

#### Scenario: User removes the image
- **WHEN** the user clicks "Remove" on the current image
- **THEN** `DELETE /api/settings/background-image` is called and the upload zone returns to the empty placeholder state

#### Scenario: Non-image file dropped
- **WHEN** the user drops a file that is not an image (e.g., a PDF)
- **THEN** the file is rejected with an inline error message and no upload is attempted

### Requirement: Strip management section allows creating, editing, and deleting strips
The Settings tab SHALL include a "Strip Controllers" section displaying all configured strips in a table with columns for BT Name, Length, Start (X,Y), End (X,Y), Reverse, and Actions, and a form or inline row for adding new strips.

#### Scenario: Strip list loads on tab open
- **WHEN** the user opens the Settings tab
- **THEN** the strip table is populated with all rows from `GET /api/settings/strips`

#### Scenario: User adds a new strip
- **WHEN** the user fills in the "Add Strip" form fields (BT Name, Length, Start X/Y, End X/Y) and clicks "Add"
- **THEN** `POST /api/settings/strips` is called, the new row appears in the table, and the form is cleared

#### Scenario: User edits a strip inline
- **WHEN** the user clicks an "Edit" icon on a strip row and modifies a field
- **THEN** the row enters edit mode with inline inputs; clicking "Save" calls `PUT /api/settings/strips/{id}` and exits edit mode

#### Scenario: User deletes a strip
- **WHEN** the user clicks the "Delete" icon on a strip row
- **THEN** a confirmation prompt appears; on confirmation, `DELETE /api/settings/strips/{id}` is called and the row is removed from the table

#### Scenario: Required field missing on add
- **WHEN** the user clicks "Add" without filling in BT Name or Length
- **THEN** the missing fields are highlighted and the add request is not sent

### Requirement: Settings tab shows save feedback
The Settings tab SHALL display inline success or error feedback after each save or upload operation, clearing automatically after 4 seconds.

#### Scenario: Save succeeds
- **WHEN** a settings save or image upload completes successfully
- **THEN** a green success message is shown near the relevant section

#### Scenario: Save fails
- **WHEN** a settings save or image upload returns an error response
- **THEN** a red error message is shown with the reason, and the user's input is preserved for correction
