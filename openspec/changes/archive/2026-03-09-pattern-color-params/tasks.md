## 1. Backend — PatternParameters Helper

- [x] 1.1 Add `fun getColor(key: String, default: Color): Color` to `PatternParameters` in `Pattern.kt`; parse `#RRGGBBbb` by taking `substring(1,7)` for RGB and `substring(7,9)` for brightness (clamped to 0–31); if the string is only 7 chars long treat brightness as 31; return `default` on any missing key, wrong type, or parse error

## 2. Backend — Remove Standalone Brightness Params

- [x] 2.1 In `RainbowPattern.kt`, remove `ParameterDef.IntParam("brightness", ...)` from the parameters list; remove the `private var brightness` field; replace `brightness` usages in `initialize()` and `render*()` with the literal `31`
- [x] 2.2 In `PlasmaPattern.kt`, remove `ParameterDef.IntParam("brightness", ...)` from the parameters list; remove the `private var brightness` field; replace `brightness` usages in `initialize()` and `render()` with the literal `31`

## 3. Backend — SolidColorPattern Color Refactor

- [x] 3.1 In `SolidColorPattern.kt`, replace the parameters list (remove `FloatParam("hue")`, `FloatParam("saturation")`, `FloatParam("value")`, `IntParam("brightness")`); add `ColorParam("color", "Color", "#ff00001f")`
- [x] 3.2 In `SolidColorPattern.initialize()`, replace the HSV-based construction with `color = params.getColor("color", Color.RED)` — the returned `Color` already carries the brightness from the `bb` suffix

## 4. Frontend — Install react-color

- [x] 4.1 In `frontend/`, run `npm install react-color` and `npm install --save-dev @types/react-color`

## 5. Frontend — Color Picker Component

- [x] 5.1 Create `frontend/src/components/ColorPicker.tsx`; accept props `value: string` (a `#RRGGBBbb` string) and `onChange: (value: string) => void`; manage local `mode: 'rgb' | 'hsv'` state and `open: boolean` state
- [x] 5.2 Parse the incoming `value` prop into `{r, g, b, brightness}` on each render; derive HSV from RGB for display in HSV mode using the existing `Color.toHSV()` logic (re-implement in JS: a simple RGB→HSV conversion function)
- [x] 5.3 Render the swatch `<div>` with `background: #RRGGBB`; clicking it toggles open/closed
- [x] 5.4 When open, render the popover containing: hue range slider for visual selection; a two-button mode toggle ("RGB" / "HSV"); input fields for the active mode; a brightness range input (0–31, labeled as percentage); a read-only hex display
- [x] 5.5 In RGB mode render three `<input type="number">` fields (R 0–255, G 0–255, B 0–255); on change, reconstruct the `#RRGGBBbb` string and call `onChange`
- [x] 5.6 In HSV mode render three `<input type="number">` fields (H 0–360, S 0–100, V 0–100); convert to RGB via an `hsvToRgb` utility, reconstruct `#RRGGBBbb`, and call `onChange`
- [x] 5.7 Wire the brightness `<input type="range">` (0–31) to update the `bb` suffix of the current color string and call `onChange`
- [x] 5.8 Render a full-screen backdrop `<div className="color-picker-backdrop">` behind the popover that calls `setOpen(false)` on click

## 6. Frontend — Wire ColorPicker into ParameterControl

- [x] 6.1 In `ParameterControl.tsx`, replace the `param.type === 'color'` branch: import `ColorPicker` and render `<ColorPicker value={colorValue} onChange={(v) => onChange(param.name, v)} />`

## 7. Frontend — CSS

- [x] 7.1 Add to `App.css`: `.color-swatch` (width 36px, height 24px, border-radius 3px, border 1px solid #555, cursor pointer, display inline-block), `.color-picker-container` (position relative, display inline-block), `.color-picker-popover` (position absolute, z-index 100, top 28px, left 0, background #2a2a2a, border 1px solid #444, border-radius 6px, padding 12px, min-width 220px), `.color-picker-backdrop` (position fixed, inset 0, z-index 99), `.color-picker-mode-toggle` (display flex, gap 4px, margin-bottom 8px), `.color-picker-fields` (display grid, grid-template-columns repeat(3, 1fr), gap 6px), `.color-picker-brightness` (margin-top 8px), `.color-picker-hex` (font-family monospace, font-size 11px, color #888, margin-top 6px)

## 8. Verification

- [x] 8.1 Run `./gradlew test` in the project root and confirm all pattern tests pass with the updated parameter structures
- [x] 8.2 Run `npm run build` in `frontend/` and confirm no TypeScript errors
