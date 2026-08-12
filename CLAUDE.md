# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

checkpointd is a video game backlog/library app (Letterboxd/Backloggd-style) with a Java/Spring Boot backend (`checkpointd-api/`) and a React/TypeScript/Vite frontend (`checkpointd-web/`), plus production deployment config at the repo root. It's a monorepo with backend and frontend kept strictly separated.

Deployed live at `checkpointd.fun` on a self-managed VPS (Docker Compose + Caddy), not a managed PaaS.

## Commands

### Backend (`checkpointd-api/`)

```powershell
cd checkpointd-api
$env:JWT_SECRET="replace-with-local-development-secret"
$env:IGDB_CLIENT_ID="replace-with-client-id"
$env:IGDB_CLIENT_SECRET="replace-with-client-secret"
$env:IGDB_BASE_URL="https://api.igdb.com/v4"
$env:TWITCH_TOKEN_URL="https://id.twitch.tv/oauth2/token"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Runs on `http://localhost:8080`. Spring Boot does **not** read the root `.env` file when launched via Maven — env vars must be set in the shell (or IDE run config). IGDB vars are only needed for external search/import; if missing, those endpoints return `503` while the rest of the API still works.

Tests: `.\mvnw.cmd clean test` (from `checkpointd-api/`). Single test class: `.\mvnw.cmd test -Dtest=ClassName`. Single method: `.\mvnw.cmd test -Dtest=ClassName#methodName`.

### Frontend (`checkpointd-web/`)

```powershell
cd checkpointd-web
npm install
npm run dev      # http://localhost:5173
npm run build    # tsc --noEmit (x2 configs) + vite build
```

No frontend test suite exists currently — verification is `npm run build` (typecheck + production build) plus manual smoke testing.

### Local Postgres

```powershell
docker compose up -d db      # starts Postgres only, from repo root
docker compose down          # stop without deleting the volume — never use `-v` unless you mean to wipe local data
```

## Architecture

### Backend: package-per-domain-module

`checkpointd-api/src/main/java/com/chmz31/checkpointd/` has one top-level package per domain — `auth`, `user`, `game`, `externalgames`, `library`, `profile`, `review`, `list`, `follow`, `like`, `comment`, `notification`, plus `common` (cross-cutting: `config` incl. `SecurityConfig`, `dto`, `exception`). Each domain module follows `entity/ repository/ service/ controller/ dto/` internally.

There's a deliberate **no-cycles discipline** between modules — before adding a dependency from module A to module B, check whether B already depends on A (directly or transitively) to avoid introducing a cycle. This has driven real design decisions, e.g. comment-like entities live inside `comment/` rather than `like/`, specifically to avoid a `list → like → comment → list` cycle (since `comment.entity` already depends on `list.entity`, and `list.service` already depends on `like.repository`).

**Entity modeling pattern**: dedicated entity pairs per target type are the default (`ListLike`/`ReviewLike`, `ListComment`/`ReviewComment`) rather than generic/polymorphic tables — consistent with the rest of the schema. The one deliberate exception is `notification/entity/Notification.java`, a single generic entity with real nullable FKs to `game_lists` and `reviews` (`ON DELETE CASCADE`), justified by 8 notification types that only ever resolve to one of two link shapes; duplicating a table per event type would have been excessive there.

**Auth**: JWT (HS256) via Spring Security's OAuth2 resource server support, not sessions. There is **no `hasRole()`/`JwtAuthenticationConverter`-based role gating** — `SecurityConfig` only distinguishes `authenticated()` vs `permitAll()` per route; anything admin-only reads the JWT's `role` claim manually inside the controller/service (`jwt.getClaimAsString("role")`). Personal/authenticated routes generally don't need route-specific config — they fall under the `/api/v1/**` → `authenticated()` catch-all unless something needs `permitAll()` (see the explicit `permitAll()` list in `SecurityConfig` for the public read endpoints — public profiles, popular lists, review/comment reads, etc.).

**DB**: PostgreSQL + Flyway, sequential `V{n}__description.sql` migrations in `checkpointd-api/src/main/resources/db/migration/` (currently up to V17). Migrations run automatically on API startup in every environment (dev and prod) — no manual migration step, ever.

**Config profiles**: `application.yml` is fully env-var-driven with no risky defaults (safe to run in prod as-is). `application-dev.yml` layers in localhost-friendly fallbacks (default DB connection string, permissive CORS) — this profile must never be active in production.

**Tests**: Mockito unit tests for services (`@ExtendWith(MockitoExtension.class)`, `@Mock`/`@InjectMocks`). Full-context `@SpringBootTest @AutoConfigureMockMvc` MockMvc tests for controllers — these require **every** repository in the whole app mocked via `@MockitoBean` (adding any new repository means adding a `@MockitoBean` to every existing controller test class, mechanical but easy to forget).

### Frontend

React + TypeScript + Vite + React Router v6. `src/api.ts` has a single `apiRequest<T>` fetch wrapper all API calls go through; `API_BASE_URL` comes from `import.meta.env.VITE_API_BASE_URL` (empty string in production, meaning same-origin — use `??` not `||` when touching this, since an intentional empty string is falsy and `||` will silently break it). Route groups in `App.tsx` split into a public shell (`PublicShell`) and an authenticated `AppShell` wrapping `<Outlet/>`; `AppShell` persists across in-group navigation, which is why things like the notification-count polling `useEffect` live there.

**UI convention**: no dropdowns/popovers anywhere in the app — everything is full-page navigation. Paginated lists follow a consistent fetch-on-mount + `pagination-controls` pattern. Keep new UI consistent with this rather than introducing modal/popover patterns.

`src/routePaths.ts` centralizes route-building helpers (don't hand-build URL strings elsewhere). `src/types.ts` mirrors backend DTOs/enums by hand (no codegen) — keep them in sync manually when backend response shapes change.

### Production deployment

`docker-compose.prod.yml` (repo root) runs three containers: `db` (Postgres), `api` (Spring Boot, built from `checkpointd-api/Dockerfile`), and `web` (Caddy, built from `checkpointd-web/Dockerfile`) — Caddy serves the built frontend directly and reverse-proxies `/api/*` to `api`, so frontend and API share one origin (no CORS needed in the browser) and Caddy is the only container with published ports. Caddy also handles TLS automatically via Let's Encrypt (`checkpointd-web/Caddyfile`) — no nginx/certbot.

Redeploy: `deploy/deploy.sh` (run on the VPS from `/opt/checkpointd`) — `git pull`, rebuild images, `docker compose up -d`, prune. `deploy/backup-db.sh` runs daily via cron, dumping Postgres to `/var/backups/checkpointd/` with 7-day local retention.

`.env.prod` (untracked, lives only on the VPS) holds real secrets; `.env.prod.example` is the placeholder template. Never reuse the local dev `JWT_SECRET` in prod.

## Scope discipline (from AGENTS.md)

- Stay within the requested scope — don't add product features, controllers, services, entities, migrations, frontend screens, or CI workflows unless explicitly asked.
- Don't change the planned stack versions (Java 25, Spring Boot 4.1.x, Maven, PostgreSQL 18, Flyway, Spring Security + JWT; React/TypeScript/Vite on the frontend) without permission.
- Don't introduce real secrets or create a committed `.env`; `.env.example`-style files get placeholder values only.
- Don't hardcode `localhost` in application code — use configuration/environment variables (the frontend's `API_BASE_URL` fallback is the one legitimate exception, and only as a fallback for the unset case, not the primary path).
