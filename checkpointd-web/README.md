# checkpointd-web

React + TypeScript + Vite frontend for checkpointd.

## Current MVP

- Login and registration
- JWT token storage in `localStorage`
- Routed authenticated pages for Search and Library
- External game search through the checkpointd backend
- Direct add from external search results to the authenticated user's library
- Metadata chips for genres and platforms
- Short game summary previews
- Library stats
- Library list with status filter, local search, and sorting
- Library entry edit and delete

## Local Development

Create a local environment file:

```powershell
Copy-Item .env.example .env
```

Default local API URL:

```text
VITE_API_BASE_URL=http://localhost:8080
```

Install dependencies:

```powershell
npm install
```

Run the dev server:

```powershell
npm run dev
```

Build:

```powershell
npm run build
```

The app talks only to the checkpointd backend API. It does not call IGDB directly and does not receive IGDB or Twitch credentials.
