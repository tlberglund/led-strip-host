## Context

The Pattern tab in the frontend has an "Apply Pattern" button (`ApplyPatternButton.tsx`) that manually POSTs the current pattern and params to the backend. This is largely redundant — parameters already auto-apply via a 50ms debounce on every change. The button provides no meaningful workflow value.

The goal is to replace it with two purpose-built buttons: **Save** (to persist the current pattern+params as a named preset) and **New** (to start fresh with a blank, unsaved state). The existing `useSavedPatterns` hook and `/api/saved-patterns` API already support all required backend operations; this is a purely frontend change.

## Goals / Non-Goals

**Goals:**
- Remove `ApplyPatternButton` and its handler
- Add Save button with context-sensitive behavior (overwrite vs. name-and-save)
- Add New button that resets to first-alphabetical pattern with default params in unsaved state
- Show `<unsaved pattern>` label in the pattern dropdown when in unsaved state
- Track unsaved/saved state clearly in App-level state

**Non-Goals:**
- No backend API changes
- No changes to the debounced auto-apply behavior
- No changes to the Saved Patterns panel (LoadSaved/rename/delete remain there)
- No undo/history functionality

## Decisions

### Decision: Use `activePresetId` (not `activePresetName`) to track saved state

**Rationale:** `activePresetName` is already used for the startup-default concept (the preset loaded at boot). It conflates two concerns: "what is currently in the editor" vs. "what will load on next boot." We need a separate piece of state — `activePresetId: number | null` — that tracks whether the current parameter state corresponds to a saved preset. When non-null, the Save button overwrites that preset. When null, Save triggers the name dialog.

**Alternative considered:** Reuse `activePresetName` by treating non-null as "saved." Rejected because loading a preset for editing should not automatically make it the boot default, and the naming conflates too many responsibilities.

### Decision: Inline text input for new-preset naming, not a modal dialog

**Rationale:** A modal is heavier than needed for entering a single name. An inline input that appears in place of (or adjacent to) the Save button keeps the user in context. An empty-name guard disables the confirm action. This matches the existing inline save-as pattern in `SavedPatternsPanel`.

**Alternative considered:** A `<dialog>` modal. Would work, but adds complexity and takes focus away from the pattern controls.

### Decision: "First alphabetical" for New button — use index 0 of the sorted patterns list

**Rationale:** The backend already returns patterns in alphabetical order (`PatternRegistry.listPatterns()` uses `.sorted()`). The frontend receives them in that order. Using `patterns[0]` is deterministic and requires no additional sorting logic.

### Decision: `<unsaved pattern>` as the PatternSelector label in unsaved state

**Rationale:** Clear and unambiguous. Rendered as a disabled/placeholder-styled option (or as a non-selectable header) so the user understands they are not viewing a saved preset. The actual pattern name is still shown in the parameter area.

**Alternative considered:** Show the pattern name with an asterisk (e.g., "Plasma *"). Rejected — asterisks are subtle; the unsaved-state concept deserves its own clear indicator.

### Decision: Save overwrites without additional confirmation when a preset is loaded

**Rationale:** The user loaded a preset explicitly; Save is a natural "commit changes" action. Adding a confirmation step would be annoying for iterative parameter tweaking. If they want a new preset, they use New first.

## Risks / Trade-offs

- **Risk:** User edits a loaded preset, then presses New accidentally, losing the in-progress edits. → Mitigation: The New button should not auto-save; the user can simply re-load the preset from the Saved Patterns panel. Document this in UI (tooltip on New button: "Start a new unsaved pattern").
- **Risk:** Inline name input overlapping with button layout in narrow sidebars. → Mitigation: Use a slide-in expand or a small popover anchored to the Save button rather than reflowing the button row.
- **Trade-off:** `activePresetId` state is reset to null whenever the user changes the pattern or any parameter after loading a preset — this would mean the Save button immediately switches to "save-as" mode after one tweak. This is undesirable; instead, `activePresetId` should only clear on explicit New or manual pattern selection, not on parameter edits.
