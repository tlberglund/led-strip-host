## Why

The existing `PlasmaPattern` uses a straightforward additive sine/cosine approach. A multiplicative interference-based plasma — derived from the 4rknova algorithm — produces visually richer banding and radial symmetry that looks substantially different on physical LED strips. Adding it gives users a more compelling second plasma option with its own configurable color gradient and motion controls.

## What Changes

- New `PlasmaInterferencePattern` Kotlin class implementing the 4rknova interference plasma algorithm
- Two color parameters (`colorStart`, `colorEnd`) replace the fixed cosine-palette offsets, letting the user define the gradient the plasma cycles through
- `speed` parameter controls overall animation rate
- `scale` parameter controls spatial zoom (tighter or wider band spacing)
- `waveAmplitude` parameter scales the interference product, controlling contrast between light and dark bands
- Registered in `DefaultPatternRegistry` alongside existing patterns
- Pattern name exposed to frontend automatically via the existing patterns API

## Capabilities

### New Capabilities
- `plasma-interference-pattern`: The algorithm, parameters, color mapping, and registration of the new PlasmaInterferencePattern

### Modified Capabilities
<!-- none — no existing spec-level requirements change -->

## Impact

- New file: `src/main/kotlin/com/timberglund/ledhost/pattern/patterns/PlasmaInterferencePattern.kt`
- Modified: `src/main/kotlin/com/timberglund/ledhost/Application.kt` (register the new pattern)
- No API changes, no database changes, no frontend changes beyond the new pattern appearing in the pattern selector
- No new dependencies
