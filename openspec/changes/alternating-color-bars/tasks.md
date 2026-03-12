## 1. Backend Pattern Implementation

- [x] 1.1 Create `AlternatingBarsPattern.kt` in `src/main/kotlin/com/timberglund/ledhost/pattern/patterns/` implementing the `Pattern` interface
- [x] 1.2 Declare the five `ParameterDef` entries: `colorA` (ColorParam, default `#ff00001f`), `colorB` (ColorParam, default `#0000ff1f`), `barWidth` (FloatParam, 10–1000, default 80), `speed` (FloatParam, 0–200, default 50), `angle` (FloatParam, 0–360, default 0)
- [x] 1.3 In `initialize()`, read all five parameters using `params.getColor()` / `params.get<Float>()` and store as instance fields; convert `barWidth` mm → pixels and `angle` degrees → radians
- [x] 1.4 In `update()`, accumulate the scroll offset as `speed_px_per_s * deltaTime` (convert speed from mm/s to px/s using `MM_PER_LED`)
- [x] 1.5 In `render()`, iterate all `(x, y)` viewport pixels; compute `t = (x * cos(θ) + y * sin(θ) + scrollOffset) / barWidthPixels`; derive `frac = t - floor(t)`, `barIndex = floor(t).toInt()`, `baseColor`, and `otherColor`; compute `distToEdge = min(frac, 1f - frac) * barWidthPixels`; apply smoothstep blend `raw = clamp(distToEdge / 0.5f, 0f, 1f)`, `smooth = raw * raw * (3f - 2f * raw)`; call `viewport.setPixel(x, y, Color.blend(otherColor, baseColor, smooth))`
- [x] 1.6 Define `MM_PER_LED = 16.0f` and `TRANSITION_HALF_PX = 0.5f` as companion object constants

## 2. Pattern Registration

- [x] 2.1 In `Application.kt`, add `patternRegistry.register(AlternatingBarsPattern())` alongside the other pattern registrations
- [x] 2.2 Verify the pattern name returned by `AlternatingBarsPattern.name` is `"alternating-bars"` (kebab-case, consistent with API naming convention)

## 3. Verification

- [x] 3.1 Run the application and confirm `GET /api/patterns` includes `alternating-bars` with all five parameters
- [x] 3.2 Activate the pattern via `POST /api/pattern/alternating-bars` with default params and confirm bars render in the preview
- [x] 3.3 Test angle=0° produces vertical bars, angle=90° produces horizontal bars
- [x] 3.4 Test speed=0 produces a static pattern; positive speed produces scrolling
- [x] 3.5 Test barWidth=80mm produces bars approximately 5 pixels wide in the viewport
- [x] 3.6 Confirm both colors render correctly including APA102 brightness component
