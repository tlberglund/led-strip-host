## Why

The LED strip host needs a wider variety of patterns to showcase the capabilities of the hardware. A simple alternating-bars pattern provides an accessible, visually striking effect that users can easily tune to their environment, and serves as a foundation for understanding how angle and speed interact with the LED coordinate system.

## What Changes

- New `AlternatingBarsPattern` Kotlin class implementing the pattern engine interface
- Pattern registered and available in the frontend pattern selector
- Frontend UI card for the pattern with controls: two color pickers, bar width (mm), speed, and angle (0–360°)

## Capabilities

### New Capabilities
- `alternating-bars-pattern`: A scrolling/animated pattern that renders alternating solid-color bars across the LED strip, with configurable colors, bar width in mm, scroll speed, and bar angle.

### Modified Capabilities

## Impact

- New Kotlin pattern class under the existing pattern package
- New React frontend component for the pattern's parameter UI (following the existing pattern-color-params UI convention)
- Pattern must be registered in whatever pattern registry/factory exists in the backend
- No API changes; uses existing pattern parameter passing mechanism
