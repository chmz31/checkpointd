# Architecture

checkpointd is a local-first, deploy-aware monorepo with separated backend and frontend workspaces.

## Backend

`checkpointd-api/` owns the application API and server-side integrations. It is a Java 25 Spring Boot application using Spring Security JWT, Spring Data JPA, PostgreSQL 18, and Flyway migrations.

The backend is responsible for:

- registering and authenticating users
- issuing and validating JWT access tokens
- exposing the current authenticated user
- searching IGDB through Twitch client credentials
- importing external game metadata into the local game catalog
- managing user library entries

IGDB credentials and Twitch access tokens stay server-side. The frontend never calls IGDB directly.

External game flow:

1. The frontend searches checkpointd.
2. The backend searches IGDB.
3. The frontend can add a result to the library.
4. The backend imports/caches the local game, then creates the user library entry.

## Frontend

`checkpointd-web/` is a React + TypeScript + Vite app using React Router. It stores the JWT access token in `localStorage` and calls the checkpointd API with Bearer authentication.

The current frontend supports auth, external game search, direct add-to-library, metadata chips, summary previews, library stats, library filtering, local search, sorting, editing, deletion, metadata sync, and library entry detail pages.

Frontend routing:

- `/login` and `/register` are unauthenticated auth routes.
- `/search`, `/library`, and `/library/:entryId` are authenticated routes rendered inside the app shell.
- `/library/:entryId` is a protected user-library entry detail page backed by the library entry API. It is not a public game page.

## Data

PostgreSQL is the system of record for users, local games, and library entries. Flyway manages schema migrations. Docker Compose provides local PostgreSQL for development.

Game metadata is stored simply:

- `games.summary`
- `games.backdrop_url`
- `game_genres`
- `game_platforms`
- `game_screenshots`
- `game_artworks`

Genres, platforms, screenshots, and artworks are ordered string collections owned by a game. checkpointd does not currently model genres, platforms, or media as full entities.

External search remains lightweight. Richer media is captured during IGDB fetch-by-id flows used by import and metadata sync. Existing cached games are not automatically re-synced or backfilled yet, but individual IGDB-backed library entries can refresh metadata and media through sync.

Future frontend/API work may add public game pages, richer game detail metadata and media, reviews, lists, and social features.

## CI

GitHub Actions contains separate workflows for backend and frontend validation:

- `api-ci.yml` runs Maven backend tests.
- `web-ci.yml` installs frontend dependencies with `npm ci` and runs the Vite build.

## Boundaries

- Backend and frontend code stay separated by workspace.
- External provider credentials stay in backend configuration.
- `.env` files are ignored and must not be committed.
- Deployment-specific configuration should be introduced only when deployment work begins.
