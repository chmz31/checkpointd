# checkpointd

checkpointd - your save file for every game you play.

checkpointd is a video game backlog and library app inspired by Letterboxd, IMDb, Backloggd, and GameTrack. It lets players search for games, import game metadata into a local catalog, and track their personal library with status, ratings, and notes.

## Current MVP

- Register and login with JWT authentication.
- Fetch the current authenticated user.
- Search external games through the backend using IGDB.
- Import external games into the local catalog/cache with basic metadata.
- Store and display game summaries, genres, platforms, screenshots, artworks, and backdrop images.
- Create and browse local catalog games.
- Add games to a personal library.
- List, filter, fetch, update, and delete library entries.
- Track library status, rating, notes, and library stats.
- Use the React frontend for routed login/register, external search, direct add-to-library, library stats, filtering, search, sorting, editing, deletion, and metadata sync.
- Show metadata chips and short summary previews in search results and library cards when metadata is available.
- Open dedicated library entry detail pages with full metadata, tracking facts, notes, sync, edit, and delete actions.
- Show a detail-page Media section with screenshots/artworks when visual media is available.

Metadata appears for newly imported/cached games. Existing cached games are not automatically backfilled with IGDB metadata yet, but `Sync metadata` can refresh summary, genres, platforms, screenshots, artworks, and backdrop images for individual IGDB-backed library entries.

## Tech Stack

Backend:

- Java 25
- Spring Boot
- Spring Security JWT
- Spring Data JPA
- PostgreSQL 18
- Flyway
- Docker Compose
- Maven Wrapper

Frontend:

- React
- TypeScript
- Vite
- React Router
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
  deploy/               Production deploy script
  .github/workflows/    Backend and frontend CI workflows
  docker-compose.yml    Local PostgreSQL
  docker-compose.prod.yml   Production stack (Postgres, API, Caddy)
```

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

- `VITE_API_BASE_URL`, normally `http://localhost:8080` for local development

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

Frontend routes:

- `/login` and `/register` for authentication
- `/search` for authenticated external game search and direct add-to-library
- `/library` for the authenticated user's library, stats, filters, search, and sorting
- `/library/:entryId` for an authenticated user's library entry detail page

`/library/:entryId` is a user-library detail page, not a public game page.

## Validation Commands

Backend tests:

```powershell
cd checkpointd-api
.\mvnw.cmd clean test
```

Frontend production build:

```powershell
cd checkpointd-web
npm run build
```

Docker Compose configuration:

```powershell
docker compose config
```

## API Overview

Auth:

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `GET /api/v1/users/me`

External games:

- `GET /api/v1/external-games/search?q={query}`
- `POST /api/v1/external-games/import`

Local games:

- `POST /api/v1/games`
- `GET /api/v1/games`
- `GET /api/v1/games/{gameId}`

Local game responses include core fields plus optional `summary`, `genres`, `platforms`, `screenshotUrls`, `artworkUrls`, and `backdropUrl`.

Library:

- `POST /api/v1/library`
- `GET /api/v1/library`
- `GET /api/v1/library/stats`
- `GET /api/v1/library/{entryId}`
- `PATCH /api/v1/library/{entryId}`
- `POST /api/v1/library/{entryId}/sync-metadata`
- `DELETE /api/v1/library/{entryId}`

Library entry responses include the tracked entry fields plus selected game metadata and media for card and detail display.

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

This pulls `main`, rebuilds the images, restarts the stack with `docker compose up -d`, and prunes old images. Flyway migrations run automatically on API startup, same as local development — no manual migration step.

## Security Notes

- `JWT_SECRET` is required for local backend auth.
- `IGDB_CLIENT_SECRET` must stay server-side.
- `.env` files are ignored by git.
- Use `.env.example` only for placeholder values and variable names.
- The frontend sends JWT Bearer tokens to checkpointd only; it does not receive Twitch or IGDB credentials.

## Roadmap

- Ongoing UI polish
- Re-sync or backfill metadata for existing cached games
- Public game pages
- Richer game detail metadata and media
- Pagination and server-side library search
- Public profiles and lists
- Social features
- Crossplay data
- Recommendations
- [x] Deployment — live at [checkpointd.fun](https://checkpointd.fun)

## Philosophy

checkpointd is local-first and deploy-aware. Local development should be easy to run with Docker Compose and clear environment variables, while project decisions should leave room for production deployment later without coupling the app to one hosting provider or machine-specific setup.
