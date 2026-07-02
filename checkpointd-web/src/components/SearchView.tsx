import { FormEvent, useState } from 'react';
import { api } from '../api';
import type { ExternalGameSearchResult, Game } from '../types';
import { AddToLibraryForm } from './AddToLibraryForm';
import { GameResultCard } from './GameResultCard';

export function SearchView({ onLibraryChange }: { onLibraryChange: () => void }) {
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
