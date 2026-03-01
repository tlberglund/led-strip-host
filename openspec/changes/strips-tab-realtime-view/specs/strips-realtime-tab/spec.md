## ADDED Requirements

### Requirement: Strips tab subscribes to WebSocket for real-time updates
The Strips tab SHALL maintain an open WebSocket connection to `/ws/strips` whenever the tab is active and display strip data from server push messages rather than polling.

#### Scenario: Tab becomes active and connection opens
- **WHEN** the user navigates to the Strips tab
- **THEN** a WebSocket connection to `/ws/strips` is opened within 100 ms and the tab displays the strip list from the first `strips_update` message received

#### Scenario: Tab becomes inactive and connection closes
- **WHEN** the user navigates away from the Strips tab
- **THEN** the `/ws/strips` WebSocket connection is closed

#### Scenario: WebSocket connection lost while tab is active
- **WHEN** the `/ws/strips` connection drops unexpectedly
- **THEN** the tab displays a "Reconnecting…" indicator and attempts to reconnect with exponential backoff (initial delay 1 s, max 30 s)

### Requirement: Each strip is shown with name, connection status, and action buttons
The Strips tab SHALL display each discovered strip as a row containing the strip name, a connection status badge, and two separate action buttons: Disconnect (for connected strips) and Reconnect (for disconnected strips).

#### Scenario: Strip is connected
- **WHEN** a strip's `connected` field is `true`
- **THEN** the row shows a "Connected" badge and a "Disconnect" button; the "Reconnect" button is not shown

#### Scenario: Strip is disconnected
- **WHEN** a strip's `connected` field is `false`
- **THEN** the row shows a "Disconnected" badge and a "Reconnect" button; the "Disconnect" button is not shown

#### Scenario: User clicks Disconnect
- **WHEN** the user clicks the "Disconnect" button for a strip
- **THEN** a `POST /api/strips/{id}/disconnect` request is made and the UI optimistically updates the badge to "Disconnected" while awaiting the server-pushed confirmation

#### Scenario: User clicks Reconnect
- **WHEN** the user clicks the "Reconnect" button for a strip
- **THEN** a `POST /api/strips/{id}/connect` request is made and the UI shows a "Connecting…" badge while awaiting the server-pushed confirmation

### Requirement: Strip address and LED count are displayed
The Strips tab SHALL show each strip's BLE address and LED count (from configuration) in the strip row.

#### Scenario: Strip row is rendered
- **WHEN** a strip row is displayed
- **THEN** the row includes the BLE MAC address and the LED count (e.g., "144 LEDs")

### Requirement: Activity log shows real-time discovery events
The Strips tab SHALL display a scrolling activity log below the strip list, showing discovery event messages received from the server with timestamps.

#### Scenario: New discovery event received
- **WHEN** the client receives a `discovery_event` WebSocket message
- **THEN** a new entry is prepended to the activity log with the current local time and the event message text

#### Scenario: Activity log is bounded
- **WHEN** the number of activity log entries exceeds 50
- **THEN** the oldest entries are removed so that at most 50 entries are shown

#### Scenario: No events yet
- **WHEN** the Strips tab is first opened and no `discovery_event` messages have been received
- **THEN** the activity log shows a placeholder message such as "Waiting for discovery events…"

### Requirement: Empty state shown when no strips are discovered
The Strips tab SHALL display a clear empty state when the strip list is empty.

#### Scenario: No strips discovered
- **WHEN** the `strips_update` message contains an empty array
- **THEN** the strip list area shows "No strip controllers discovered." and the activity log is still visible

### Requirement: Tab shows scanning indicator during active scan
The Strips tab SHALL display a visual indicator when the backend is actively performing a BLE scan.

#### Scenario: Scan started event received
- **WHEN** the client receives a `discovery_event` message whose text begins with "Scanning"
- **THEN** a scanning indicator (e.g., spinner or "Scanning…" label) is shown in the tab header area

#### Scenario: Scan completed event received
- **WHEN** the client receives a `discovery_event` message whose text begins with "Scan complete"
- **THEN** the scanning indicator is hidden
