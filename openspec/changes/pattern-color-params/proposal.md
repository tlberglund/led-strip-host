## Why

Patterns that need color inputs currently expose them as decomposed HSV floats (hue, saturation, value) — which is both awkward for pattern authors and produces a confusing UI for users who expect a color picker. The existing `ColorParam` type in `ParameterDef` is already defined but never used by any pattern; this change activates it end-to-end. Brightness is currently a separate `IntParam(0–31)` in each pattern, detached from the color it belongs to. Since APA102 LEDs carry brightness as a fourth per-LED channel alongside R, G, B, brightness belongs inside the color value — not as a loose sibling parameter.

## What Changes

- `ColorParam` (already declared in `ParameterDef`) is now used by patterns that need color inputs
- `SolidColorPattern` is refactored to expose a single `ColorParam` instead of separate hue/saturation/value floats and a separate brightness param
- `RainbowPattern` and `PlasmaPattern` lose their standalone `IntParam("brightness", ...)` — brightness is not applicable as an independent axis for these patterns; per-LED brightness instead comes from the hardware default (full brightness)
- Wire format for color params is `#RRGGBBbb` — a 9-character lowercase hex string where `RRGGBB` is the standard RGB color and `bb` is the APA102 brightness field (0–31) encoded as two hex digits (`00`–`1f`). This keeps the value self-contained in a single string.
- `PatternParameters` gains a helper `getColor(key, default): Color` that parses `#RRGGBBbb` into a `Color(r, g, b, brightness)`
- Backend `setPattern` flow handles color strings as-is (already works for string params); no protocol changes
- Frontend `ParameterControl` component renders `ColorParam` fields with a custom color picker popover that offers **both RGB and HSV input modes** — the user can toggle between them. The picker also includes a brightness slider (0–31, displayed as 0–100%). RGB is the canonical representation sent to the backend; HSV is a UI convenience only.

## Capabilities

### New Capabilities
- `pattern-color-params`: Color parameter type support in patterns — RGB+brightness wire format (`#RRGGBBbb`), HSV/RGB toggle in the UI color picker, `getColor` helper in `PatternParameters`, brightness embedded in color value

### Modified Capabilities
- *(none — existing pattern specs don't exist yet; all pattern behavior is new spec territory)*

## Impact

- `Pattern.kt` — `PatternParameters.getColor()` helper added; `ColorParam.default` format changes to `#RRGGBBbb`
- `SolidColorPattern.kt` — parameters list changes; `initialize()` reads color via `getColor`
- `RainbowPattern.kt` — `IntParam("brightness", ...)` removed; renders at full brightness
- `PlasmaPattern.kt` — `IntParam("brightness", ...)` removed; renders at full brightness
- `PreviewServer.kt` — no changes needed; color strings already pass through unchanged
- `frontend/src/components/ParameterControl.tsx` — new `color` case renders a custom picker with RGB/HSV mode toggle and brightness slider
- `frontend/src/types.ts` — `ColorParamDef` already exists; no type changes needed
- New npm dependency: `react-color` + `@types/react-color`
