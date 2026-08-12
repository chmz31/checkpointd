# checkpointd-web

React + TypeScript + Vite frontend for checkpointd.

## Stack

- React 19
- TypeScript
- Vite 7
- React Router 7
- No external UI/state-management/HTTP-client libraries — plain CSS, local component state, and a hand-rolled `fetch`-based API client (`src/api.ts`)

## Features

- Login and registration, JWT stored in `localStorage`.
- Self-service account deletion (password-confirmed).
- External game search (via the backend/IGDB) with direct add-to-library; also search public lists and members from the same page.
- Personal library: stats, status filter, local search, sorting, metadata sync, edit, delete.
- Library entry and public game detail pages with metadata, media gallery, and tracking facts.
- Reviews: write/edit a rating + text review per game (spoiler flag, public/private visibility); browse a game's or a user's reviews, sortable by newest/oldest/highest/lowest rated.
- Lists: create/edit/delete public or private game lists, add/remove games, browse popular lists and a user's own lists.
- Follows: follow/unfollow users, view followers/following lists.
- Likes on reviews and lists.
- Comments on reviews and lists with one level of threaded replies, comment likes, and reporting.
- In-app notifications with an unread badge (polled every 30s), mark-all-read on page view.
- Public profiles: bio, stats, recent games/reviews, public/private visibility.
- Static Privacy and About pages, linked from a footer on every page.

## Routes

Route groups are split by shell in `App.tsx`: `PublicShell` renders when logged out, `AppShell` when logged in — most routes render under whichever shell matches the current auth state, rather than hard-requiring login. `AppShell` persists across in-group navigation (e.g. the notification-count polling effect lives there).

**Reachable logged out or logged in:**

- `/login`, `/register` — redirect to `/library` if already logged in
- `/u/:username` — public profile
- `/u/:username/reviews` — a user's public reviews
- `/u/:username/games/:gameId[/:slug]` — a user's public review of one game
- `/u/:username/lists` — a user's public lists
- `/u/:username/lists/:listId[/:slug]` — a public list's detail
- `/u/:username/followers`, `/u/:username/following`
- `/games/:gameId[/:slug]/reviews` — a game's public reviews
- `/privacy`, `/about`

**Authenticated only** (redirects to `/login` otherwise):

- `/search` — multi-category search (Games / Lists / Members)
- `/library`, `/library/:entryId[/:slug]`
- `/games/:gameId[/:slug]` — game detail page (distinct from the `/reviews` variant above)
- `/lists`, `/lists/popular`
- `/notifications`
- `/admin/comments` — reported-comment moderation queue, gated inline on `user.role === 'ADMIN'`

The `:gameId`/`:entryId`/`:listId` routes each have a "with slug" and "without slug" form — the slug is an optional, SEO-friendly URL segment; the id alone still resolves the same page.

`src/routePaths.ts` centralizes route-building helpers for all of the above — build URLs with those helpers rather than hand-building path strings. `src/types.ts` mirrors backend response/DTO shapes by hand (no codegen); keep them in sync manually when a backend response shape changes.

**UI convention:** no dropdowns/popovers anywhere in the app — everything is full-page navigation or inline reveal (e.g. a mobile nav hamburger expands the menu in normal document flow, not as an overlay).

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

Build (also typechecks):

```powershell
npm run build
```

The app talks only to the checkpointd backend API. It does not call IGDB directly and does not receive IGDB or Twitch credentials.

There is no automated frontend test suite yet — verification is `npm run build` (typecheck + production build) plus manual smoke testing.
