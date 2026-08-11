import { FormEvent, useEffect, useState } from 'react';
import { api } from '../api';
import { formatDate } from '../dateUtils';
import type { Comment, CurrentUser, PaginatedResponse } from '../types';

export function CommentSection({
  targetType,
  targetId,
  currentUser,
}: {
  targetType: 'list' | 'review';
  targetId: string;
  currentUser: CurrentUser | null;
}) {
  const [comments, setComments] = useState<PaginatedResponse<Comment> | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [body, setBody] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [reportedIds, setReportedIds] = useState<Set<string>>(new Set());

  const isAdmin = currentUser?.role === 'ADMIN';

  useEffect(() => {
    setPage(0);
    loadComments(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [targetType, targetId]);

  function loadComments(targetPage: number) {
    setLoading(true);
    setError(null);

    const request =
      targetType === 'list' ? api.getListComments(targetId, targetPage, 20) : api.getReviewComments(targetId, targetPage, 20);

    request
      .then(setComments)
      .catch((caught) => setError(caught instanceof Error ? caught.message : 'Could not load comments'))
      .finally(() => setLoading(false));
  }

  function changePage(nextPage: number) {
    setPage(nextPage);
    loadComments(nextPage);
  }

  async function submitComment(event: FormEvent) {
    event.preventDefault();
    const trimmed = body.trim();
    if (!trimmed) return;

    setSubmitting(true);
    setError(null);

    try {
      const created =
        targetType === 'list' ? await api.addListComment(targetId, { body: trimmed }) : await api.addReviewComment(targetId, { body: trimmed });
      setBody('');
      setComments((current) =>
        current
          ? { ...current, content: [created, ...current.content], totalElements: current.totalElements + 1 }
          : current,
      );
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Could not post comment');
    } finally {
      setSubmitting(false);
    }
  }

  async function deleteComment(commentId: string) {
    setError(null);

    try {
      if (targetType === 'list') {
        await api.deleteListComment(targetId, commentId);
      } else {
        await api.deleteReviewComment(targetId, commentId);
      }
      setComments((current) =>
        current
          ? {
              ...current,
              content: current.content.filter((comment) => comment.id !== commentId),
              totalElements: Math.max(current.totalElements - 1, 0),
            }
          : current,
      );
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Could not delete comment');
    }
  }

  async function reportComment(commentId: string) {
    setError(null);

    try {
      if (targetType === 'list') {
        await api.reportListComment(targetId, commentId);
      } else {
        await api.reportReviewComment(targetId, commentId);
      }
      setReportedIds((current) => new Set(current).add(commentId));
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Could not report comment');
    }
  }

  return (
    <section className="callout-section">
      <h3>Comments{comments ? ` (${comments.totalElements})` : ''}</h3>
      {error && <p className="error compact-message">{error}</p>}
      {loading && <p className="muted">Loading comments...</p>}
      {!loading && comments?.content.length === 0 && <p className="muted">No comments yet.</p>}

      {comments?.content.map((comment) => (
        <div key={comment.id} className="comment-item">
          <p className="muted">
            <strong>{comment.displayName || comment.username}</strong> · {formatDate(comment.createdAt)}
          </p>
          <p className="review-text">{comment.body}</p>
          <div className="inline-actions">
            {(comment.owner || isAdmin) && (
              <button className="button-ghost button-small" onClick={() => deleteComment(comment.id)}>
                Delete
              </button>
            )}
            {currentUser && !comment.owner && (
              <button
                className="button-ghost button-small"
                onClick={() => reportComment(comment.id)}
                disabled={reportedIds.has(comment.id)}
              >
                {reportedIds.has(comment.id) ? 'Reported' : 'Report'}
              </button>
            )}
          </div>
        </div>
      ))}

      {comments && comments.totalPages > 1 && (
        <div className="pagination-controls">
          <button onClick={() => changePage(Math.max(page - 1, 0))} disabled={comments.first}>
            Previous
          </button>
          <span className="muted">
            Page {comments.page + 1} of {comments.totalPages}
          </span>
          <button onClick={() => changePage(page + 1)} disabled={comments.last}>
            Next
          </button>
        </div>
      )}

      {currentUser && (
        <form className="comment-form" onSubmit={submitComment}>
          <textarea
            value={body}
            onChange={(event) => setBody(event.target.value)}
            placeholder="Add a comment..."
            maxLength={2000}
          />
          <button type="submit" disabled={submitting || !body.trim()}>
            {submitting ? 'Posting...' : 'Post comment'}
          </button>
        </form>
      )}
    </section>
  );
}
