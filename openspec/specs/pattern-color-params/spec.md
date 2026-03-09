## ADDED Requirements

### Requirement: Color parameter wire format is #RRGGBBbb
The wire format for a `ColorParam` value SHALL be a 9-character lowercase string: `#` followed by 6 hex digits for RGB and 2 hex digits for the APA102 brightness field (range `00`–`1f`, i.e. 0–31 decimal). The `default` field of every `ColorParam` declaration SHALL use this format. The backend SHALL pass the string through `parseJsonParams` unchanged. If a stored string is only 7 characters (`#RRGGBB`), `getColor` SHALL treat the brightness as 31 (maximum).

#### Scenario: Full-brightness red is represented correctly
- **WHEN** a color param is set to full-brightness red
- **THEN** the stored string is `"#ff00001f"`

#### Scenario: Half-brightness blue is represented correctly
- **WHEN** a color param is set to blue with brightness 15 (≈ half)
- **THEN** the stored string is `"#0000ff0f"`

#### Scenario: Bare #RRGGBB string defaults brightness to 31
- **WHEN** `getColor` receives a stored value of `"#00ff00"` (7 chars, no brightness suffix)
- **THEN** the returned `Color` has `r=0`, `g=255`, `b=0`, `brightness=31`

### Requirement: Patterns declare color inputs using ColorParam
When a pattern requires a user-specified color, it SHALL declare a `ParameterDef.ColorParam` with a `default` value in `#RRGGBBbb` format. Patterns MUST NOT decompose a user-specified color into separate hue, saturation, and value float parameters. Patterns MUST NOT declare a standalone brightness parameter.

#### Scenario: ColorParam appears in pattern metadata
- **WHEN** a client calls `GET /api/patterns`
- **THEN** any pattern with a color input includes a parameter entry with `"type": "color"` and a `"default"` field in `#RRGGBBbb` format

#### Scenario: Color param value sent from frontend
- **WHEN** the frontend sends a `POST /api/pattern/:name` body containing `{"color": "#ff44001f"}`
- **THEN** the backend passes the string `"#ff44001f"` into `PatternParameters` unchanged

### Requirement: PatternParameters provides a getColor helper
`PatternParameters` SHALL provide a `fun getColor(key: String, default: Color): Color` method that parses a `#RRGGBBbb` string into a `Color(r, g, b, brightness)`. The brightness value SHALL be clamped to 0–31. If the key is absent, the stored value is not a string, or parsing fails, `getColor` SHALL return `default`.

#### Scenario: Valid #RRGGBBbb string is parsed correctly
- **WHEN** `getColor("color", Color.WHITE)` is called and the stored value is `"#ff44001f"`
- **THEN** the returned `Color` has `r=255`, `g=68`, `b=0`, `brightness=31`

#### Scenario: Missing key returns default
- **WHEN** `getColor("color", Color.RED)` is called and no `"color"` key exists
- **THEN** the returned `Color` equals `Color.RED`

#### Scenario: Malformed string returns default
- **WHEN** `getColor("color", Color.BLACK)` is called and the stored value is `"not-a-color"`
- **THEN** the returned `Color` equals `Color.BLACK`

#### Scenario: Out-of-range brightness is clamped
- **WHEN** `getColor("color", Color.BLACK)` is called and the stored value is `"#ffffff3f"` (brightness byte 0x3f = 63, above max 31)
- **THEN** the returned `Color` has `brightness=31`

### Requirement: SolidColorPattern uses ColorParam with embedded brightness
`SolidColorPattern` SHALL declare exactly one `ColorParam` named `"color"` with label `"Color"` and default `"#ff00001f"`. It SHALL NOT declare separate hue, saturation, or value float parameters, and SHALL NOT declare a separate brightness parameter. It SHALL read the full color including brightness via `PatternParameters.getColor("color", Color.RED)`.

#### Scenario: SolidColorPattern metadata
- **WHEN** `GET /api/patterns` is called
- **THEN** the `"Solid Color"` entry contains exactly one parameter with `"name": "color"` and `"type": "color"`, and no parameters named `"hue"`, `"saturation"`, `"value"`, or `"brightness"`

#### Scenario: SolidColorPattern renders chosen color at chosen brightness
- **WHEN** `SolidColorPattern` is initialized with `color = "#0000ff0f"` (blue, brightness 15)
- **THEN** every pixel in the viewport has `r=0`, `g=0`, `b=255`, `brightness=15`

### Requirement: RainbowPattern and PlasmaPattern remove standalone brightness parameter
`RainbowPattern` and `PlasmaPattern` SHALL NOT declare a `ParameterDef` named `"brightness"`. They SHALL render at APA102 hardware brightness 31 (maximum). Intensity control for these patterns remains available via the `"value"` float parameter (HSV V channel).

#### Scenario: RainbowPattern metadata has no brightness param
- **WHEN** `GET /api/patterns` is called
- **THEN** the `"Rainbow"` pattern entry has no parameter with `"name": "brightness"`

#### Scenario: PlasmaPattern metadata has no brightness param
- **WHEN** `GET /api/patterns` is called
- **THEN** the `"Plasma"` pattern entry has no parameter with `"name": "brightness"`

### Requirement: Frontend renders ColorParam fields with RGB/HSV toggle and brightness slider
The frontend `ParameterControl` component SHALL render a color picker popover for any parameter with `type === 'color'`. The popover SHALL contain:
- A saturation+value gradient square and a hue strip (from `react-color` primitives) for visual selection
- A mode toggle with two options: **RGB** and **HSV**
- In RGB mode: numeric inputs for R (0–255), G (0–255), B (0–255)
- In HSV mode: numeric inputs for H (0–360), S (0–100), V (0–100)
- A brightness slider labeled "Brightness", range 0–31, displayed as a percentage (0–100%)
- A read-only hex display showing the current `#RRGGBB` value

The picker SHALL open when the color swatch is clicked and close when the user clicks outside it. RGB SHALL be the canonical internal representation; HSV inputs SHALL be converted to RGB before `onChange` is called. `onChange(param.name, value)` SHALL be called with a `#RRGGBBbb` string.

#### Scenario: Color swatch reflects current color
- **WHEN** a color param has value `"#3399ff1f"`
- **THEN** the swatch element has a CSS background color of `#3399ff`

#### Scenario: Picker opens on swatch click
- **WHEN** the user clicks the color swatch
- **THEN** the picker popover becomes visible

#### Scenario: Picker closes on outside click
- **WHEN** the user clicks outside the picker while it is open
- **THEN** the picker popover is hidden

#### Scenario: RGB mode calls onChange with #RRGGBBbb
- **WHEN** the user sets R=255, G=0, B=0 in RGB mode with brightness slider at 31
- **THEN** `onChange(param.name, "#ff00001f")` is called

#### Scenario: HSV mode converts to RGB and calls onChange
- **WHEN** the user sets H=120, S=100, V=100 in HSV mode (pure green) with brightness at 31
- **THEN** `onChange(param.name, "#00ff001f")` is called

#### Scenario: Brightness slider change updates the bb suffix
- **WHEN** the brightness slider is moved to position 15 while the color is `#0000ff`
- **THEN** `onChange(param.name, "#0000ff0f")` is called
