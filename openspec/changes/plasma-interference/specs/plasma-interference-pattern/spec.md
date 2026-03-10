## ADDED Requirements

### Requirement: PlasmaInterferencePattern exists and is registered
The system SHALL provide a `PlasmaInterferencePattern` class implementing the `Pattern` interface, registered in `DefaultPatternRegistry` under the name `"Plasma (Interference)"`.

#### Scenario: Pattern appears in the pattern list
- **WHEN** a client calls `GET /api/patterns`
- **THEN** the response SHALL include an entry with `name: "Plasma (Interference)"` and four parameter definitions: `colorStart`, `colorEnd`, `speed`, and `scale`

### Requirement: PlasmaInterferencePattern renders using the 4rknova multiplicative interference algorithm
The system SHALL compute each pixel's color by:
1. Normalizing pixel coordinates to UV space: `uvX = x / width - 0.5`, `uvY = y / height - 0.5`, then multiplying both by `scale`
2. Computing phase oscillators:
   - `phaseV = cos(uvY + sin(0.148 - t)) + 2.4 * t`
   - `phaseH = sin(uvX + cos(0.628 + t)) - 0.7 * t`
   where `t` is elapsed time in seconds multiplied by the `speed` parameter
3. Computing the interference value: `plasma = 7.0 * cos(sqrt(uvX² + uvY²) + phaseH) * sin(phaseV + phaseH)`
4. Normalizing to a blend factor: `blendT = 0.5 + 0.5 * cos(plasma)`
5. Linearly interpolating in RGB space between `colorStart` and `colorEnd` using `blendT`

#### Scenario: Pixels are colored according to the interference formula
- **WHEN** the pattern is active and `update()` is called with any elapsed time
- **THEN** each pixel's RGB components SHALL equal the linear interpolation of `colorStart` and `colorEnd` at the blend factor derived from the 4rknova formula

#### Scenario: Speed parameter scales animation rate
- **WHEN** `speed` is set to 2.0 and `update()` is called with elapsed time `dt`
- **THEN** the internal time counter SHALL advance by `2.0 * dt`, producing animation twice as fast as `speed = 1.0`

#### Scenario: Scale parameter changes spatial frequency
- **WHEN** `scale` is set to 2.0
- **THEN** UV coordinates SHALL be multiplied by 2.0 before the wave math, producing bands twice as dense as `scale = 1.0`

### Requirement: PlasmaInterferencePattern exposes four configurable parameters
The system SHALL expose the following parameters via `PatternParameters`:

| Name | Type | Default | Range / Step |
|------|------|---------|--------------|
| `colorStart` | ColorParam | `#0000ff1f` | — |
| `colorEnd` | ColorParam | `#ff00ff1f` | — |
| `speed` | FloatParam | `1.0` | 0.1–5.0, step 0.1 |
| `scale` | FloatParam | `1.0` | 0.1–4.0, step 0.1 |

#### Scenario: Default parameter values produce a valid render
- **WHEN** the pattern is applied with no explicit parameters (all defaults)
- **THEN** the pattern SHALL render without errors, producing a blue-to-magenta gradient animated at normal speed

#### Scenario: colorStart and colorEnd control the gradient endpoints
- **WHEN** `colorStart` is `#ff0000ff` (red, full brightness) and `colorEnd` is `#0000ffff` (blue, full brightness)
- **THEN** pixels at the trough of the interference wave (blendT ≈ 0) SHALL be red and pixels at the peak (blendT ≈ 1) SHALL be blue

### Requirement: PlasmaInterferencePattern interpolates brightness between the two color parameters
The system SHALL interpolate the APA102 brightness byte for each pixel using the same `blendT` factor as the RGB channels:
`brightness = colorStart.brightness + blendT * (colorEnd.brightness - colorStart.brightness)`

#### Scenario: Brightness interpolates between start and end values
- **WHEN** `colorStart` is `#ffffff0a` (brightness = 10) and `colorEnd` is `#ffffff1e` (brightness = 30)
- **THEN** a pixel where `blendT = 0.5` SHALL have APA102 brightness = 20

#### Scenario: Brightness is at colorStart level at the trough
- **WHEN** `blendT = 0.0` for a given pixel
- **THEN** that pixel's APA102 brightness SHALL equal `colorStart.brightness`

#### Scenario: Brightness is at colorEnd level at the peak
- **WHEN** `blendT = 1.0` for a given pixel
- **THEN** that pixel's APA102 brightness SHALL equal `colorEnd.brightness`
