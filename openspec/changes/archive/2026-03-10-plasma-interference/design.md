## Context

The project already has a `PlasmaPattern` that combines four additive sine waves with HSV color output. The new `PlasmaInterferencePattern` implements the 4rknova multiplicative interference algorithm, which produces sharper banding and radial symmetry through a different mathematical structure. It reuses all existing infrastructure: the `Pattern` interface, `PatternParameters`/`ParameterDef`, `ColorParam`/`getColor()`, `Color`, and `Viewport`.

## Goals / Non-Goals

**Goals:**
- Implement the 4rknova plasma algorithm faithfully, adapted from GLSL to Kotlin
- Support two user-configurable colors (start/end) mapped onto the plasma value
- Expose `speed` and `scale` as tuning knobs
- Register the pattern so it appears in the frontend selector with no additional wiring

**Non-Goals:**
- Specular / surface-normal effects (not representable on APA102 LEDs)
- HDR or multi-layer compositing
- Any changes to the frontend, API, or database

## Decisions

### UV normalization
The original GLSL shader uses `uv = fragCoord / iResolution - 0.5`, producing coordinates in roughly `[-0.5, 0.5]`. We replicate this: `uvX = x / width - 0.5`, `uvY = y / height - 0.5`. This keeps the radial distance at `length(uv)` centered on the viewport regardless of aspect ratio.

**`scale` is applied as a multiplier on `uv` before all wave math.** Higher scale → higher spatial frequency → tighter bands. Default 1.0 matches the reference algorithm unchanged.

### Algorithm
The three-step pipeline:

```
// 1. Phase oscillators (nested trig creates non-repeating drift)
phaseV = cos(uvY + sin(0.148 - t)) + 2.4 * t
phaseH = sin(uvX + cos(0.628 + t)) - 0.7 * t

// 2. Interference product (multiplication creates sharp zero-crossings)
radialDist = sqrt(uvX^2 + uvY^2)
plasma = 7.0 * cos(radialDist + phaseH) * sin(phaseV + phaseH)

// 3. Normalize to [0,1] blend factor
blendT = 0.5 + 0.5 * cos(plasma)
```

The constant `7.0` is kept fixed — it controls band density and has been experimentally tuned in the reference; exposing it as a parameter would be confusing without significant benefit for a first pass.

### Color mapping
`blendT` linearly interpolates between `colorStart` and `colorEnd` in RGB space:

```
r          = colorStart.r          + blendT * (colorEnd.r          - colorStart.r)
g          = colorStart.g          + blendT * (colorEnd.g          - colorStart.g)
b          = colorStart.b          + blendT * (colorEnd.b          - colorStart.b)
brightness = colorStart.brightness + blendT * (colorEnd.brightness - colorStart.brightness)
```

Brightness is interpolated using the same `blendT` as RGB, so the full `#RRGGBBbb` value of both colors contributes to each pixel. This lets users set, for example, a dim cool start and a bright warm end to reinforce the plasma's depth cues.

Linear RGB lerp is chosen over HSV because the cosine-palette aesthetic of this algorithm looks best with smooth perceptual transitions between saturated hues. HSV interpolation would produce hue rotations that fight the inherent color structure of the effect.

**Alternative considered:** expose separate brightness parameter. Rejected — the `#RRGGBBbb` color format already embeds brightness and is consistent with `SolidColorPattern` and `PlasmaPattern`.

### Parameters
| Name | Type | Range | Default | Purpose |
|------|------|-------|---------|---------|
| `colorStart` | ColorParam | — | `#0000ff1f` | Gradient start (trough of plasma) |
| `colorEnd` | ColorParam | — | `#ff00ff1f` | Gradient end (peak of plasma) |
| `speed` | FloatParam | 0.1–2.0, step 0.1 | 1.0 | Time multiplier |
| `scale` | FloatParam | 0.1–4.0, step 0.1 | 1.0 | Spatial frequency |

### Registration
Added to `DefaultPatternRegistry` in `Application.kt` after the existing `PlasmaPattern` registration. Pattern name: `"Plasma (Interference)"`.

## Risks / Trade-offs

- **Trig-heavy inner loop** — four trig calls per pixel. At 240×135 = 32,400 pixels × 60 FPS the CPU load is non-trivial. Kotlin's `kotlin.math` functions map to JVM intrinsics on modern JVMs, so this should be fine in practice, same as the existing `PlasmaPattern`. If needed, a LUT could be introduced later.
- **Linear RGB lerp** — looks good for hue-complementary color pairs; for colors close in hue it may look washed-out near the midpoint. The user can work around this by choosing more distinct colors.
