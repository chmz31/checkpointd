import { FormEvent, useEffect, useMemo, useState } from 'react';
import { api, clearStoredToken, getStoredToken, setStoredToken } from './api';
import type {
  CurrentUser,
  ExternalGameSearchResult,
  Game,
  LibraryEntry,
  LibraryStatus,
} from './types';

const libraryStatuses: LibraryStatus[] = [
  'WISHLIST',
  'BACKLOG',
  'PLAYING',
  'COMPLETED',
  'DROPPED',
  'PAUSED',
];

type View = 'search' | 'library';
type AuthMode = 'login' | 'register';

export default function App() {
  const [token, setToken] = useState(getStoredToken());
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [authMode, setAuthMode] = useState<AuthMode>('login');
  const [view, setView] = useState<View>('search');
  const [libraryRefreshKey, setLibraryRefreshKey] = useState(0);

  useEffect(() => {
    if (!token) {
      setUser(null);
      return;
    }

    api
      .me()
      .then(setUser)
      .catch(() => {
        clearStoredToken();
        setToken(null);
      });
  }, [token]);

  function handleToken(nextToken: string) {
    setStoredToken(nextToken);
    setToken(nextToken);
  }

  function logout() {
    clearStoredToken();
    setToken(null);
    setUser(null);
    setView('search');
  }

  return (
    <main className="app-shell">
      <Header user={user} view={view} setView={setView} onLogout={logout} />
      {!token ? (
        <AuthPanel mode={authMode} setMode={setAuthMode} onToken={handleToken} />
      ) : (
        <section className="workspace">
          {view === 'search' ? (
            <SearchView onLibraryChange={() => setLibraryRefreshKey((key) => key + 1)} />
          ) : (
            <LibraryView refreshKey={libraryRefreshKey} />
          )}
        </section>
      )}
    </main>
  );
}

function Header({
  user,
  view,
  setView,
  onLogout,
}: {
  user: CurrentUser | null;
  view: View;
  setView: (view: View) => void;
  onLogout: () => void;
}) {
  return (
    <header className="topbar">
      <div>
        <p className="eyebrow">checkpointd</p>
        <h1>your save file for every game you play.</h1>
      </div>
      {user && (
        <nav className="nav-actions" aria-label="Primary">
          <button className={view === 'search' ? 'active' : ''} onClick={() => setView('search')}>
            Search
          </button>
          <button className={view === 'library' ? 'active' : ''} onClick={() => setView('library')}>
            Library
          </button>
          <span className="user-pill">{user.username}</span>
          <button onClick={onLogout}>Logout</button>
        </nav>
      )}
    </header>
  );
}

function AuthPanel({
  mode,
  setMode,
  onToken,
}: {
  mode: AuthMode;
  setMode: (mode: AuthMode) => void;
  onToken: (token: string) => void;
}) {
  const [email, setEmail] = useState('');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setLoading(true);
    setError(null);

    try {
      const response =
        mode === 'login'
          ? await api.login(email, password)
          : await api.register(email, username, password);
      onToken(response.accessToken);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Authentication failed');
    } finally {
      setLoading(false);
    }
  }

  return (
    <section className="auth-layout">
      <form className="panel auth-panel" onSubmit={submit}>
        <div className="section-heading">
          <h2>{mode === 'login' ? 'Login' : 'Create account'}</h2>
          <button
            type="button"
            className="link-button"
            onClick={() => setMode(mode === 'login' ? 'register' : 'login')}
          >
            {mode === 'login' ? 'Register' : 'Login'}
          </button>
        </div>
        <label>
          Email
          <input value={email} onChange={(event) => setEmail(event.target.value)} type="email" required />
        </label>
        {mode === 'register' && (
          <label>
            Username
            <input value={username} onChange={(event) => setUsername(event.target.value)} required />
          </label>
        )}
        <label>
          Password
          <input
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            type="password"
            minLength={8}
            required
          />
        </label>
        {error && <p className="error">{error}</p>}
        <button type="submit" disabled={loading}>
          {loading ? 'Working...' : mode === 'login' ? 'Login' : 'Register'}
        </button>
      </form>
    </section>
  );
}

