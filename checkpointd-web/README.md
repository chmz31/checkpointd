# checkpointd-web

React + TypeScript + Vite frontend for checkpointd.

## Current MVP

- Login and registration
- JWT token storage in `localStorage`
- External game search through the checkpointd backend
- External game import
- Add imported games to the authenticated user's library
- Library list, status filter, edit, and delete

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
