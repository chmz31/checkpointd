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

## Out Of Scope For Initial Scaffold

- Spring Boot project generation
- Frontend implementation
- Java entities
- Controllers
- Services
- Database migrations
- CI workflow
- Deploy config
- Real secrets

## Philosophy

checkpointd is local-first and deploy-aware. Local development should be easy to run with Docker Compose and clear environment variables, while project decisions should leave room for production deployment later without coupling early code to one hosting provider or machine-specific setup.
