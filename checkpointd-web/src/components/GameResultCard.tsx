import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api';
import type { ExternalGameSearchResult } from '../types';
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
  const [error, setError] = useState<string | null>(null);

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
        <CoverImage src={result.coverUrl} title={result.title} />
        <div>
          <h3>{result.title}</h3>
          <p className="muted">
            {[result.slug, result.releaseDate].filter(Boolean).join(' | ') || result.provider}
          </p>
          <GameMetadata summary={result.summary} genres={result.genres} platforms={result.platforms} />
        </div>
        <div className="result-actions">
          <button className="button-ghost button-small" onClick={viewGamePage} disabled={viewing}>
            {viewing ? 'Opening...' : 'View game page'}
          </button>
          <button onClick={() => setAdding((current) => !current)}>
            {adding ? 'Close' : 'Add to Library'}
          </button>
        </div>
      </div>
      {error && <p className="error compact-message card-message">{error}</p>}
      {adding && <AddSearchResultToLibrary result={result} onAdded={onAdded} />}
    </article>
  );
}
