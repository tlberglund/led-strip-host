## ADDED Requirements

### Requirement: Backend broadcasts per-LED colors after each rendered frame
After each frame render cycle the backend SHALL broadcast one `strip_leds` WebSocket message per strip to all connected `/ws/strips` clients, throttled to the same rate as the viewport WebSocket broadcast (nominally 20 FPS). Each message SHALL contain the strip ID and the RGB colors of all LEDs in that strip encoded as a lowercase hexadecimal string (6 characters per LED, `RRGGBB` order, no separators). The colors SHALL reflect the values most recently sent to the strip hardware.

#### Scenario: Frame rendered with connected strips
- **WHEN** a frame is rendered and the WebSocket broadcast interval has elapsed
- **THEN** one `strip_leds` message is sent for each configured strip containing `stripId` and the `rgb` hex string for all LEDs in that strip

#### Scenario: Strip has zero configured LEDs
- **WHEN** a strip is configured with zero length
- **THEN** no `strip_leds` message is sent for that strip

#### Scenario: No WebSocket clients connected
- **WHEN** a frame is rendered and no clients are subscribed to `/ws/strips`
- **THEN** the broadcast attempt completes without error and no message is stored or queued

### Requirement: `strip_leds` message carries a compact hex-encoded RGB payload
The `rgb` field in a `strip_leds` message SHALL be a lowercase hexadecimal string of exactly `ledCount × 6` characters. Each 6-character group SHALL represent one LED as `RRGGBB`. The first group SHALL correspond to LED index 0.

#### Scenario: Strip with 3 LEDs — red, green, blue
- **WHEN** a strip has 3 LEDs with colors red (255, 0, 0), green (0, 255, 0), blue (0, 0, 255)
- **THEN** the `rgb` field is `"ff000000ff000000ff"`

#### Scenario: All LEDs black (strip off)
- **WHEN** all LEDs in a strip have color (0, 0, 0)
- **THEN** the `rgb` field is a string of `ledCount × 6` zero characters (e.g., `"000000000000"` for 2 LEDs)

### Requirement: Frontend handles `strip_leds` WebSocket messages
The Strips tab frontend SHALL process `strip_leds` messages and maintain per-strip LED color state. The state SHALL be updated on every received message. Unknown message types SHALL continue to be silently ignored.

#### Scenario: `strip_leds` message received
- **WHEN** a `strip_leds` message arrives for strip N
- **THEN** the decoded RGB color array for strip N is updated in state within one render cycle

#### Scenario: `strip_leds` message received before strips are known
- **WHEN** a `strip_leds` message arrives before any `strips_update` message
- **THEN** the LED data is stored and used when the strip card is eventually rendered

### Requirement: Each strip card renders a real-time LED color display
Each strip card in the Strips tab SHALL display a `<canvas>` element showing one colored square per LED, with the color of each square matching the most recently received `strip_leds` data for that strip. The canvas SHALL update on every received `strip_leds` message for that strip.

#### Scenario: LED color data available
- **WHEN** at least one `strip_leds` message has been received for a strip
- **THEN** the strip card shows a canvas with `ledCount` colored squares in a horizontal row, each square's fill matching the corresponding LED's RGB color

#### Scenario: No LED data received yet
- **WHEN** no `strip_leds` message has been received for a strip
- **THEN** the canvas area shows a row of dark placeholder squares (all black or dark grey)

#### Scenario: Colors update in real time
- **WHEN** a new `strip_leds` message arrives for a strip whose card is visible
- **THEN** the canvas redraws within one animation frame to show the new colors
