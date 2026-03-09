## Context

Configuration is currently loaded once from `config.yaml` at startup and is immutable for the life of the process. Changing anything — strip layout, viewport size, background image path — requires editing a file and restarting the server. The file also contains a filesystem path for the background image, meaning images must be manually placed on the server's filesystem.

The goal is a fully in-process, runtime-configurable settings store backed by PostgreSQL, surfaced through a Settings tab in the React UI. Port number stays in `config.yaml` because it is needed before the web server starts.

## Goals / Non-Goals

**Goals:**
- PostgreSQL database holds all settings (viewport, FPS, strip layouts, background image blob, scan interval)
- REST API for reading and writing each settings category
- Settings tab UI: viewport/FPS section, background image upload (drag-and-drop + picker), strip management (add/edit/delete)
- On first run, seed DB from existing `config.yaml` fields if present (zero-downtime migration for existing deployments)
- Background image served from a filesystem cache; DB bytea column is the source of truth
- `docker-compose.yml` provided for spinning up a local PostgreSQL instance during development

**Non-Goals:**
- Saved pattern presets (deferred to a future feature)
- Multi-user auth or per-user settings
- Hot-reload of `webServer.port` without restart
- Real-time push of settings changes to other connected browser tabs

## Decisions

### 1 — PostgreSQL via Exposed ORM

**Decision**: Use [Exposed](https://github.com/JetBrains/Exposed) (Kotlin SQL framework) with the PostgreSQL JDBC driver (`org.postgresql:postgresql`).

**Rationale**: PostgreSQL is production-grade, handles concurrent connections well, and its `bytea` type is a natural fit for storing image blobs without the file-size limitations that SQLite's blob handling can introduce. Exposed provides type-safe table definitions and a lightweight DAO layer that fits naturally in a Kotlin/coroutines codebase. The Docker Compose file makes local development frictionless.

**Alternatives considered**: SQLite — simpler deployment but single-writer limitation and weaker typing; less suitable if the app later runs with multiple processes or remote clients. H2 — embedded and useful for tests but not a common production choice. Raw JDBC — more boilerplate, no benefit at this scale.

### 2 — Single `settings` key-value table + dedicated `strips` table + `background_image` table

**Decision**: Three tables:
- `settings(key VARCHAR PRIMARY KEY, value TEXT)` — scalar settings (viewport width/height, FPS, scan interval)
- `strips(id SERIAL PRIMARY KEY, bt_name VARCHAR NOT NULL, length INT, start_x INT, start_y INT, end_x INT, end_y INT, reverse BOOLEAN DEFAULT false)`
- `background_image(id INT PRIMARY KEY DEFAULT 1, data BYTEA, mime_type VARCHAR, updated_at BIGINT)`

**Rationale**: Key-value for scalars avoids schema churn when adding new settings. Dedicated `strips` table allows clean CRUD. Separate image table with a fixed `id=1` row prevents the blob from bloating settings reads.

**Alternatives considered**: Single JSON blob — loses ability to query/update individual strip rows. Storing image as base64 in the key-value table — bloats all settings reads.

### 3 — Filesystem cache for background image

**Decision**: On image upload, write the blob to a cache file at a configurable path (default: `./bg-image-cache.<ext>`). `GET /api/background-image` serves the cached file via `respondFile`. On server start, if the DB has an image but the cache file is absent, regenerate it from the DB.

**Rationale**: Serving a file is far more efficient than streaming a bytea column from PostgreSQL on every request. Cache invalidation is trivial — one file, replaced atomically on each upload.

### 4 — `SettingsRepository` as the single source of truth

**Decision**: Introduce `SettingsRepository` (a plain Kotlin class) that owns the Exposed database connection, exposes suspend functions for all CRUD operations, and is injected into `PreviewServer` and `Application`. `Configuration` is reduced to `MinimalConfig(port: Int, databaseUrl: String, databaseUser: String, databasePassword: String)` (YAML-deserialized).

**Rationale**: A single owner of the DB connection prevents leaks. Inject-everywhere avoids global state. Connection parameters must stay in `config.yaml` since the DB must be reachable before any settings can be read.

### 5 — Docker Compose for local development

**Decision**: Provide a `docker-compose.yml` at the project root that starts a `postgres:16-alpine` container with a named volume, default credentials matching the `config.yaml` defaults, and a health check.

**Rationale**: Developers shouldn't need a local PostgreSQL install to work on the project. The compose file is the canonical "spin up dependencies" command and doubles as documentation for the required DB configuration.

### 6 — Settings UI built with `frontend-design` skill

**Decision**: The Settings tab UI will be built using the `frontend-design` skill for high-quality component design, consistent with the existing app aesthetic.

**Rationale**: The Settings tab is user-facing and benefits from polished design — section cards, a drag-and-drop image upload zone, and an inline-editable strip table.

## Risks / Trade-offs

- **PostgreSQL availability**: Unlike SQLite, Postgres is an external process. If the DB is unreachable at startup, the app fails to start. → Mitigation: Clear error message with connection details; the Docker Compose health check prevents the app from starting before Postgres is ready if using compose.
- **Image cache staleness on crash**: If the server crashes mid-upload, the cache file may be stale or missing. → Mitigation: Regenerate cache from DB on startup if file is missing or `updated_at` is newer than the file's mtime.
- **Strip deletion while scanner holds a BLE connection**: Deleting a strip from the DB while a BLE client is connected. → Mitigation: The DELETE endpoint disconnects the BLE client before removing the DB row.
- **Seeding idempotency**: If `config.yaml` has strip IDs that conflict with DB rows, seeding must not duplicate. → Mitigation: Only seed if the `strips` table is empty.

## Migration Plan

1. Run `docker compose up -d` (or point `config.yaml` at an existing Postgres instance)
2. Deploy updated server binary
3. On first startup, server detects empty DB and seeds from `config.yaml` if present; logs a warning that config fields (except port and DB connection) are now ignored
4. Rollback: revert binary; re-add full fields to `config.yaml`; the DB is left in place but unused

## Open Questions

- Database file location: `config.yaml` will carry `databaseUrl`, `databaseUser`, `databasePassword`. Default URL: `jdbc:postgresql://localhost:5432/ledstrip`. Confirm this is acceptable for the Pi deployment.
- Image size limit for upload: proposed 10 MB. Adjust?
