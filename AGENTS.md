# AGENTS.md

Guidance for Codex and other coding agents working in this repository.

## Scope

- Stay within the user's requested scope.
- Do not add product features, application code, controllers, services, entities, migrations, frontend screens, or CI workflows unless explicitly asked.
- Keep backend and frontend work separated between `checkpointd-api/` and `checkpointd-web/`.

## Stack Discipline

- Do not change the planned Java, Spring Boot, Maven, PostgreSQL, or frontend dependency versions without permission.
- Planned backend stack: Java 25 LTS, Spring Boot 4.1.x, Maven, PostgreSQL 18, Flyway, Spring Security + JWT.
- Planned frontend stack: React, TypeScript, and Vite.

## Configuration And Secrets

- Do not introduce real secrets.
- Do not create `.env` unless explicitly requested.
- Use `.env.example` for placeholder environment variable names and safe sample values only.
- Do not hardcode `localhost` in application code. Use configuration and environment variables.

## Development Practice

- Run relevant commands after changes.
- Explain which files changed and which commands were executed.
- Keep changes small, intentional, and aligned with the repository structure.
