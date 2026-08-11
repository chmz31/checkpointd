import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api';
import { formatDate } from '../dateUtils';
import { canonicalGamePath } from '../routePaths';
import type { PaginatedResponse, ReportedListComment, ReportedReviewComment } from '../types';

export function AdminReportedCommentsPage() {
  return (
    <section className="panel">
      <div className="section-heading">
        <h2>Reported comments</h2>
      </div>
      <ReportedListComments />
      <ReportedReviewComments />
    </section>
  );
}

function ReportedListComments() {
  const [comments, setComments] = useState<PaginatedResponse<ReportedListComment> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    load();
  }, []);

  function load() {
    setLoading(true);
    setError(null);
    api
      .getReportedListComments(0, 20)
      .then(setComments)
      .catch((caught) => setError(caught instanceof Error ? caught.message : 'Could not load reported list comments'))
      .finally(() => setLoading(false));
  }

  async function remove(comment: ReportedListComment) {
    setError(null);
    try {
      await api.deleteListComment(comment.listId, comment.id);
      setComments((current) =>
        current ? { ...current, content: current.content.filter((item) => item.id !== comment.id) } : current,
      );
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Could not delete comment');
    }
  }

  return (
    <div className="callout-section">
      <h3>List comments</h3>
      {error && <p className="error compact-message">{error}</p>}
      {loading && <p className="muted">Loading...</p>}
      {!loading && comments?.content.length === 0 && (
        <div className="empty-state catalog-empty">
          <h3>No reported list comments</h3>
          <p>Comments flagged by users will show up here.</p>
        </div>
      )}
      {comments?.content.map((comment) => (
        <div key={comment.id} className="comment-item">
          <p className="muted">
            <strong>{comment.displayName || comment.username}</strong> on <strong>{comment.listName}</strong> ·{' '}
            {formatDate(comment.createdAt)} · {comment.reportCount} {comment.reportCount === 1 ? 'report' : 'reports'}
          </p>
          <p className="review-text">{comment.body}</p>
          <div className="inline-actions">
            <button className="button-ghost button-small" onClick={() => remove(comment)}>
              Delete
            </button>
          </div>
        </div>
      ))}
    </div>
  );
}

function ReportedReviewComments() {
  const [comments, setComments] = useState<PaginatedResponse<ReportedReviewComment> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    load();
  }, []);

  function load() {
    setLoading(true);
    setError(null);
    api
      .getReportedReviewComments(0, 20)
      .then(setComments)
      .catch((caught) => setError(caught instanceof Error ? caught.message : 'Could not load reported review comments'))
      .finally(() => setLoading(false));
  }

  async function remove(comment: ReportedReviewComment) {
    setError(null);
    try {
      await api.deleteReviewComment(comment.reviewId, comment.id);
      setComments((current) =>
        current ? { ...current, content: current.content.filter((item) => item.id !== comment.id) } : current,
      );
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Could not delete comment');
    }
  }

  return (
    <div className="callout-section">
      <h3>Review comments</h3>
      {error && <p className="error compact-message">{error}</p>}
      {loading && <p className="muted">Loading...</p>}
      {!loading && comments?.content.length === 0 && (
        <div className="empty-state catalog-empty">
          <h3>No reported review comments</h3>
          <p>Comments flagged by users will show up here.</p>
        </div>
      )}
      {comments?.content.map((comment) => (
        <div key={comment.id} className="comment-item">
          <p className="muted">
            <strong>{comment.displayName || comment.username}</strong> on{' '}
            <Link className="inline-link" to={canonicalGamePath(comment.gameId, undefined, comment.gameTitle)}>
              {comment.gameTitle}
            </Link>{' '}
            · {formatDate(comment.createdAt)} · {comment.reportCount} {comment.reportCount === 1 ? 'report' : 'reports'}
          </p>
          <p className="review-text">{comment.body}</p>
          <div className="inline-actions">
            <button className="button-ghost button-small" onClick={() => remove(comment)}>
              Delete
            </button>
          </div>
        </div>
      ))}
    </div>
  );
}
