# checkpointd-web

React + TypeScript + Vite frontend for checkpointd.

## Current MVP

- Login and registration
- JWT token storage in `localStorage`
- React Router routes for auth, search, library, and library entry details
- External game search through the checkpointd backend
- Direct add from external search results to the authenticated user's library
- Metadata chips for genres and platforms
- Short game summary previews
- Library stats
- Library list with status filter, local search, and sorting
- Library entry detail page with full metadata, tracking facts, notes, sync, edit, and delete
- Library entry media gallery for screenshots/artworks when available

## Routes

- `/login` shows the login form.
- `/register` shows the registration form.
- `/search` is an authenticated page for external game search and direct add-to-library.
- `/library` is an authenticated page for the current user's library, stats, search, filters, and sorting.
- `/library/:entryId` is an authenticated detail page for one of the current user's library entries.

`/library/:entryId` is not a public game page; it is scoped to the authenticated user's tracked entry.

The detail page uses a backdrop image when available, falling back to the cover image. The Media section appears only when the backend has screenshots or artworks for that game.

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
