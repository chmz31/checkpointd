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

## Frontend

`checkpointd-web/` is a React + TypeScript + Vite app. It stores the JWT access token in `localStorage` and calls the checkpointd API with Bearer authentication.

The current frontend supports auth, external game search, import, add-to-library, library filtering, editing, and deletion.

## Data

PostgreSQL is the system of record for users, local games, and library entries. Flyway manages schema migrations. Docker Compose provides local PostgreSQL for development.

## CI

GitHub Actions contains separate workflows for backend and frontend validation:

- `api-ci.yml` runs Maven backend tests.
- `web-ci.yml` installs frontend dependencies with `npm ci` and runs the Vite build.

## Boundaries

- Backend and frontend code stay separated by workspace.
- External provider credentials stay in backend configuration.
- `.env` files are ignored and must not be committed.
- Deployment-specific configuration should be introduced only when deployment work begins.
