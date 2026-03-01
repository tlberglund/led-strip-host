## Why

The Strips tab currently shows a static snapshot of discovered devices, requiring a manual page poll to detect newly-connected or rebooted strips. Operators need real-time awareness of what devices are online and what the system is actively doing to find them — especially during startup or after a strip reboots mid-session.

## What Changes

- Add a persistent background coroutine (backend) that continuously scans for new BLE strip controllers and updates the discovered-device registry
- Add a WebSocket channel (or server-sent event stream) that pushes strip-list updates and live discovery activity messages to the frontend
- Upgrade the Strips tab to consume real-time push updates instead of polling, displaying current strip status and a scrolling activity log of recent discovery events
- Add explicit Disconnect / Reconnect buttons for each strip (distinct actions, replacing the current single toggle button)

## Capabilities

### New Capabilities

- `strip-discovery-service`: Background coroutine that continuously scans for BLE strip devices, maintains discovered-device state, and emits events when new devices are found or when connection state changes
- `strips-realtime-tab`: Frontend Strips tab that subscribes to a WebSocket endpoint for real-time strip-list updates and a live discovery activity feed, with per-strip Disconnect and Reconnect action buttons

### Modified Capabilities

<!-- none — no existing specs -->

## Impact

- **Backend**: `BluetoothTester` gains a background scan loop (coroutine); `PreviewServer` adds a new WebSocket endpoint (`/strips`) for strip events; `Application.kt` wires the coroutine lifecycle
- **Frontend**: `useStrips.ts` hook replaced with a WebSocket-backed hook; `StripManagerTab.tsx` redesigned to show live data and an activity feed; `types.ts` extended with new message types
- **No breaking API changes**: existing REST endpoints (`GET /api/strips`, `POST /api/strips/{id}/connect`, `POST /api/strips/{id}/disconnect`) are preserved
