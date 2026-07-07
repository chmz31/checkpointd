# checkpointd-api

Spring Boot backend API for checkpointd.

## Stack

- Java 25
- Spring Boot
- Spring Security JWT
- Spring Data JPA
- PostgreSQL 18
- Flyway
- Maven Wrapper

## Responsibilities

- Auth and current-user endpoints
- JWT access token generation and validation
- Local game catalog persistence
- External IGDB search through Twitch client credentials
- External game import/cache
- User library add/list/filter/get/update/delete

## Game Metadata

checkpointd stores basic game metadata:

- `summary`
- `genres`
- `platforms`
- `screenshotUrls`
- `artworkUrls`
- `backdropUrl`

Metadata is included in:

- local game responses
- external game search results
- imported game responses
- library entry responses

Flyway migration `V3__add_game_metadata.sql` adds `games.summary`, `game_genres`, and `game_platforms`. Genres and platforms are simple ordered string collections, not full domain entities.

Flyway migration `V4__add_game_media_metadata.sql` adds `games.backdrop_url`, `game_screenshots`, and `game_artworks`. Screenshots and artworks are simple ordered URL collections owned by a game.

External search remains lightweight and focuses on core metadata. Richer visual media is retrieved during IGDB fetch-by-id flows used by import and metadata sync, then persisted for newly imported/cached or individually synced games.

Existing cached games are not backfilled automatically.

## Local Development

Start PostgreSQL from the repository root:

```powershell
docker compose up -d db
```

Create the root `.env` file from `.env.example` for Docker Compose:

```powershell
Copy-Item ..\.env.example ..\.env
```

Docker Compose reads the root `.env` file for PostgreSQL settings. Spring Boot does not automatically read the root `.env` file when launched through Maven; set required backend variables in your shell or IDE run configuration.

IGDB variables are only needed for real external search/import calls. If they are missing, those endpoints return `503 Service Unavailable`.

Run the API:

```powershell
$env:JWT_SECRET="replace-with-local-development-secret"
$env:IGDB_CLIENT_ID="replace-with-client-id"
$env:IGDB_CLIENT_SECRET="replace-with-client-secret"
$env:IGDB_BASE_URL="https://api.igdb.com/v4"
$env:TWITCH_TOKEN_URL="https://id.twitch.tv/oauth2/token"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

The API runs on `http://localhost:8080` by default.

## Tests

Run from this directory:

```powershell
.\mvnw.cmd clean test
```

Tests do not require a local PostgreSQL instance.
