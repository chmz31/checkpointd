import { useState } from 'react';
import type { ExternalGameSearchResult } from '../types';
import { AddSearchResultToLibrary } from './AddSearchResultToLibrary';
import { CoverImage } from './CoverImage';

export function GameResultCard({
  result,
  onAdded,
}: {
  result: ExternalGameSearchResult;
  onAdded: () => void;
}) {
  const [adding, setAdding] = useState(false);

  return (
    <article className="search-result-card">
      <div className="game-row search-result-summary">
        <CoverImage src={result.coverUrl} title={result.title} />
        <div>
          <h3>{result.title}</h3>
          <p className="muted">
            {[result.slug, result.releaseDate].filter(Boolean).join(' | ') || result.provider}
          </p>
        </div>
        <div className="result-actions">
          <button onClick={() => setAdding((current) => !current)}>
            {adding ? 'Close' : 'Add to Library'}
          </button>
        </div>
      </div>
      {adding && <AddSearchResultToLibrary result={result} onAdded={onAdded} />}
    </article>
  );
}
