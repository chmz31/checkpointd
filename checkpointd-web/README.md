# checkpointd-web

React + TypeScript + Vite frontend for checkpointd.

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

The app talks only to the checkpointd backend API. It does not call IGDB directly.
