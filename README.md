# checkpointd

checkpointd - your save file for every game you play.

checkpointd is a video game backlog and library app inspired by Letterboxd, IMDb, Backloggd, and GameTrack. The goal is to help players search for games, track what they own or play, and keep personal notes, ratings, and status history in one place.

## Planned Stack

- Java 25 LTS
- Spring Boot 4.1.x
- Maven
- PostgreSQL 18
- Flyway
- Spring Security + JWT
- React + TypeScript + Vite later
- Docker Compose for local development
- GitHub Actions CI later

## v0.1 API MVP Scope

- Backend API only
- Auth
- External game search from the backend
- Local metadata cache
- Personal game library
- Status, rating, and notes
- Basic filters

## Local Development

v0.1 is API-only for now. The frontend workspace exists for a later milestone, but local development currently centers on PostgreSQL and the backend API.

Create a local environment file from the safe example:

```powershell
Copy-Item .env.example .env
```

Start PostgreSQL with Docker Compose:

```powershell
docker compose up -d db
```

```md
Stop PostgreSQL without deleting the database volume:

```powershell
docker compose down

Do not use docker compose down -v unless you intentionally want to delete local database data.

Validate the Docker Compose configuration:

```powershell
docker compose config
```

Run backend tests from `checkpointd-api`:

```powershell
cd checkpointd-api
.\mvnw.cmd clean test
```

## Out Of Scope For v0.1

- Frontend implementation
- Public reviews
- Public profiles
- Social features
- Followers
- Lists
- Comments
- Crossplay
- Cross-save
- Steam import
- Achievements
- Recommendations
- Advanced statistics
- Production deployment
- Domain, hosting, or VPS setup

## Philosophy

checkpointd is local-first and deploy-aware. Local development should be easy to run with Docker Compose and clear environment variables, while project decisions should leave room for production deployment later without coupling early code to one hosting provider or machine-specific setup.