function SearchView({ onLibraryChange }: { onLibraryChange: () => void }) {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<ExternalGameSearchResult[]>([]);
  const [importedGame, setImportedGame] = useState<Game | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function search(event: FormEvent) {
    event.preventDefault();
    if (!query.trim()) return;

    setLoading(true);
    setError(null);
    setImportedGame(null);

    try {
      setResults(await api.searchExternalGames(query.trim()));
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Search failed');
    } finally {
      setLoading(false);
    }
  }

  async function importGame(result: ExternalGameSearchResult) {
    setLoading(true);
    setError(null);

    try {
      setImportedGame(await api.importExternalGame(result.provider, result.externalId));
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Import failed');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="content-grid">
      <section className="panel">
        <div className="section-heading">
          <h2>External Search</h2>
          {loading && <span className="muted">Loading...</span>}
        </div>
        <form className="search-form" onSubmit={search}>
          <input
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Search for a game"
            required
          />
          <button type="submit" disabled={loading}>
            Search
          </button>
        </form>
        {error && <p className="error">{error}</p>}
        <div className="result-list">
          {results.map((result) => (
            <GameResultCard
              key={`${result.provider}-${result.externalId}`}
              result={result}
              onImport={() => importGame(result)}
            />
          ))}
        </div>
      </section>
      <section className="panel">
        <h2>Import</h2>
        {importedGame ? (
          <AddToLibraryForm game={importedGame} onAdded={onLibraryChange} />
        ) : (
          <p className="empty-state">Import a search result to cache it locally, then add it to your library.</p>
        )}
      </section>
    </div>
  );
}

function GameResultCard({
  result,
  onImport,
}: {
  result: ExternalGameSearchResult;
  onImport: () => void;
}) {
  return (
    <article className="game-row">
      <CoverImage src={result.coverUrl} title={result.title} />
      <div>
        <h3>{result.title}</h3>
        <p className="muted">
          {[result.slug, result.releaseDate].filter(Boolean).join(' | ') || result.provider}
        </p>
      </div>
      <button onClick={onImport}>Import</button>
    </article>
  );
}

function AddToLibraryForm({ game, onAdded }: { game: Game; onAdded: () => void }) {
  const [status, setStatus] = useState<LibraryStatus>('BACKLOG');
  const [rating, setRating] = useState('');
  const [notes, setNotes] = useState('');
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setLoading(true);
    setMessage(null);
    setError(null);

    try {
      await api.addLibraryEntry({
        gameId: game.id,
        status,
        rating: rating ? Number(rating) : null,
        notes: notes.trim() || undefined,
      });
      setMessage('Added to library.');
      onAdded();
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Could not add to library');
    } finally {
      setLoading(false);
    }
  }

  return (
    <form className="stack" onSubmit={submit}>
      <article className="game-row selected">
        <CoverImage src={game.coverUrl} title={game.title} />
        <div>
          <h3>{game.title}</h3>
          <p className="muted">{[game.slug, game.releaseDate].filter(Boolean).join(' | ')}</p>
        </div>
      </article>
      <label>
        Status
        <select value={status} onChange={(event) => setStatus(event.target.value as LibraryStatus)}>
          {libraryStatuses.map((libraryStatus) => (
            <option key={libraryStatus} value={libraryStatus}>
              {libraryStatus}
            </option>
          ))}
        </select>
      </label>
      <label>
        Rating
        <input
          value={rating}
          onChange={(event) => setRating(event.target.value)}
          min={1}
          max={10}
          type="number"
          placeholder="1-10"
        />
      </label>
      <label>
        Notes
        <textarea value={notes} onChange={(event) => setNotes(event.target.value)} rows={4} />
      </label>
      {message && <p className="success">{message}</p>}
      {error && <p className="error">{error}</p>}
      <button type="submit" disabled={loading}>
        {loading ? 'Adding...' : 'Add to Library'}
      </button>
    </form>
  );
}

