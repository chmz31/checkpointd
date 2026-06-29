# Architecture

checkpointd starts as a local-first monorepo with clearly separated backend and frontend workspaces.

The v0.1 focus is the backend API. It will eventually own authentication, external game search integration, local metadata caching, and personal library data. PostgreSQL is the planned system of record, with Flyway managing schema migrations once application code begins.

The frontend is planned for a later milestone and will live independently under `checkpointd-web/`. Shared deployment concerns should be introduced only when needed, without coupling early backend development to frontend implementation details.
