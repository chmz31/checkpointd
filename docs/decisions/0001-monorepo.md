# 0001: Initial Monorepo

## Status

Accepted

## Context

checkpointd will have a backend API, a future frontend, local development infrastructure, and project documentation. Early development benefits from keeping these pieces discoverable in one repository while the product shape is still small.

## Decision

Use a monorepo with separate top-level workspaces:

- `checkpointd-api/` for the backend API
- `checkpointd-web/` for the future frontend
- `docs/` for architecture notes and decisions
- `.github/workflows/` reserved for future CI workflows

## Consequences

The repository can evolve backend, frontend, documentation, and local infrastructure together while preserving clear boundaries between application layers. CI, deployment, and generated project files can be added later without changing the initial layout.
