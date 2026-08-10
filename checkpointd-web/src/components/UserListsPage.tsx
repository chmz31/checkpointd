import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api';
import { userProfilePath } from '../routePaths';
import type { GameList, PaginatedResponse } from '../types';
import { ListCard } from './ListCard';

export function UserListsPage() {
  const { username } = useParams();
  const [lists, setLists] = useState<PaginatedResponse<GameList> | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!username) return;
    setLoading(true);
    setError(null);
    api
      .getUserLists(username, page, 20)
      .then(setLists)
      .catch((caught) => setError(caught instanceof Error ? caught.message : 'Could not load lists'))
      .finally(() => setLoading(false));
  }, [username, page]);

  return (
    <section className="panel review-page">
      <div className="section-heading">
        <h2>{username ? `Lists by @${username}` : 'Lists'}</h2>
        {username && (
          <Link className="nav-link button-small" to={userProfilePath(username)}>
            Back to profile
          </Link>
        )}
      </div>
      {error && (
        <div className="empty-state catalog-empty">
          <h3>Lists not available</h3>
          <p>{error}</p>
        </div>
      )}
      {loading && <p className="muted">Loading lists...</p>}
      {!loading && !error && lists?.content.length === 0 && (
        <div className="empty-state catalog-empty">
          <h3>No public lists yet</h3>
          <p>This profile has not shared any public lists.</p>
        </div>
      )}
      <div className="review-list">
        {lists?.content.map((list) => (
          <ListCard key={list.id} list={list} />
        ))}
      </div>
      {lists && lists.totalPages > 1 && (
        <div className="pagination-controls">
          <button onClick={() => setPage((current) => Math.max(current - 1, 0))} disabled={lists.first}>
            Previous
          </button>
          <span className="muted">Page {lists.page + 1} of {lists.totalPages}</span>
          <button onClick={() => setPage((current) => current + 1)} disabled={lists.last}>
            Next
          </button>
        </div>
      )}
    </section>
  );
}
