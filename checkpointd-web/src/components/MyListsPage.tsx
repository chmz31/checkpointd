import { useEffect, useState } from 'react';
import { api } from '../api';
import type { GameListRequest, PaginatedResponse, GameList } from '../types';
import { ListCard } from './ListCard';
import { ListForm } from './ListForm';

export function MyListsPage() {
  const [lists, setLists] = useState<PaginatedResponse<GameList> | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [creating, setCreating] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    setLoading(true);
    setError(null);
    api
      .getMyLists(page, 20)
      .then(setLists)
      .catch((caught) => setError(caught instanceof Error ? caught.message : 'Could not load lists'))
      .finally(() => setLoading(false));
  }, [page]);

  async function createList(request: GameListRequest) {
    setSaving(true);
    setError(null);

    try {
      await api.createList(request);
      setCreating(false);
      setPage(0);
      setLists(await api.getMyLists(0, 20));
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Could not create list');
    } finally {
      setSaving(false);
    }
  }

  return (
    <section className="panel review-page">
      <div className="section-heading">
        <h2>Your lists</h2>
        <button className="button-ghost button-small" onClick={() => setCreating((current) => !current)}>
          {creating ? 'Close' : 'New list'}
        </button>
      </div>
      {creating && (
        <div className="callout-section">
          <ListForm submitting={saving} onSubmit={createList} />
        </div>
      )}
      {error && <p className="error compact-message">{error}</p>}
      {loading && <p className="muted">Loading lists...</p>}
      {!loading && lists?.content.length === 0 && (
        <div className="empty-state catalog-empty">
          <h3>No lists yet</h3>
          <p>Create a list to start curating games, public or private.</p>
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
