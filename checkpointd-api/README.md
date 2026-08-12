# checkpointd-api

Spring Boot backend API for checkpointd.

## Stack

- Java 25
- Spring Boot 4.1.0
- Spring Security (JWT via OAuth2 resource server support)
- Spring Data JPA
- PostgreSQL 18
- Flyway (schema currently at `V18`)
- Maven Wrapper

## Architecture

Package-per-domain-module under `com.chmz31.checkpointd`, each following `entity/ repository/ service/ controller/ dto/` internally, with a deliberate no-cycles discipline between modules (check whether a target module already depends on the source module, directly or transitively, before adding a new cross-module dependency).

| Module | Owns |
|---|---|
| `auth` | Registration, login, JWT issuance, current-user identity, account deletion |
| `user` | Core `User` entity and repository (no controller of its own) |
| `game` | Canonical local `Game` entity — creation, lookup, listing, metadata staleness |
| `externalgames` | External IGDB search through Twitch client credentials, import/cache into local `Game` |
| `library` | Per-user library entries — add/list/filter/get/update/delete, stats, metadata sync |
| `profile` | Public/own profile data, profile editing, member search |
| `review` | Per-user, per-game reviews (upsert), public/own listings, sortable order |
| `list` | User-curated game lists — CRUD, items, search, popularity, public listing |
| `follow` | User-to-user follow relationships, followers/following listings |
| `like` | Likes on lists and reviews |
| `comment` | Comments on lists/reviews with one level of threaded replies, comment likes, reporting, admin moderation queue |
| `notification` | In-app notifications (follows, likes, comments, replies), unread counts, read-state |
| `common` | Cross-cutting: `SecurityConfig`, async/HTTP client config, shared `PaginatedResponse` DTO, global exception handling |

**Auth model:** JWT (HS256) via Spring Security's OAuth2 resource server support, not sessions. There is no `hasRole()`/route-level role gating in `SecurityConfig` — it only distinguishes `authenticated()` vs `permitAll()` per route. Admin-only behavior (comment moderation) reads the JWT's `role` claim manually inside the controller/service.

**Entity modeling:** dedicated entity pairs per target type are the default (`ListLike`/`ReviewLike`, `ListComment`/`ReviewComment`) rather than generic/polymorphic tables. `notification.entity.Notification` is the one deliberate exception — a single generic entity with nullable FKs to lists and reviews, justified by 8 notification types that only ever resolve to one of two link shapes.

## Game Metadata

checkpointd stores game metadata:

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

**IGDB search filtering:** external search restricts results to IGDB's `game_type` field (values `0, 4, 8, 9, 10, 11` — main game, standalone expansion, remake, remaster, expanded game, port), with a null-safety fallback since `game_type` isn't populated for every record. IGDB's older `category` field looks like it should do this job but is almost entirely unpopulated in practice — `game_type` is the field that's actually reliably filled in. This was found the hard way: filtering on `category` alone silently returned zero results for every search once deployed, since even ordinary main games have no `category` value set. See `IgdbClient.searchBody` for the exact query and the comment explaining the enum values.

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

Single test class: `.\mvnw.cmd test -Dtest=ClassName`. Single method: `.\mvnw.cmd test -Dtest=ClassName#methodName`.

Tests do not require a local PostgreSQL instance. Services use Mockito unit tests (`@ExtendWith(MockitoExtension.class)`); controllers use full-context `@SpringBootTest @AutoConfigureMockMvc` tests, which require every repository in the app mocked via `@MockitoBean` — adding a new repository means adding a `@MockitoBean` to every existing controller test class.
