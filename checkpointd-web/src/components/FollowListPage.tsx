import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api';
import { userProfilePath } from '../routePaths';
import type { PaginatedResponse, UserSummary } from '../types';

export function FollowListPage({ mode }: { mode: 'followers' | 'following' }) {
  const { username } = useParams();
  const [users, setUsers] = useState<PaginatedResponse<UserSummary> | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!username) return;
    setLoading(true);
    setError(null);
    const request = mode === 'followers' ? api.getFollowers(username, page, 20) : api.getFollowing(username, page, 20);
    request
      .then(setUsers)
      .catch((caught) => setError(caught instanceof Error ? caught.message : 'Could not load list'))
      .finally(() => setLoading(false));
  }, [username, page, mode]);

  const heading = mode === 'followers' ? `Followers of @${username}` : `@${username} is following`;
  const emptyMessage = mode === 'followers' ? 'No followers yet.' : 'Not following anyone yet.';

  return (
    <section className="panel review-page">
      <div className="section-heading">
        <h2>{heading}</h2>
        {username && (
          <Link className="nav-link button-small" to={userProfilePath(username)}>
            Back to profile
          </Link>
        )}
      </div>
      {error && (
        <div className="empty-state catalog-empty">
          <h3>Not available</h3>
          <p>{error}</p>
        </div>
      )}
      {loading && <p className="muted">Loading...</p>}
      {!loading && !error && users?.content.length === 0 && (
        <div className="empty-state catalog-empty">
          <h3>Nothing here yet</h3>
          <p>{emptyMessage}</p>
        </div>
      )}
      <div className="review-list">
        {users?.content.map((user) => (
          <Link className="list-card" key={user.username} to={userProfilePath(user.username)}>
            <h3>{user.displayName || user.username}</h3>
            <p className="muted">@{user.username}</p>
          </Link>
        ))}
      </div>
      {users && users.totalPages > 1 && (
        <div className="pagination-controls">
          <button onClick={() => setPage((current) => Math.max(current - 1, 0))} disabled={users.first}>
            Previous
          </button>
          <span className="muted">Page {users.page + 1} of {users.totalPages}</span>
          <button onClick={() => setPage((current) => current + 1)} disabled={users.last}>
            Next
          </button>
        </div>
      )}
    </section>
  );
}
