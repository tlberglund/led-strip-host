## Context

The LED strip host renders patterns by writing to a 2D `Viewport` (pixel grid, origin top-left). Physical LEDs are mapped from viewport coordinates via `PixelMapper`. All existing patterns iterate viewport pixels and apply a color function based on coordinates and time. This pattern follows the same model.

Physical LED spacing is ~16mm, so one viewport unit (pixel) represents approximately 16mm in physical space. Bar width is user-specified in mm, making it intuitive regardless of LED density.

## Goals / Non-Goals

**Goals:**
- Render alternating solid-color bars that scroll over time
- Bar width specified in mm (converted to viewport units at render time)
- Bar angle 0–360° (direction bars travel across the viewport)
- Two independently configurable colors (full `#RRGGBBbb` with brightness)
- Speed control for scroll rate

**Non-Goals:**
- Configurable softness/edge-width parameter (transition width is fixed)
- Per-strip or per-axis independent speed
- Dynamic bar count (derived from width)

## Decisions

### Bar Rendering via Directional Projection with Antialiased Edges

Each pixel (x, y) is projected onto the axis perpendicular to the bars using:

```
projection = x * cos(θ) + y * sin(θ)
```

where θ is the bar angle in radians. An offset driven by `speed * totalTime` scrolls the bars continuously. Bar color is determined with a 1-pixel antialiased transition zone at each boundary:

```
t           = (projection + offset) / barWidthPixels
frac        = t - floor(t)                             // [0, 1) within current bar
barIndex    = floor(t).toInt()
baseColor   = if (barIndex % 2 == 0) colorA else colorB
otherColor  = if (barIndex % 2 == 0) colorB else colorA

// Distance in pixels to nearest bar boundary
distToEdge  = min(frac, 1f - frac) * barWidthPixels

// Smoothstep blend: 0 = fully otherColor at boundary, 1 = fully baseColor at center
raw         = clamp(distToEdge / TRANSITION_HALF_PX, 0f, 1f)   // TRANSITION_HALF_PX = 1.5f
smooth      = raw * raw * (3f - 2f * raw)

color       = Color.blend(otherColor, baseColor, smooth)
```

Since bars always alternate, both boundaries of any bar blend toward the same `otherColor`, so a single `distToEdge` check covers both edges symmetrically.

**Alternatives considered:**
- Hard `floor()` cutoff with no blending — produces sharp staircase artifacts at diagonal angles, visible as LEDs jump discretely between colors.
- Cosine-wave soft cycle — smooth but bars are never truly solid; the center always has some gradient. Rejected in favor of solid bars with edge-only blending.
- Configurable transition width parameter — adds UI complexity for a perceptual detail users are unlikely to tune. Fixed at 3 pixels / 48mm (TRANSITION_HALF_PX = 1.5f), which ensures the blend spans enough LEDs to appear smooth at any scroll speed.

### mm-to-Pixels Conversion

`barWidthPixels = barWidthMm / MM_PER_LED` where `MM_PER_LED = 16.0f`.

This constant matches the physical LED spacing stated in the requirements. It is defined as a named constant in the pattern class (not a parameter) so it can be adjusted if LED density ever changes.

**Alternative:** Make MM_PER_LED a global config value — unnecessary complexity for a single constant; defer until there is a real need.

### Parameter Definitions

| Parameter | Type | Range | Default | Notes |
|-----------|------|-------|---------|-------|
| `colorA` | ColorParam | — | `#ff00001f` | First bar color |
| `colorB` | ColorParam | — | `#0000ff1f` | Second bar color |
| `barWidth` | FloatParam | 10–1000 mm | 80 mm | ~5 LED pitches |
| `speed` | FloatParam | -500–500 mm/s | 50 mm/s | 0 = static, negative = reverse |
| `angle` | FloatParam | 0–360° | 0° | Direction of travel |

Speed in mm/s keeps the control intuitive: a value of 100 mm/s scrolls one bar-width every `barWidth/100` seconds.

### No Custom Frontend Component

All parameter types (`ColorParam`, `FloatParam`) are already handled generically by `ParameterControl.tsx`. No new frontend code is required — the UI is auto-generated from the pattern's `parameters` list.

## Risks / Trade-offs

- **Floating-point projection at every pixel per frame** — acceptable; all existing patterns do per-pixel math. The viewport is not large.
- **angle=0 and angle=360 visually identical** — expected behavior; 360° wraps to 0°. No mitigation needed.
- **Very narrow bar widths (≤16mm = 1 pixel)** — at minimum width, the entire bar falls within the transition zone and bars never appear fully solid; they will look like a soft blend. This is acceptable at the extreme minimum and not worth special-casing.
- **Transition width fixed at 3 pixels / 48mm** — at very wide bars (1000mm ≈ 62 pixels) the blend zone is ~5% of the bar, imperceptible from a distance. At narrow bars (10mm ≈ 0.6 pixels) the bar is entirely within the transition zone and appears as a soft gradient rather than a solid bar; acceptable at that extreme.
