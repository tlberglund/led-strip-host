## Why

The current "Apply Pattern" button is largely redundant since parameters already auto-apply with a 50ms debounce. Replacing it with **Save** and **New** buttons gives users a meaningful preset workflow: they can capture the current pattern state as a named preset (or overwrite an existing one) and quickly start fresh with a default pattern — making the pattern tab feel like a proper preset editor rather than a one-shot applier.

## What Changes

- Remove the `ApplyPatternButton` component and its `handleApplyPattern` handler
- Add a **Save** button that:
  - If the current working state is a loaded saved preset, overwrites that preset with the current params
  - If not (unsaved state), opens an inline input or modal prompting for a preset name, then creates a new preset
- Add a **New** button that resets the UI to an unsaved state: first pattern alphabetically with all default params
- Update `PatternSelector` to show `<unsaved pattern>` (or similar) as the label when working in unsaved state
- Track "unsaved state" in App-level state — a boolean or null active-preset-id flag already partially exists via `activePresetName`

## Capabilities

### New Capabilities

- `pattern-preset-save`: Save and save-as workflow for the Save button — overwrite existing preset or prompt for a new name
- `pattern-new-button`: New button behavior — reset to first alphabetical pattern with default params and enter unsaved state

### Modified Capabilities

- `saved-patterns-ui`: The controls sidebar UI changes significantly — ApplyPatternButton is removed, Save/New buttons are added, and PatternSelector gains an unsaved-state label

## Impact

- **Frontend only** — no backend API changes needed; existing `/api/saved-patterns` POST/PUT endpoints already support save and update
- Affected files: `App.tsx`, `ApplyPatternButton.tsx` (removed or repurposed), `ControlsSidebar.tsx`, `PatternSelector.tsx`, possibly a new `SavePatternDialog` or inline input component
- No breaking changes to the API or backend
