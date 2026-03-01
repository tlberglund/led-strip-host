## 1. Backend — Configuration

- [x] 1.1 Add `scanIntervalSeconds` field to `Configuration` data class with default value of 15
- [x] 1.2 Add `scanIntervalSeconds` entry to `config.yaml` with value 15

## 2. Backend — Discovery Service

- [x] 2.1 Define `StripDiscoveryEvent` sealed class with subtypes: `NewDeviceFound(name, address)`, `ScanCompleted(found: Int)`, `ScanError(message: String)`
- [x] 2.2 Add `MutableSharedFlow<StripDiscoveryEvent>` to `BluetoothTester` and expose it as a public `val discoveryEvents: SharedFlow`
- [x] 2.3 Add `startBackgroundScanning(scope: CoroutineScope, intervalMs: Long)` method to `BluetoothTester` that launches a coroutine loop using `Dispatchers.IO`
- [x] 2.4 Implement loop body: emit `ScanCompleted` or `NewDeviceFound` events, merge new devices into `discoveredDevices` without overwriting existing entries, use 5000 ms scan timeout
- [x] 2.5 Wire `startBackgroundScanning` call in `Application.kt`, passing the app's coroutine scope and `configuration.scanIntervalSeconds * 1000L`

## 3. Backend — WebSocket Endpoint

- [x] 3.1 Add a `StripsBroadcaster` (or reuse a similar pattern to `WebSocketBroadcaster`) that tracks `/ws/strips` clients and can send JSON text frames
- [x] 3.2 Define serializable message data classes: `StripsUpdateMessage(type="strips_update", strips: List<StripStatusResponse>)` and `DiscoveryEventMessage(type="discovery_event", message: String)`
- [x] 3.3 Add `/ws/strips` WebSocket route in `PreviewServer`; on client connect, immediately send a `strips_update` snapshot of current strip list
- [x] 3.4 In `PreviewServer`, collect `bleManager.discoveryEvents` flow and broadcast a `discovery_event` message to all connected `/ws/strips` clients for each emission
- [x] 3.5 After each `NewDeviceFound` event, also broadcast a full `strips_update` snapshot so clients see the updated list

## 4. Frontend — Types

- [x] 4.1 Add `StripsWsMessage` union type to `types.ts`: `StripsUpdateMessage { type: 'strips_update'; strips: StripStatus[] }` and `DiscoveryEventMessage { type: 'discovery_event'; message: string }`
- [x] 4.2 Add `ActivityLogEntry { timestamp: string; message: string }` interface to `types.ts`

## 5. Frontend — WebSocket Hook

- [x] 5.1 Create `frontend/src/hooks/useStripsWebSocket.ts` that opens a WebSocket to `/ws/strips` when active and closes it when inactive
- [x] 5.2 Implement reconnect logic with exponential backoff (initial 1 s, max 30 s) in `useStripsWebSocket.ts`
- [x] 5.3 Parse incoming messages: update `strips` state on `strips_update`, prepend to `activityLog` (capped at 50 entries) on `discovery_event`
- [x] 5.4 Expose `{ strips, activityLog, isScanning, isConnected }` from the hook; derive `isScanning` from whether the latest activity log entry begins with "Scanning"

## 6. Frontend — Strips Tab UI

- [x] 6.1 Rewrite `StripManagerTab.tsx` to use `useStripsWebSocket` instead of `useStrips`
- [x] 6.2 Render a strip row for each strip: name, BLE address, LED count, connection badge
- [x] 6.3 Show only a "Disconnect" button when `strip.connected === true`; show only a "Reconnect" button when `strip.connected === false`
- [x] 6.4 On Disconnect click: call `POST /api/strips/{id}/disconnect`, optimistically set local badge to "Disconnected"
- [x] 6.5 On Reconnect click: call `POST /api/strips/{id}/connect`, optimistically set local badge to "Connecting…"
- [x] 6.6 Render activity log section below strip list: show "Waiting for discovery events…" when empty, otherwise a list of timestamped entries prepended as they arrive
- [x] 6.7 Show a scanning indicator (spinner or label) in the tab when `isScanning` is true
- [x] 6.8 Show "No strip controllers discovered." empty state when strip list is empty
- [x] 6.9 Show "Reconnecting…" status indicator when WebSocket connection is lost and backoff is active

## 8. Auto-Reconnect

- [x] 8.1 Add `ReconnectAttempted(id: Int, name: String)`, `ReconnectSucceeded(id: Int, name: String)`, and `ReconnectFailed(id: Int, name: String, reason: String)` subtypes to `StripDiscoveryEvent`
- [x] 8.2 In the `startBackgroundScanning` loop body, after processing new devices, iterate over `discoveredDevices` entries where the known client is absent or `isConnected == false` and call `connectStrip(id)` for each
- [x] 8.3 Emit `ReconnectAttempted` before each reconnect call and `ReconnectSucceeded` or `ReconnectFailed` depending on the result
- [x] 8.4 In `PreviewServer`'s discovery event collector, add `when` branches for the three new event subtypes: format human-readable messages and broadcast `strips_update` after every `ReconnectSucceeded` or `ReconnectFailed`

## 7. Cleanup

- [x] 7.1 Remove the old `useStrips.ts` hook if it is no longer referenced anywhere
- [x] 7.2 Verify existing REST endpoints (`GET /api/strips`, `POST /api/strips/{id}/connect`, `POST /api/strips/{id}/disconnect`) still work correctly and are unchanged
