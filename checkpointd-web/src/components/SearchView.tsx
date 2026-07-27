import { FormEvent, useState } from 'react';
import { api } from '../api';
import type { ExternalGameSearchResult } from '../types';
import { GameResultCard } from './GameResultCard';

export function SearchView({ onLibraryChange }: { onLibraryChange: () => void }) {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<ExternalGameSearchResult[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [hasSearched, setHasSearched] = useState(false);

  async function search(event: FormEvent) {
    event.preventDefault();
    if (!query.trim()) return;

    setLoading(true);
    setError(null);
    setHasSearched(true);

    try {
      setResults(await api.searchExternalGames(query.trim()));
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Search failed');
    } finally {
      setLoading(false);
    }
  }

  return (
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
      {!loading && !error && !hasSearched && (
        <div className="empty-state catalog-empty">
          <h3>Build your library</h3>
          <p>Search the catalog and start tracking the games you play.</p>
        </div>
      )}
      {!loading && !error && hasSearched && results.length === 0 && (
        <div className="empty-state catalog-empty">
          <h3>No games found</h3>
          <p>Try a different title or a shorter search phrase.</p>
        </div>
      )}
      <div className="result-list">
        {results.map((result) => (
          <GameResultCard
            key={`${result.provider}-${result.externalId}`}
            result={result}
            onAdded={onLibraryChange}
          />
        ))}
      </div>
    </section>
  );
}
