import type { ExternalGameSearchResult } from '../types';
import { CoverImage } from './CoverImage';

export function GameResultCard({
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
