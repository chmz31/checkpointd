import { FormEvent, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api';
import { userProfilePath } from '../routePaths';
import type { ExternalGameSearchResult, GameList, PaginatedResponse, UserSummary } from '../types';
import { GameResultCard } from './GameResultCard';
import { ListCard } from './ListCard';

type Category = 'games' | 'lists' | 'members';

export function SearchView({ onLibraryChange }: { onLibraryChange: () => void }) {
  const [category, setCategory] = useState<Category>('games');
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(0);
  const [gameResults, setGameResults] = useState<ExternalGameSearchResult[]>([]);
  const [listResults, setListResults] = useState<PaginatedResponse<GameList> | null>(null);
  const [memberResults, setMemberResults] = useState<PaginatedResponse<UserSummary> | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [hasSearched, setHasSearched] = useState(false);

  function changeCategory(nextCategory: Category) {
    if (nextCategory === category) return;
    setCategory(nextCategory);
    setPage(0);
    setGameResults([]);
    setListResults(null);
    setMemberResults(null);
    setHasSearched(false);
    setError(null);
  }

  async function runSearch(targetPage: number) {
    if (!query.trim()) return;

    setLoading(true);
    setError(null);
    setHasSearched(true);

    try {
      if (category === 'games') {
        setGameResults(await api.searchExternalGames(query.trim()));
      } else if (category === 'lists') {
        setListResults(await api.searchLists(query.trim(), targetPage));
      } else {
        setMemberResults(await api.searchUsers(query.trim(), targetPage));
      }
      setPage(targetPage);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Search failed');
    } finally {
      setLoading(false);
    }
  }

  function search(event: FormEvent) {
    event.preventDefault();
    runSearch(0);
  }

  const placeholder =
    category === 'games' ? 'Search for a game' : category === 'lists' ? 'Search for a list' : 'Search for a member';
  const emptyTitle =
    category === 'games' ? 'No games found' : category === 'lists' ? 'No lists found' : 'No members found';
  const emptyBody =
    category === 'games'
      ? 'Try a different title or a shorter search phrase.'
      : category === 'lists'
        ? 'Try a different list name.'
        : 'Try a different username or display name.';
  const currentPageData = category === 'lists' ? listResults : category === 'members' ? memberResults : null;
  const hasResults =
    category === 'games'
      ? gameResults.length > 0
      : category === 'lists'
        ? (listResults?.content.length ?? 0) > 0
        : (memberResults?.content.length ?? 0) > 0;

  return (
    <section className="panel">
      <div className="section-heading">
        <h2>Search</h2>
        {loading && <span className="muted">Loading...</span>}
      </div>
      <div className="inline-actions">
        <button
          type="button"
          className={`profile-tab-link${category === 'games' ? ' active' : ''}`}
          onClick={() => changeCategory('games')}
        >
          Games
        </button>
        <button
          type="button"
          className={`profile-tab-link${category === 'lists' ? ' active' : ''}`}
          onClick={() => changeCategory('lists')}
        >
          Lists
        </button>
        <button
          type="button"
          className={`profile-tab-link${category === 'members' ? ' active' : ''}`}
          onClick={() => changeCategory('members')}
        >
          Members
        </button>
      </div>
      <form className="search-form" onSubmit={search}>
        <input
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder={placeholder}
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
          <p>Search games, lists, and members to get started.</p>
        </div>
      )}
      {!loading && !error && hasSearched && !hasResults && (
        <div className="empty-state catalog-empty">
          <h3>{emptyTitle}</h3>
          <p>{emptyBody}</p>
        </div>
      )}
      {category === 'games' && (
        <div className="result-list">
          {gameResults.map((result) => (
            <GameResultCard
              key={`${result.provider}-${result.externalId}`}
              result={result}
              onAdded={onLibraryChange}
            />
          ))}
        </div>
      )}
      {category === 'lists' && (
        <div className="result-list">
          {listResults?.content.map((list) => <ListCard key={list.id} list={list} />)}
        </div>
      )}
      {category === 'members' && (
        <div className="result-list">
          {memberResults?.content.map((user) => (
            <Link className="list-card" key={user.username} to={userProfilePath(user.username)}>
              <h3>{user.displayName || user.username}</h3>
              <p className="muted">@{user.username}</p>
            </Link>
          ))}
        </div>
      )}
      {currentPageData && currentPageData.totalPages > 1 && (
        <div className="pagination-controls">
          <button onClick={() => runSearch(Math.max(page - 1, 0))} disabled={currentPageData.first}>
            Previous
          </button>
          <span className="muted">
            Page {currentPageData.page + 1} of {currentPageData.totalPages}
          </span>
          <button onClick={() => runSearch(page + 1)} disabled={currentPageData.last}>
            Next
          </button>
        </div>
      )}
    </section>
  );
}
