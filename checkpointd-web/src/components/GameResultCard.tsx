import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api, isApiErrorStatus } from '../api';
import type { ExternalGameSearchResult, LibraryEntry } from '../types';
import { AddSearchResultToLibrary } from './AddSearchResultToLibrary';
import { CoverImage } from './CoverImage';
import { GameMetadata } from './GameMetadata';

export function GameResultCard({
  result,
  onAdded,
}: {
  result: ExternalGameSearchResult;
  onAdded: () => void;
}) {
  const navigate = useNavigate();
  const [adding, setAdding] = useState(false);
  const [viewing, setViewing] = useState(false);
  const [libraryEntry, setLibraryEntry] = useState<LibraryEntry | null>(null);
  const [lookupError, setLookupError] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLibraryEntry(null);
    setLookupError(null);

    api
      .getLibraryEntryByExternalGame(result.provider, result.externalId)
      .then((entry) => {
        if (!cancelled) {
          setLibraryEntry(entry);
          setAdding(false);
        }
      })
      .catch((caught) => {
        if (cancelled) return;

        if (!isApiErrorStatus(caught, 404)) {
          const message = caught instanceof Error ? caught.message : '';
          setLookupError(message || 'Could not check library status');
        }
      });

    return () => {
      cancelled = true;
    };
  }, [result.provider, result.externalId]);

  async function viewGamePage() {
    setViewing(true);
    setError(null);

    try {
      const game = await api.importExternalGame(result.provider, result.externalId);
      navigate(`/games/${game.id}`);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Could not open game page');
    } finally {
      setViewing(false);
    }
  }

  return (
    <article className="search-result-card">
      <div className="game-row search-result-summary">
        <div className="cover-link">
          <CoverImage src={result.coverUrl} title={result.title} />
        </div>
        <div className="entry-card-body">
          <h3>{result.title}</h3>
          <p className="muted">
            {[result.slug, result.releaseDate].filter(Boolean).join(' | ') || result.provider}
          </p>
          <GameMetadata summary={result.summary} genres={result.genres} platforms={result.platforms} />
        </div>
        <div className="result-actions">
          {libraryEntry && <span className="user-pill status-badge">In Library</span>}
          <button className="button-ghost button-small" onClick={viewGamePage} disabled={viewing}>
            {viewing ? 'Opening...' : 'View game page'}
          </button>
          {libraryEntry ? (
            <Link className="nav-link button-small" to={`/library/${libraryEntry.id}`}>
              View library entry
            </Link>
          ) : (
            <button onClick={() => setAdding((current) => !current)}>
              {adding ? 'Close' : 'Add to Library'}
            </button>
          )}
        </div>
      </div>
      {error && <p className="error compact-message card-message">{error}</p>}
      {lookupError && <p className="error compact-message card-message">{lookupError}</p>}
      {adding && !libraryEntry && (
        <AddSearchResultToLibrary
          result={result}
          onAdded={(entry) => {
            setLibraryEntry(entry);
            setAdding(false);
            onAdded();
          }}
        />
      )}
    </article>
  );
}
