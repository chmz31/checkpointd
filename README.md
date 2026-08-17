# checkpointd

checkpointd - your save file for every game you play.

checkpointd is a video game backlog and social library app inspired by Letterboxd, IMDb, Backloggd, and GameTrack. It lets players search for games, import game metadata into a local catalog, track a personal library with status/rating/notes, write reviews, curate public lists, follow other players, and discuss games through comments — all on top of a JWT-authenticated Spring Boot API and a React frontend.

Live at [checkpointd.fun](https://checkpointd.fun).

## Features

**Library and games**

- Register and login with JWT authentication; self-service account deletion (password-confirmed, cascades all owned data).
- Email verification on registration (via Resend) — banner-only, non-blocking: register/login stay instant, an unverified user just sees a dismissible reminder with a resend button.
- Search external games through the backend via IGDB, filtered to real standalone games (base games, remakes, remasters, expanded editions, ports) rather than DLC/expansions/bundles/mods.
- Import external games into a local catalog with metadata: summary, genres, platforms, screenshots, artworks, and a backdrop image.
- Track a personal library with status, notes, and per-entry tracking dates; filter, sort, and search the library; re-sync metadata for individual entries.
- Dedicated library entry and public game detail pages with metadata, media, and tracking facts. Game detail pages are public — reachable logged out, so shared links and search results don't dead-end at a login wall.

**Social**

- Write and edit reviews (rating + text, spoiler flag, public/private visibility) per game; browse a game's reviews or a user's reviews, sortable by newest/oldest/highest/lowest rated.
- Create and curate public or private game lists; browse popular lists and a specific user's lists.
- Follow other users; view followers/following lists.
- Like reviews and lists.
- Comment on reviews and lists, with one level of threaded replies and comment likes; report comments for moderation; admins can review and delete reported comments.
- In-app notifications (follows, likes, comments, replies) with an unread badge, delivered via polling.
- Public profiles (bio, stats, recent games/reviews) with public/private visibility.
- Multi-category search — Games, Lists, and Members — from one search page.
- Static Privacy and About pages, linked from the footer on every page.

Metadata appears automatically for newly imported/cached games. Existing cached games are not backfilled automatically, but "Sync metadata" refreshes an individual IGDB-backed library entry on demand.

## Tech Stack

Backend:

- Java 25
- Spring Boot 4.1.0
- Spring Security (JWT via OAuth2 resource server support)
- Spring Data JPA
- PostgreSQL 18
- Flyway (schema currently at `V18`)
- Maven Wrapper

Frontend:

- React 19
- TypeScript
- Vite 7
- React Router 7
- No external UI/state-management/HTTP-client libraries — plain CSS, local component state, and a hand-rolled `fetch`-based API client (`src/api.ts`)
- npm

Integrations and automation:

- IGDB through Twitch client credentials from the backend only
- GitHub Actions backend CI
- GitHub Actions frontend CI

Production:

- Docker Compose (Postgres, API, and Caddy as reverse proxy + static file server)
- Caddy for automatic HTTPS via Let's Encrypt

## Monorepo Structure

```text
checkpointd/
  checkpointd-api/      Spring Boot backend API
  checkpointd-web/      React + TypeScript + Vite frontend
  docs/                 Architecture notes and decisions
  deploy/               Production deploy + backup scripts
  .github/workflows/    Backend and frontend CI workflows
  docker-compose.yml    Local PostgreSQL
  docker-compose.prod.yml   Production stack (Postgres, API, Caddy)
```

### Backend domain modules

`checkpointd-api` is organized package-per-domain under `com.chmz31.checkpointd`, with a deliberate no-cycles discipline between modules:

| Module | Owns |
|---|---|
| `auth` | Registration, login, JWT issuance, current-user identity, account deletion, email verification |
| `user` | Core `User` entity and repository (no controller of its own — exposed via `auth`/`profile`) |
| `game` | Canonical local `Game` entity — creation, lookup, listing, metadata staleness |
| `externalgames` | IGDB search and import-to-local-`Game` |
| `library` | Per-user library entries — status, tracking dates, stats, metadata sync |
| `profile` | Public/own profile data, profile editing, member search |
| `review` | Per-user, per-game reviews (upsert), public/own listings, sorting |
| `list` | User-curated game lists — CRUD, items, search, popularity |
| `follow` | User-to-user follow relationships, followers/following listings |
| `like` | Likes on lists and reviews (comment likes live under `comment`) |
| `comment` | Comments on lists/reviews (with replies), comment likes, reporting, admin moderation |
| `notification` | In-app notifications, unread counts, read-state |
| `common` | Cross-cutting: security config, async/HTTP client config, shared `PaginatedResponse`, global exception handling |

## Local Development Requirements

- Java 25
- Docker Desktop or Docker Engine with Compose
- Node.js 24 or a compatible current Node.js version
- npm
- PowerShell on Windows for the commands below

## Environment Variables

Create the root environment file from the safe example:

```powershell
Copy-Item .env.example .env
```

Docker Compose reads the root `.env` file for PostgreSQL settings and port mappings. Spring Boot does not automatically read the root `.env` file when launched through Maven; set required backend variables in your shell or IDE run configuration.

Important root/backend variables:

- `POSTGRES_DB`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `POSTGRES_PORT`
- `API_PORT`
- `JWT_SECRET`
- `JWT_EXPIRATION_MINUTES`
- `IGDB_CLIENT_ID`
- `IGDB_CLIENT_SECRET`
- `IGDB_BASE_URL`
- `TWITCH_TOKEN_URL`
- `CORS_ALLOWED_ORIGINS`

Create the frontend environment file:

```powershell
cd checkpointd-web
Copy-Item .env.example .env
```

Frontend variable:

- `VITE_API_BASE_URL`, normally `http://localhost:8080` for local development. Leave it empty (`""`) for a same-origin production build — use `??`, not `||`, anywhere this value is read, since an intentional empty string is falsy in JS.

Use Twitch Developer Console credentials locally for `IGDB_CLIENT_ID` and `IGDB_CLIENT_SECRET`. Do not commit real secrets. The frontend does not receive IGDB credentials and never calls IGDB directly.

IGDB variables are only needed for real external search/import calls. If they are missing, those external provider endpoints return `503 Service Unavailable` while the rest of the API can still run.

## Run PostgreSQL

From the repository root:

```powershell
docker compose up -d db
```

Stop PostgreSQL without deleting the database volume:

```powershell
docker compose down
```

Do not use `docker compose down -v` unless you intentionally want to delete local database data.

## Run Backend

From the repository root:

```powershell
cd checkpointd-api
$env:JWT_SECRET="replace-with-local-development-secret"
$env:IGDB_CLIENT_ID="replace-with-client-id"
$env:IGDB_CLIENT_SECRET="replace-with-client-secret"
$env:IGDB_BASE_URL="https://api.igdb.com/v4"
$env:TWITCH_TOKEN_URL="https://id.twitch.tv/oauth2/token"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

The backend runs locally on `http://localhost:8080` by default.

## Run Frontend

From the repository root:

```powershell
cd checkpointd-web
npm install
npm run dev
```

The Vite dev server runs locally on `http://localhost:5173` by default.

### Frontend routes

Route groups are split by shell in `App.tsx`: `PublicShell` renders when logged out, `AppShell` when logged in — most routes render under whichever shell matches the current auth state rather than hard-requiring login.

Reachable while logged out (`PublicShell`) or logged in (`AppShell`):

- `/login`, `/register` — authentication (redirect to `/library` if already logged in)
- `/u/:username` — public profile
- `/u/:username/reviews` — a user's public reviews (sortable)
- `/u/:username/games/:gameId[/:slug]` — a user's public review of one game
- `/u/:username/lists` — a user's public lists
- `/u/:username/lists/:listId[/:slug]` — a public list's detail
- `/u/:username/followers`, `/u/:username/following` — follow lists
- `/games/:gameId[/:slug]/reviews` — a game's public reviews
- `/games/:gameId[/:slug]` — a game detail page (metadata, media, reviews; Add-to-Library/Write-a-review hidden until logged in)
- `/privacy`, `/about` — static pages
- `/verify-email` — handles the emailed verification link

Authenticated only (`AppShell`, redirects to `/login` otherwise):

- `/search` — multi-category search (Games / Lists / Members)
- `/library` — the current user's library, stats, filters, search, sorting
- `/library/:entryId[/:slug]` — a library entry detail page
- `/lists` — the current user's own lists
- `/lists/popular` — popular public lists
- `/notifications` — in-app notifications
- `/admin/comments` — reported-comment moderation queue (admin role only)

## Validation Commands

Backend tests:

```powershell
cd checkpointd-api
.\mvnw.cmd clean test
```

Frontend production build (also runs the TypeScript compiler):

```powershell
cd checkpointd-web
npm run build
```

Docker Compose configuration:

```powershell
docker compose config
```

There is currently no frontend automated test suite — frontend verification is `tsc --noEmit` (via `npm run build`) plus manual smoke testing.

## API Overview

Full paths, grouped by backend module. See the module table above for what each owns.

**auth** (`/api/v1/auth`, `/api/v1/users`)

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/verify-email` — public, consumes an emailed verification token
- `GET /api/v1/users/me`
- `DELETE /api/v1/users/me` — password-confirmed self-service account deletion
- `POST /api/v1/users/me/resend-verification` — authenticated, 60s cooldown

**game** (`/api/v1/games`)

- `POST /api/v1/games`
- `GET /api/v1/games?q={query}`
- `GET /api/v1/games/{gameId}` — public

**externalgames** (`/api/v1/external-games`)

- `GET /api/v1/external-games/search?q={query}` — IGDB search, filtered to real standalone games
- `POST /api/v1/external-games/import`

**library** (`/api/v1/library`)

- `POST /api/v1/library`
- `GET /api/v1/library` — paginated, filterable by status/`q`, sortable
- `GET /api/v1/library/stats`
- `GET /api/v1/library/by-game/{gameId}`
- `GET /api/v1/library/by-external-game?provider=&externalId=`
- `GET /api/v1/library/{entryId}`
- `PATCH /api/v1/library/{entryId}`
- `POST /api/v1/library/{entryId}/sync-metadata`
- `DELETE /api/v1/library/{entryId}`

**profile** (`/api/v1/profiles`)

- `GET /api/v1/profiles/me`
- `PATCH /api/v1/profiles/me`
- `GET /api/v1/profiles/search?q={query}` — member search
- `GET /api/v1/profiles/{username}`

**review** (`/api/v1/reviews`)

- `GET /api/v1/reviews/games/{gameId}`
- `GET /api/v1/reviews/users/{username}?sort={newest|oldest|highest|lowest}`
- `GET /api/v1/reviews/users/{username}/games/{gameId}`
- `GET /api/v1/reviews/me`
- `GET /api/v1/reviews/me/games/{gameId}`
- `PUT /api/v1/reviews/me/games/{gameId}`
- `DELETE /api/v1/reviews/me/games/{gameId}`

**list** (`/api/v1/lists`)

- `POST /api/v1/lists`
- `GET /api/v1/lists/me`
- `GET /api/v1/lists/me/{listId}`
- `PATCH /api/v1/lists/me/{listId}`
- `DELETE /api/v1/lists/me/{listId}`
- `POST /api/v1/lists/me/{listId}/items`
- `DELETE /api/v1/lists/me/{listId}/items/{gameId}`
- `GET /api/v1/lists/search?q={query}`
- `GET /api/v1/lists/popular`
- `GET /api/v1/lists/users/{username}`
- `GET /api/v1/lists/users/{username}/{listId}`

**follow** (`/api/v1/follows`)

- `POST /api/v1/follows/users/{username}`
- `DELETE /api/v1/follows/users/{username}`
- `GET /api/v1/follows/users/{username}/status`
- `GET /api/v1/follows/users/{username}/followers`
- `GET /api/v1/follows/users/{username}/following`

**like** (`/api/v1/likes`)

- `POST /api/v1/likes/lists/{listId}`
- `DELETE /api/v1/likes/lists/{listId}`
- `GET /api/v1/likes/lists/{listId}/status`
- `POST /api/v1/likes/reviews/{reviewId}`
- `DELETE /api/v1/likes/reviews/{reviewId}`
- `GET /api/v1/likes/reviews/{reviewId}/status`

**comment** (`/api/v1/comments`, comment likes under `/api/v1/likes`, moderation under `/api/v1/admin/comments`)

- `POST /api/v1/comments/lists/{listId}`
- `GET /api/v1/comments/lists/{listId}`
- `DELETE /api/v1/comments/lists/{listId}/{commentId}` — author or admin
- `POST /api/v1/comments/lists/{listId}/{commentId}/report`
- `POST /api/v1/comments/reviews/{reviewId}`
- `GET /api/v1/comments/reviews/{reviewId}`
- `DELETE /api/v1/comments/reviews/{reviewId}/{commentId}` — author or admin
- `POST /api/v1/comments/reviews/{reviewId}/{commentId}/report`
- `POST /api/v1/likes/list-comments/{commentId}`
- `DELETE /api/v1/likes/list-comments/{commentId}`
- `GET /api/v1/likes/list-comments/{commentId}/status`
- `POST /api/v1/likes/review-comments/{commentId}`
- `DELETE /api/v1/likes/review-comments/{commentId}`
- `GET /api/v1/likes/review-comments/{commentId}/status`
- `GET /api/v1/admin/comments/lists/reported` — admin only (role checked in code, not route config)
- `GET /api/v1/admin/comments/reviews/reported` — admin only

**notification** (`/api/v1/notifications`)

- `GET /api/v1/notifications`
- `GET /api/v1/notifications/unread-count`
- `POST /api/v1/notifications/read-all`

There is no `hasRole()`/route-level role gating in `SecurityConfig` — admin-only endpoints read the JWT's `role` claim manually in the controller/service.

## CI

- `API CI` runs backend tests for `checkpointd-api`.
- `Web CI` installs frontend dependencies with `npm ci` and builds `checkpointd-web`.

## Production Deployment

checkpointd runs in production as three Docker containers defined in `docker-compose.prod.yml`: Postgres, the Spring Boot API, and a Caddy container that serves the built frontend and reverse-proxies `/api/*` to the API — same origin, so no CORS is needed in the browser and only one TLS certificate is required. Caddy obtains and renews that certificate automatically from Let's Encrypt; there is no separate nginx/certbot setup. `db` and `api` are not reachable from outside the VPS — only Caddy publishes ports 80/443.

### One-time VPS setup

1. Point the domain's DNS `A` record at the VPS's public IP.
2. Install Docker Engine and the Compose plugin.
3. If the VPS has limited RAM, add a swapfile (at least 2GB) — building the Maven jar and the Vite bundle on the VPS is the main memory pressure point.
4. Restrict the firewall to SSH, HTTP, and HTTPS, e.g. with `ufw`:
   ```bash
   sudo ufw allow 22
   sudo ufw allow 80
   sudo ufw allow 443
   sudo ufw enable
   ```
5. Clone the repository, e.g. to `/opt/checkpointd`.
6. Copy `.env.prod.example` to `.env.prod` and fill in real values — a freshly generated `JWT_SECRET` (never reuse the local dev secret), real IGDB credentials, and Postgres credentials.
7. Run `deploy/deploy.sh`. On first boot, Caddy requests its certificate once DNS resolves and ports 80/443 are reachable.

### Redeploying

From the repo checkout on the VPS:

```bash
deploy/deploy.sh
```

This pulls `main`, rebuilds the images, restarts the stack with `docker compose up -d`, and prunes old images. Flyway migrations run automatically on API startup, same as local development — no manual migration step. Deploys are deliberate and manual by design — there is no CI/CD auto-deploy on merge.

### Database Backups

`deploy/backup-db.sh` dumps the production Postgres database with `pg_dump`, gzips it to `/var/backups/checkpointd/`, and prunes anything older than 14 days, via a daily cron job on the VPS. An off-box copy is also pulled to a machine outside the VPS daily (via a scheduled `scp` pull over the same SSH key used for deploys) — that pull script lives outside this repo since it runs on the puller's machine, not the VPS.

To set up the on-VPS cron job:

```bash
sudo crontab -e
```

```cron
0 3 * * * /opt/checkpointd/deploy/backup-db.sh >> /var/log/checkpointd-backup.log 2>&1
```

To restore a backup:

```bash
cd /opt/checkpointd
gunzip -c /var/backups/checkpointd/checkpointd-<timestamp>.sql.gz | \
  docker compose -f docker-compose.prod.yml --env-file .env.prod exec -T db \
  psql -U checkpointd -d checkpointd
```

## Security Notes

- `JWT_SECRET` is required for local backend auth.
- `IGDB_CLIENT_SECRET` must stay server-side.
- `.env` files are ignored by git.
- Use `.env.example` only for placeholder values and variable names.
- The frontend sends JWT Bearer tokens to checkpointd only; it does not receive Twitch or IGDB credentials.
- Passwords are hashed with BCrypt before storage; account deletion requires re-entering the password and cascades all owned data (library entries, reviews, lists, follows, likes, comments, notifications) via `ON DELETE CASCADE`.
- `RESEND_API_KEY` must stay server-side; email verification tokens are single-use and expire after 24 hours.

## Roadmap

Shipped since the original MVP: public profiles, reviews, lists, follows, likes, comments with moderation, notifications, account deletion, multi-category search, email verification, and public game detail pages — see `Features` above.

Still open:

- Re-sync or backfill metadata for existing cached games in bulk (today only individual entries can manually re-sync)
- Server-side/paginated library search improvements
- Crossplay data
- Recommendations
- Error tracking / observability (e.g. Sentry) — today the only way to learn about a bug is manual log-checking or a user mentioning it
- CI/CD auto-deploy (deploys are manual by design for now)
- Frontend automated test suite (Vitest/RTL) — verification today is typecheck + build + manual testing
- Ongoing UI polish

Done:

- [x] Deployment — live at [checkpointd.fun](https://checkpointd.fun)
- [x] Public profiles and lists
- [x] Social features (reviews, follows, likes, comments, notifications)
- [x] Off-box database backups
- [x] Email verification
- [x] Public game detail pages

## Philosophy

checkpointd is local-first and deploy-aware. Local development should be easy to run with Docker Compose and clear environment variables, while project decisions should leave room for production deployment later without coupling the app to one hosting provider or machine-specific setup.
