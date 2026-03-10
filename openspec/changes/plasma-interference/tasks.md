## 1. New Pattern Class

- [x] 1.1 Create `src/main/kotlin/com/timberglund/ledhost/pattern/patterns/PlasmaInterferencePattern.kt` with the `PlasmaInterferencePattern` class implementing `Pattern`
- [x] 1.2 Declare four `ParameterDef` entries: `colorStart` (ColorParam, default `#0000ff1f`), `colorEnd` (ColorParam, default `#ff00ff1f`), `speed` (FloatParam, 0.1–5.0, step 0.1, default 1.0), `scale` (FloatParam, 0.1–4.0, step 0.1, default 1.0)
- [x] 1.3 Implement `initialize()` to read all four parameters from `PatternParameters`
- [x] 1.4 Implement `update(dt)` to advance internal time counter by `dt * speed`
- [x] 1.5 Implement `render(viewport)` using the 4rknova algorithm: UV normalization with scale, phaseV/phaseH oscillators, interference product, blendT cosine-normalization, and linear RGB lerp between colorStart and colorEnd
- [x] 1.6 Interpolate the APA102 brightness byte using the same `blendT` as RGB: `brightness = colorStart.brightness + blendT * (colorEnd.brightness - colorStart.brightness)`

## 2. Registration

- [x] 2.1 Add `PlasmaInterferencePattern` to `DefaultPatternRegistry` in `Application.kt`, after the existing `PlasmaPattern` entry, with name `"Plasma (Interference)"`