function LibraryView({ refreshKey }: { refreshKey: number }) {
  const [entries, setEntries] = useState<LibraryEntry[]>([]);
  const [statusFilter, setStatusFilter] = useState<LibraryStatus | 'ALL'>('ALL');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const groupedCount = useMemo(() => entries.length, [entries]);

  async function loadLibrary() {
    setLoading(true);
    setError(null);

    try {
      setEntries(await api.listLibrary(statusFilter === 'ALL' ? undefined : statusFilter));
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Could not load library');
    } finally {
      setLoading(false);
    }
  }

  async function deleteEntry(entryId: string) {
    setError(null);
    try {
      await api.deleteLibraryEntry(entryId);
      setEntries((current) => current.filter((entry) => entry.id !== entryId));
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Could not remove entry');
    }
  }

  function replaceEntry(updatedEntry: LibraryEntry) {
    setEntries((current) => {
      if (statusFilter !== 'ALL' && updatedEntry.status !== statusFilter) {
        return current.filter((entry) => entry.id !== updatedEntry.id);
      }

      return current.map((entry) => (entry.id === updatedEntry.id ? updatedEntry : entry));
    });
  }

  useEffect(() => {
    loadLibrary();
  }, [refreshKey, statusFilter]);

  return (
    <section className="panel">
      <div className="section-heading">
        <h2>Library</h2>
        <div className="inline-actions">
          <span className="muted">{groupedCount} entries</span>
          <select
            className="compact-select"
            value={statusFilter}
            onChange={(event) => setStatusFilter(event.target.value as LibraryStatus | 'ALL')}
            aria-label="Filter library by status"
          >
            <option value="ALL">ALL</option>
            {libraryStatuses.map((libraryStatus) => (
              <option key={libraryStatus} value={libraryStatus}>
                {libraryStatus}
              </option>
            ))}
          </select>
          <button onClick={loadLibrary} disabled={loading}>
            Refresh
          </button>
        </div>
      </div>
      {error && <p className="error">{error}</p>}
      {loading && <p className="muted">Loading...</p>}
      {!loading && entries.length === 0 && <p className="empty-state">Your library is empty.</p>}
      <div className="library-list">
        {entries.map((entry) => (
          <LibraryEntryCard
            key={entry.id}
            entry={entry}
            onUpdated={replaceEntry}
            onDelete={() => deleteEntry(entry.id)}
          />
        ))}
      </div>
    </section>
  );
}

function LibraryEntryCard({
  entry,
  onUpdated,
  onDelete,
}: {
  entry: LibraryEntry;
  onUpdated: (entry: LibraryEntry) => void;
  onDelete: () => void;
}) {
  const [editing, setEditing] = useState(false);
  const [status, setStatus] = useState<LibraryStatus>(entry.status);
  const [rating, setRating] = useState(entry.rating ? String(entry.rating) : '');
  const [notes, setNotes] = useState(entry.notes || '');
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setStatus(entry.status);
    setRating(entry.rating ? String(entry.rating) : '');
    setNotes(entry.notes || '');
  }, [entry]);

  async function save(event: FormEvent) {
    event.preventDefault();
    setLoading(true);
    setMessage(null);
    setError(null);

    try {
      const updatedEntry = await api.updateLibraryEntry(entry.id, {
        status,
        ...(rating ? { rating: Number(rating) } : {}),
        notes,
      });
      onUpdated(updatedEntry);
      setEditing(false);
      setMessage('Saved.');
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Could not update entry');
    } finally {
      setLoading(false);
    }
  }

  function cancel() {
    setStatus(entry.status);
    setRating(entry.rating ? String(entry.rating) : '');
    setNotes(entry.notes || '');
    setEditing(false);
    setError(null);
  }

  return (
    <article className="library-entry-card">
      <div className="library-entry">
        <CoverImage src={entry.gameCoverUrl} title={entry.gameTitle} />
        <div>
          <h3>{entry.gameTitle}</h3>
          <p className="status-line">
            <span>{entry.status}</span>
            {entry.rating && <span>{entry.rating}/10</span>}
          </p>
          {entry.notes && <p className="notes">{entry.notes}</p>}
          {message && <p className="success compact-message">{message}</p>}
        </div>
        <div className="entry-actions">
          <button onClick={() => setEditing((current) => !current)}>{editing ? 'Close' : 'Edit'}</button>
          <button onClick={onDelete}>Delete</button>
        </div>
      </div>

      {editing && (
        <form className="edit-form" onSubmit={save}>
          <label>
            Status
            <select value={status} onChange={(event) => setStatus(event.target.value as LibraryStatus)}>
              {libraryStatuses.map((libraryStatus) => (
                <option key={libraryStatus} value={libraryStatus}>
                  {libraryStatus}
                </option>
              ))}
            </select>
          </label>
          <label>
            Rating
            <input
              value={rating}
              onChange={(event) => setRating(event.target.value)}
              min={1}
              max={10}
              type="number"
              placeholder="1-10"
            />
          </label>
          <label className="notes-field">
            Notes
            <textarea value={notes} onChange={(event) => setNotes(event.target.value)} rows={3} />
          </label>
          {error && <p className="error compact-message">{error}</p>}
          <div className="inline-actions edit-actions">
            <button type="submit" disabled={loading}>
              {loading ? 'Saving...' : 'Save'}
            </button>
            <button type="button" onClick={cancel} disabled={loading}>
              Cancel
            </button>
          </div>
        </form>
      )}
    </article>
  );
}

function CoverImage({ src, title }: { src?: string | null; title: string }) {
  if (!src) {
    return <div className="cover placeholder" aria-label={`${title} cover`} />;
  }

  return <img className="cover" src={src} alt={`${title} cover`} loading="lazy" />;
}
