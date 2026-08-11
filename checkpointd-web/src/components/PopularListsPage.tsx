import { useEffect, useState } from 'react';
import { api } from '../api';
import type { GameList, PaginatedResponse } from '../types';
import { ListCard } from './ListCard';

export function PopularListsPage() {
  const [lists, setLists] = useState<PaginatedResponse<GameList> | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setLoading(true);
    setError(null);
    api
      .getPopularLists(page, 20)
      .then(setLists)
      .catch((caught) => setError(caught instanceof Error ? caught.message : 'Could not load popular lists'))
      .finally(() => setLoading(false));
  }, [page]);

  return (
    <section className="panel review-page">
      <div className="section-heading">
        <h2>Popular lists</h2>
      </div>
      {error && <p className="error compact-message">{error}</p>}
      {loading && <p className="muted">Loading lists...</p>}
      {!loading && lists?.content.length === 0 && (
        <div className="empty-state catalog-empty">
          <h3>No popular lists yet</h3>
          <p>Once public lists start getting likes, the most liked ones will show up here.</p>
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
