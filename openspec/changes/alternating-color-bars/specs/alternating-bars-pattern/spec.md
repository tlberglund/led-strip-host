## ADDED Requirements

### Requirement: Pattern renders alternating color bars with antialiased edges
The system SHALL render a repeating sequence of two color bars across the viewport. Each bar's center SHALL be filled with its assigned color (colorA or colorB). Within a 1-pixel-wide transition zone at each bar boundary, the system SHALL blend between the two colors using a smoothstep function and `Color.blend()`. Bar position for each pixel SHALL be determined by projecting the pixel's (x, y) coordinates onto the bar travel axis: `projection = x * cos(θ) + y * sin(θ)` where θ is the angle in radians.

#### Scenario: Bar centers render as solid color
- **WHEN** the pattern is rendered and a pixel is more than 0.5 pixels from any bar boundary
- **THEN** that pixel SHALL be set to its bar's assigned color with no blending

#### Scenario: Bar boundaries are antialiased
- **WHEN** a pixel falls within 0.5 pixels of a bar boundary
- **THEN** that pixel SHALL be a smoothstep blend between colorA and colorB, with the blend ratio proportional to distance from the boundary

#### Scenario: Even-indexed bar centers use colorA
- **WHEN** the pattern is rendered with two distinct colors
- **THEN** the center of each even-indexed bar SHALL be set to colorA

#### Scenario: Odd-indexed bar centers use colorB
- **WHEN** the pattern is rendered with two distinct colors
- **THEN** the center of each odd-indexed bar SHALL be set to colorB

### Requirement: Bar width is specified in mm
The system SHALL accept bar width as a floating-point value in millimeters. The system SHALL convert mm to viewport units using the constant `MM_PER_LED = 16.0` (one viewport pixel = 16mm physical spacing). The valid range SHALL be 10–1000 mm with a default of 80 mm.

#### Scenario: Bar width converts correctly to pixels
- **WHEN** barWidth is set to 80mm
- **THEN** each bar SHALL span exactly 5 viewport pixels (80 / 16 = 5)

#### Scenario: Minimum bar width is enforced
- **WHEN** a barWidth below 10mm is provided
- **THEN** the system SHALL clamp or reject the value and use 10mm

### Requirement: Bars scroll at configurable speed
The system SHALL animate the bar pattern by applying a time-based scroll offset: `offset = speed * totalTime`. Speed SHALL be expressed in mm/s. A speed of 0 SHALL produce a static, non-scrolling pattern. The valid range SHALL be 0–200 mm/s with a default of 50 mm/s.

#### Scenario: Static pattern at speed zero
- **WHEN** speed is set to 0
- **THEN** the bar pattern SHALL not change position across frames

#### Scenario: Pattern scrolls at positive speed
- **WHEN** speed is set to a positive value
- **THEN** bars SHALL appear to move in the direction of the angle axis over time

### Requirement: Bar angle controls the direction of the bars
The system SHALL accept an angle parameter (0–360°) that controls the orientation of the bars. At 0°, bars SHALL be oriented vertically (scrolling horizontally). The angle SHALL determine the axis onto which pixel coordinates are projected. The valid range SHALL be 0–360° with a default of 0°.

#### Scenario: 0° produces vertical bars
- **WHEN** angle is 0°
- **THEN** all pixels in the same column (same x) SHALL be in the same bar

#### Scenario: 90° produces horizontal bars
- **WHEN** angle is 90°
- **THEN** all pixels in the same row (same y) SHALL be in the same bar

#### Scenario: Arbitrary angle produces diagonal bars
- **WHEN** angle is set to 45°
- **THEN** bars SHALL appear at a 45° diagonal across the viewport

### Requirement: Pattern exposes two configurable colors
The system SHALL expose two `ColorParam` parameters named `colorA` and `colorB`, each accepting a `#RRGGBBbb` color string including APA102 brightness. Default values SHALL be `#ff00001f` (red, full brightness) for colorA and `#0000ff1f` (blue, full brightness) for colorB.

#### Scenario: Both colors are independently configurable
- **WHEN** colorA and colorB are set to different values
- **THEN** even-indexed bars SHALL render with colorA and odd-indexed bars with colorB

#### Scenario: Colors include APA102 brightness
- **WHEN** a color is set with a non-maximum brightness suffix
- **THEN** the rendered LEDs SHALL reflect that brightness level

### Requirement: Pattern is registered and selectable
The system SHALL register `AlternatingBarsPattern` in the `DefaultPatternRegistry` at application startup so it appears in the pattern selector and is accessible via `GET /api/patterns` and `POST /api/pattern/alternating-bars`.

#### Scenario: Pattern appears in pattern list
- **WHEN** a client calls GET /api/patterns
- **THEN** the response SHALL include an entry with the name `alternating-bars`

#### Scenario: Pattern can be activated by name
- **WHEN** a client calls POST /api/pattern/alternating-bars with valid parameters
- **THEN** the system SHALL activate the AlternatingBarsPattern with those parameters
