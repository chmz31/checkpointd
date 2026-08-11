import { FormEvent, useEffect, useState } from 'react';
import { api } from '../api';
import { formatDate } from '../dateUtils';
import type { Comment, CurrentUser, LikeStatus, PaginatedResponse } from '../types';
import { LikeButton } from './LikeButton';

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

  function updateComment(commentId: string, updater: (comment: Comment) => Comment) {
    setComments((current) => {
      if (!current) return current;
      return {
        ...current,
        content: current.content.map((comment) => {
          if (comment.id === commentId) {
            return updater(comment);
          }
          if (comment.replies.some((reply) => reply.id === commentId)) {
            return { ...comment, replies: comment.replies.map((reply) => (reply.id === commentId ? updater(reply) : reply)) };
          }
          return comment;
        }),
      };
    });
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

  async function submitReply(parentId: string, replyBody: string) {
    const created =
      targetType === 'list'
        ? await api.addListComment(targetId, { body: replyBody, parentId })
        : await api.addReviewComment(targetId, { body: replyBody, parentId });
    setComments((current) =>
      current
        ? { ...current, content: current.content.map((comment) => (comment.id === parentId ? { ...comment, replies: [...comment.replies, created] } : comment)) }
        : current,
    );
  }

  async function deleteComment(commentId: string) {
    setError(null);

    try {
      if (targetType === 'list') {
        await api.deleteListComment(targetId, commentId);
      } else {
        await api.deleteReviewComment(targetId, commentId);
      }
      setComments((current) => {
        if (!current) return current;
        const isTopLevel = current.content.some((comment) => comment.id === commentId);
        if (isTopLevel) {
          return {
            ...current,
            content: current.content.filter((comment) => comment.id !== commentId),
            totalElements: Math.max(current.totalElements - 1, 0),
          };
        }
        return {
          ...current,
          content: current.content.map((comment) => ({
            ...comment,
            replies: comment.replies.filter((reply) => reply.id !== commentId),
          })),
          totalElements: Math.max(current.totalElements - 1, 0),
        };
      });
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
        <CommentItem
          key={comment.id}
          comment={comment}
          targetType={targetType}
          currentUser={currentUser}
          isAdmin={isAdmin}
          isReply={false}
          reportedIds={reportedIds}
          onDelete={deleteComment}
          onReport={reportComment}
          onLikeChange={(commentId, status) =>
            updateComment(commentId, (current) => ({ ...current, liked: status.liked, likeCount: status.likeCount }))
          }
          onReply={submitReply}
        />
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

function CommentItem({
  comment,
  targetType,
  currentUser,
  isAdmin,
  isReply,
  reportedIds,
  onDelete,
  onReport,
  onLikeChange,
  onReply,
}: {
  comment: Comment;
  targetType: 'list' | 'review';
  currentUser: CurrentUser | null;
  isAdmin: boolean;
  isReply: boolean;
  reportedIds: Set<string>;
  onDelete: (commentId: string) => void;
  onReport: (commentId: string) => void;
  onLikeChange: (commentId: string, status: LikeStatus) => void;
  onReply: (parentId: string, body: string) => Promise<void>;
}) {
  const [replying, setReplying] = useState(false);
  const [replyBody, setReplyBody] = useState('');
  const [submittingReply, setSubmittingReply] = useState(false);
  const [replyError, setReplyError] = useState<string | null>(null);
  const reported = reportedIds.has(comment.id);

  async function submitReply(event: FormEvent) {
    event.preventDefault();
    const trimmed = replyBody.trim();
    if (!trimmed) return;

    setSubmittingReply(true);
    setReplyError(null);

    try {
      await onReply(comment.id, trimmed);
      setReplyBody('');
      setReplying(false);
    } catch (caught) {
      setReplyError(caught instanceof Error ? caught.message : 'Could not post reply');
    } finally {
      setSubmittingReply(false);
    }
  }

  return (
    <div className={isReply ? 'comment-item comment-reply' : 'comment-item'}>
      <p className="muted">
        <strong>{comment.displayName || comment.username}</strong> · {formatDate(comment.createdAt)}
      </p>
      <p className="review-text">{comment.body}</p>
      <div className="inline-actions">
        <LikeButton
          liked={comment.liked}
          likeCount={comment.likeCount}
          onLike={() => (targetType === 'list' ? api.likeListComment(comment.id) : api.likeReviewComment(comment.id))}
          onUnlike={() => (targetType === 'list' ? api.unlikeListComment(comment.id) : api.unlikeReviewComment(comment.id))}
          onChange={(status) => onLikeChange(comment.id, status)}
        />
        {(comment.owner || isAdmin) && (
          <button className="button-ghost button-small" onClick={() => onDelete(comment.id)}>
            Delete
          </button>
        )}
        {currentUser && !comment.owner && (
          <button className="button-ghost button-small" onClick={() => onReport(comment.id)} disabled={reported}>
            {reported ? 'Reported' : 'Report'}
          </button>
        )}
        {!isReply && currentUser && (
          <button className="button-ghost button-small" onClick={() => setReplying((current) => !current)}>
            {replying ? 'Cancel' : 'Reply'}
          </button>
        )}
      </div>

      {replying && (
        <form className="comment-form" onSubmit={submitReply}>
          <textarea
            value={replyBody}
            onChange={(event) => setReplyBody(event.target.value)}
            placeholder="Write a reply..."
            maxLength={2000}
          />
          <button type="submit" disabled={submittingReply || !replyBody.trim()}>
            {submittingReply ? 'Posting...' : 'Post reply'}
          </button>
          {replyError && <p className="error compact-message">{replyError}</p>}
        </form>
      )}

      {!isReply && comment.replies.length > 0 && (
        <div className="comment-replies">
          {comment.replies.map((reply) => (
            <CommentItem
              key={reply.id}
              comment={reply}
              targetType={targetType}
              currentUser={currentUser}
              isAdmin={isAdmin}
              isReply
              reportedIds={reportedIds}
              onDelete={onDelete}
              onReport={onReport}
              onLikeChange={onLikeChange}
              onReply={onReply}
            />
          ))}
        </div>
      )}
    </div>
  );
}
