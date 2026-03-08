## 1. Infrastructure & Dependencies

- [x] 1.1 Add `docker-compose.yml` at the project root with a `postgres:16-alpine` service, named volume, default credentials (`ledstrip`/`ledstrip`/`ledstrip`), port 5432, and a `pg_isready` health check
- [x] 1.2 Add `org.postgresql:postgresql` JDBC driver and `org.jetbrains.exposed:exposed-core`, `exposed-dao`, `exposed-jdbc` dependencies to `build.gradle.kts`
- [x] 1.3 Add `databaseUrl`, `databaseUser`, and `databasePassword` fields to `Configuration` with defaults matching the Docker Compose credentials; remove all non-port fields from the required set (make them optional with defaults)

## 2. Backend — Settings Repository

- [x] 2.1 Create `src/main/kotlin/com/timberglund/ledhost/db/SettingsRepository.kt` with Exposed table definitions: `SettingsTable(key, value)`, `StripsTable(id, bt_name, length, start_x, start_y, end_x, end_y, reverse)`, `BackgroundImageTable(id, data, mime_type, updated_at)`
- [x] 2.2 Implement `SettingsRepository.connect(url, user, password)` that establishes the Exposed `Database` connection and runs `SchemaUtils.createMissingTablesAndColumns()`
- [x] 2.3 Implement `getSetting(key): String?` and `setSetting(key, value)` using `INSERT ... ON CONFLICT DO UPDATE`
- [x] 2.4 Implement `getAllStrips()`, `createStrip(...)`, `updateStrip(id, ...)`, and `deleteStrip(id)` CRUD operations
- [x] 2.5 Implement `setBackgroundImage(bytes, mimeType)` (upsert row + write cache file), `getBackgroundImage(): Pair<ByteArray, String>?`, and `deleteBackgroundImage()` (delete row + delete cache file)
- [x] 2.6 Implement cache regeneration logic: on `connect()`, if DB has a background image but the cache file is absent or older than `updated_at`, write the cache file
- [x] 2.7 Implement `seedFromConfig(config: Configuration)`: if both `settings` and `strips` tables are empty, insert rows from config fields and log a deprecation warning

## 3. Backend — Wire Repository into Application

- [x] 3.1 In `Application.kt`, instantiate `SettingsRepository`, call `connect()`, then call `seedFromConfig(config)` before constructing `PreviewServer` or `BluetoothTester`
- [x] 3.2 Pass `SettingsRepository` to `PreviewServer` constructor; remove `configuration: Configuration` usage from routes that now read from the repository (viewport, strips list, background image)
- [x] 3.3 Load strip list from `SettingsRepository.getAllStrips()` at startup and pass to `BluetoothTester` instead of `config.strips`; update `BluetoothTester` to accept an initial strip list and seed `discoveredDevices` from it
- [x] 3.4 Read `scanIntervalSeconds` from `SettingsRepository.getSetting("scanIntervalSeconds")` (with default 15) instead of `config.scanIntervalSeconds`

## 4. Backend — Settings API Routes

- [x] 4.1 Add `GET /api/settings` route returning JSON of all scalar settings with defaults
- [x] 4.2 Add `PUT /api/settings` route accepting partial JSON body, validating values (positive integers), and calling `setSetting` for each provided key
- [x] 4.3 Add `GET /api/settings/strips` route returning JSON array from `getAllStrips()`
- [x] 4.4 Add `POST /api/settings/strips` route with body validation (btName and length required) calling `createStrip()`, returning `201` with the new row
- [x] 4.5 Add `PUT /api/settings/strips/{id}` route calling `updateStrip()`, returning `404` if not found
- [x] 4.6 Add `DELETE /api/settings/strips/{id}` route that disconnects any active BLE client for that strip, calls `deleteStrip()`, returns `204` or `404`
- [x] 4.7 Add `POST /api/settings/background-image` multipart route: enforce 10 MB limit, call `setBackgroundImage()`, return `200`
- [x] 4.8 Add `DELETE /api/settings/background-image` route calling `deleteBackgroundImage()`, returning `204` or `404`
- [x] 4.9 Update `GET /api/background-image` to serve the cache file path from `SettingsRepository` instead of `configuration.backgroundImage`

## 5. Frontend — Types & Hooks

- [x] 5.1 Add TypeScript interfaces to `types.ts`: `ScalarSettings`, `StripSetting` (with `id`, `btName`, `length`, `startX`, `startY`, `endX`, `endY`, `reverse`), `StripSettingInput` (without `id`)
- [x] 5.2 Create `frontend/src/hooks/useSettings.ts` — fetches `GET /api/settings`, exposes `settings` state and a `saveSettings(partial)` function that calls `PUT /api/settings`
- [x] 5.3 Create `frontend/src/hooks/useStripSettings.ts` — fetches `GET /api/settings/strips`, exposes `strips`, `addStrip`, `updateStrip`, `deleteStrip` functions

## 6. Frontend — Settings Tab UI

- [x] 6.1 Add `'settings'` to the `Tab` union type in `App.tsx` and add the "Settings" button to the top nav tab list
- [x] 6.2 Create `frontend/src/components/SettingsTab.tsx` as the root settings component, rendered when `activeTab === 'settings'`; use the `frontend-design` skill to produce a polished layout with section cards
- [x] 6.3 Implement the "Viewport & Performance" section card: numeric inputs for width, height, and FPS populated from `useSettings`; Save button calls `saveSettings`; inline validation disables Save on non-positive values
- [x] 6.4 Implement the "Background Image" section card: drag-and-drop zone + file picker button; thumbnail preview when image is set; Remove button; upload calls `POST /api/settings/background-image`; rejection of non-image files
- [x] 6.5 Implement the "Strip Controllers" section card: table of strips from `useStripSettings`; inline edit mode per row; Delete with confirmation dialog; Add Strip form below the table with BT Name, Length, Start X/Y, End X/Y inputs
- [x] 6.6 Implement save/error feedback: green success or red error message displayed near each section after each operation, auto-cleared after 4 seconds

## 7. Cleanup & Migration

- [x] 7.1 Remove now-unused fields from `Configuration` data class (`strips`, `mapper`, `backgroundImage`, `scanIntervalSeconds`, `targetFPS` — keep `webServer.port` and new DB connection fields); update `config.yaml` accordingly
- [x] 7.2 Remove `createDefaultConfiguration()` from `Application.kt` or update it to only set port and DB defaults
- [x] 7.3 Update `PreviewServerTest.kt` setup to provide the minimal `Configuration` and a test `SettingsRepository` (or mock) instead of the full YAML-loaded config
