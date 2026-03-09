## Context

`ParameterDef.ColorParam` is already declared in `Pattern.kt` and mirrored in `frontend/src/types.ts`, but no existing pattern uses it. `ParameterControl.tsx` already has a `color` branch that renders `<input type="color">`. The full color-param path exists in skeleton form; this change activates it.

Brightness is currently a `ParameterDef.IntParam` with range 0–31 in all three patterns. This range is an APA102 hardware artifact (5-bit brightness field). The `Color` data class carries `brightness: Int` as its fourth field alongside `r`, `g`, `b`. Modeling brightness separately from color at the parameter level is misleading — on APA102 hardware, brightness is per-LED and is inseparable from the color sent to that LED. Bundling brightness into the color parameter reflects the hardware reality and removes a confusing extra slider.

## Goals / Non-Goals

**Goals:**
- `SolidColorPattern` exposes one `ColorParam` (replacing three HSV floats + a brightness int)
- `RainbowPattern` and `PlasmaPattern` remove their standalone brightness params; they render at full brightness (31)
- Wire format for color params is `#RRGGBBbb` — RGB hex plus two hex digits for APA102 brightness (0–31)
- `PatternParameters` provides `getColor(key, default): Color` that parses `#RRGGBBbb`
- Frontend renders color params with a picker that supports both **RGB** and **HSV** input modes with a toggle; also includes a brightness slider (mapped 0–31, shown as 0–100%)
- RGB is the canonical wire format; HSV is a UI convenience only

**Non-Goals:**
- Changing `Color`'s internal representation or `Color.fromHSV()` signature
- Adding alpha/opacity
- Applying a `ColorParam` to `RainbowPattern` or `PlasmaPattern` — their color behavior is algorithmic
- Removing hue/saturation/value params from `PlasmaPattern`'s hue-range controls (`hueMin`, `hueMax`, `saturation`, `value`) — those control the plasma algorithm, not a "pick a color" input

## Decisions

### D1: Brightness encoded in the color string (`#RRGGBBbb`)
**Chosen:** Extend the color wire format to `#RRGGBBbb` — 9 characters total (1 `#` + 6 RGB + 2 brightness). The brightness field is `00`–`1f` hex (0–31 decimal). Example: full-brightness red = `"#ff00001f"`, half-brightness blue = `"#0000ff0f"`.

**Why this over separate brightness param:** The brightness value is meaningless without its companion color — it applies at the LED level, not as a global scene control. A single string keeps the color+brightness atomic, prevents the UI from having a detached slider that affects whichever color happens to be active, and keeps `PatternParameters` clean (one param, one value, one key).

**Why not `rgba()`:** The CSS `rgba()` format uses alpha 0–1 and does not map naturally to a 5-bit hardware field. The custom hex format is compact and unambiguous.

**Why not a JSON object `{"hex": "#RRGGBB", "brightness": 0.8}`:** The `PatternParameters` map uses `Any` values; mixing string and object types adds complexity. Keeping it a plain string works with the existing `parseJsonParams` string passthrough in `PreviewServer`.

### D2: HSV and RGB input modes with a toggle in the picker UI
**Chosen:** The color picker popover has a mode toggle (two buttons: "RGB" / "HSV"). In RGB mode, the user sees numeric inputs for R (0–255), G (0–255), B (0–255). In HSV mode, the user sees H (0–360), S (0–100%), V (0–100%). Both modes share a saturation+value gradient square and a hue strip from `react-color` for visual selection. A brightness slider (labeled "Brightness", 0–31 mapped to 0–100%) is always visible regardless of mode. The current hex value (`#RRGGBB`) is shown as a read-only display in both modes.

**Conversion:** When the user edits HSV fields, convert to RGB before updating state and calling `onChange`. The canonical state is always `{r, g, b, brightness}` — HSV is derived on render from the current RGB.

**Why not SketchPicker alone:** `react-color`'s `SketchPicker` provides a saturation/value gradient and hue strip but its input fields default to hex + RGB only, with no HSV inputs. To support HSV entry, we need a custom picker component built on top of `react-color`'s `Saturation`, `Hue`, and `Alpha` sub-components (all exported separately), with our own input fields and mode toggle.

**Why not a third-party HSV-capable picker:** Adding another dependency for a picker widget is unnecessary given `react-color` exports the primitives we need.

### D3: `getColor` helper on `PatternParameters`
**Chosen:** Add `fun getColor(key: String, default: Color): Color` to `PatternParameters`. It parses a `#RRGGBBbb` string: `Integer.parseInt(hex.substring(1, 7), 16)` for RGB, `Integer.parseInt(hex.substring(7, 9), 16)` for brightness. Returns `default` on any missing key, wrong type, or parse error.

**Backward compat:** If the stored string is only 7 chars (`#RRGGBB`, no brightness suffix), default brightness to 31. This keeps compatibility if any code sends bare hex strings.

### D4: Full brightness (31) for algorithmic patterns
`RainbowPattern` and `PlasmaPattern` call `Color.fromHSV(h, s, v, brightness)` internally. After removing the `IntParam`, they pass `brightness = 31` (hardware maximum) directly. The APA102 brightness field in these patterns is not user-tunable — users control the visual intensity via the `value` (HSV V) parameter instead, which operates in the RGB color space and is already present.

## Risks / Trade-offs

- **`#RRGGBBbb` is a non-standard format:** Any external tool that parses color params will see an unfamiliar 9-char hex string. → Acceptable — this is an internal protocol; no external consumers exist.
- **Brightness `1f` hex = 31 but `ff` hex = 255:** The brightness field uses the raw 0–31 range encoded as hex, so valid values are `00`–`1f`. Values `20`–`ff` are out of range. → `getColor` SHALL clamp: `brightness.coerceIn(0, 31)`.
- **Custom picker complexity:** Building on `react-color`'s primitives requires more code than dropping in a prebuilt picker. → The picker is self-contained in one component; complexity is bounded.
- **SolidColorPattern is a breaking API change:** Clients POSTing `hue`/`saturation`/`value` params will silently have no effect. → No persistence mechanism for pattern params exists, so no migration needed.

## Migration Plan

No data migration needed. Pattern params are ephemeral — each POST to `/api/pattern/:name` sends the full current param set from the UI. After the change, the frontend will send `color = "#ff00001f"` instead of separate float fields. Old params sent from a stale browser cache are silently ignored by `PatternParameters.get()`.
