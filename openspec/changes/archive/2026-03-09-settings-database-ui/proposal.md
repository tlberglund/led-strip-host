## Why

The current YAML config file is a deployment artifact that operators must edit by hand and redeploy to change anything — strip layouts, viewport size, background image, FPS. Moving configuration into a database with a dedicated Settings UI makes the system reconfigurable at runtime without restarting the server or touching the filesystem.

## What Changes

- **BREAKING**: All configuration except `webServer.port` (and `targetFPS` as a reasonable fallback) moves out of `config.yaml` and into a PostgreSQL database loaded at startup and kept in sync at runtime
- Add a **Settings tab** to the web UI with sections for:
  - Viewport dimensions and target FPS
  - Background image (drag-and-drop or file picker upload; stored as a blob in the database, cached on disk for serving)
  - Strip management — create, edit, and delete strips (BT name, LED count, start/end X,Y coordinates)
- Existing REST endpoints that read from `Configuration` are updated to read from the database layer instead
- The `config.yaml` schema shrinks to only `webServer.port`; all other fields become optional/ignored with a deprecation log warning if present
- The background image endpoint switches from reading a filesystem path to serving the cached file (or streaming from DB if cache is cold)

## Capabilities

### New Capabilities

- `settings-database`: PostgreSQL-backed settings store that holds viewport config, strip layouts, FPS, and background image blob; exposes a Kotlin API for CRUD operations and change notifications
- `settings-api`: REST endpoints for reading and writing all settings categories (viewport, strips, background image upload/delete)
- `settings-ui`: Settings tab in the React frontend with sections for viewport/FPS, background image upload (drag-and-drop + file picker), and strip management (add/edit/delete rows)

### Modified Capabilities

- `strip-discovery-service`: Strip registry must now seed from the database instead of the YAML strip list; newly created strips in the UI are persisted to the database and picked up by the scanner on the next scan cycle

## Impact

- **Backend**: New `SettingsRepository` class (PostgreSQL via Exposed ORM + PostgreSQL JDBC driver); `Configuration` data class becomes minimal; `PreviewServer` routes gain `/api/settings/**` and `/api/settings/background-image` (POST); `Application.kt` bootstraps the DB connection before starting the server; `BluetoothTester` receives strip list from DB instead of config
- **Frontend**: New `SettingsTab` component and child components; new hooks for settings CRUD; `useBackgroundImage` hook updated to use the existing GET endpoint (no change to the URL, only the backend source changes)
- **Dependencies**: PostgreSQL JDBC driver + Exposed ORM added to `build.gradle.kts`; a `docker-compose.yml` provided for running a local PostgreSQL instance during development
- **Migration**: On first run with an empty database, defaults are seeded from any existing `config.yaml` fields (if present) so existing deployments don't lose their configuration
