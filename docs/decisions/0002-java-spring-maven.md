# 0002: Java 25, Spring Boot 4.1, And Maven

## Status

Accepted

## Context

checkpointd v0.1 is focused on a backend API. The project needs a stable JVM foundation, a mature web framework, and a conventional build tool that work well for local development and future CI.

## Decision

Use Java 25 LTS, Spring Boot 4.1.x, and Maven for the backend API in `checkpointd-api/`.

## Consequences

The backend starts on the current planned Java LTS release with Spring Boot conventions for web, security, data access, validation, Flyway migrations, and actuator support. Maven keeps the project easy to build from a fresh checkout through the checked-in Maven wrapper.

Dependency and version changes should be intentional and explicit, not incidental to feature work.
