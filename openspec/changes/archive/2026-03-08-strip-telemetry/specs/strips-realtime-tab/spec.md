## ADDED Requirements

### Requirement: Frontend handles the `strip_telemetry` WebSocket message type
The Strips tab SHALL process incoming `strip_telemetry` messages received on the `/ws/strips` WebSocket and update the corresponding strip's telemetry state. Unknown message types SHALL be silently ignored.

#### Scenario: `strip_telemetry` message received for a known strip
- **WHEN** a `strip_telemetry` message arrives with a `stripId` matching a known strip
- **THEN** that strip's current temperature, current, uptime, frame count, and history arrays are updated in state within one render cycle

#### Scenario: `strip_telemetry` message received before any `strips_update`
- **WHEN** a `strip_telemetry` message arrives before the first `strips_update`
- **THEN** the telemetry data is stored and displayed once the strip appears in a subsequent `strips_update`

### Requirement: Each strip card displays the latest telemetry values
Each strip card in the Strips tab SHALL display the most recent telemetry values when available: temperature (°C, 1 decimal place), current (A, 2 decimal places), uptime (formatted as `Xd Xh Xm Xs`), and frame count (integer with thousands separator).

#### Scenario: Telemetry data available for strip
- **WHEN** at least one `strip_telemetry` message has been received for a strip
- **THEN** the strip card shows temperature, current, uptime, and frame count in a telemetry section below the connection status

#### Scenario: No telemetry data yet for strip
- **WHEN** no `strip_telemetry` message has been received for a strip
- **THEN** the telemetry section shows a placeholder such as "Awaiting telemetry…"

#### Scenario: Strip is disconnected
- **WHEN** a strip's `connected` field is `false`
- **THEN** the telemetry section continues to show the last received values (stale data) until new data arrives or the tab is reloaded

### Requirement: Each strip card shows a sparkline of temperature and current history
Each strip card SHALL render two SVG sparklines — one for temperature and one for current — using the `history` arrays from the most recent `strip_telemetry` message. Sparklines SHALL be rendered as inline SVG `<polyline>` elements with no external charting dependency.

#### Scenario: History arrays contain data
- **WHEN** the `history.temperature` or `history.current` array has two or more entries
- **THEN** a sparkline polyline is rendered, scaled to fit within its SVG container, with the oldest value on the left and newest on the right

#### Scenario: History arrays have fewer than two entries
- **WHEN** either history array has zero or one entry
- **THEN** the sparkline area shows a placeholder (e.g., a horizontal line or "—") rather than a polyline

#### Scenario: All readings in history are identical
- **WHEN** all values in a history array are the same
- **THEN** the sparkline renders as a flat horizontal line at the midpoint of the container
