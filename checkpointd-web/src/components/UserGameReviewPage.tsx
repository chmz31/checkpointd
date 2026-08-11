import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api';
import { gamePath, userProfilePath } from '../routePaths';
import type { CurrentUser, Review, ReviewRequest } from '../types';
import { CoverImage } from './CoverImage';
import { ReviewCard } from './ReviewCard';
import { ReviewForm } from './ReviewForm';

export function UserGameReviewPage({ currentUser }: { currentUser: CurrentUser | null }) {
  const { username, gameId } = useParams();
  const isOwnReview = Boolean(currentUser && username && currentUser.username === username);
  const [review, setReview] = useState<Review | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [deleted, setDeleted] = useState(false);

  useEffect(() => {
    if (!username || !gameId) return;
    setLoading(true);
    setError(null);
    setDeleted(false);

    const request = isOwnReview ? api.getMyGameReview(gameId) : api.getPublicUserGameReview(username, gameId);
    request
      .then(setReview)
      .catch((caught) => setError(caught instanceof Error ? caught.message : 'Review not found'))
      .finally(() => setLoading(false));
  }, [username, gameId, isOwnReview]);

  async function saveReview(request: ReviewRequest) {
    if (!gameId) return;
    setSaving(true);
    setError(null);

    try {
      setReview(await api.saveMyGameReview(gameId, request));
      setEditing(false);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Could not save review');
    } finally {
      setSaving(false);
    }
  }

  async function deleteReview() {
    if (!gameId) return;
    setDeleting(true);
    setError(null);

    try {
      await api.deleteMyGameReview(gameId);
      setReview(null);
      setDeleted(true);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Could not delete review');
    } finally {
      setDeleting(false);
    }
  }

  if (loading) {
    return <p className="muted">Loading review...</p>;
  }

  if (deleted) {
    return (
      <section className="panel">
        <div className="empty-state catalog-empty">
          <h3>Review deleted</h3>
          <p>Your review has been removed.</p>
        </div>
      </section>
    );
  }

  if (!review) {
    return (
      <section className="panel">
        <div className="empty-state catalog-empty">
          <h3>Review not found</h3>
          <p>{error || 'This review is private or does not exist.'}</p>
        </div>
      </section>
    );
  }

  return (
    <section className="panel review-page">
      <div className="review-detail-hero">
        <Link to={gamePath({ id: review.gameId, slug: review.gameSlug, title: review.gameTitle })}>
          <CoverImage src={review.gameCoverUrl} title={review.gameTitle} />
        </Link>
        <div>
          <p className="muted">
            Review by <Link className="inline-link" to={userProfilePath(review.username)}>@{review.username}</Link>
          </p>
          <h2>{review.gameTitle}</h2>
        </div>
      </div>
      {editing ? (
        <ReviewForm review={review} submitting={saving} onSubmit={saveReview} />
      ) : (
        <>
          <ReviewCard
            review={review}
            showGame={false}
            currentUsername={currentUser?.username ?? null}
            onLikeChange={(status) =>
              setReview((current) => (current ? { ...current, liked: status.liked, likeCount: status.likeCount } : current))
            }
          />
          {isOwnReview && (
            <div className="inline-actions">
              <button className="button-ghost button-small" onClick={() => setEditing(true)}>
                Edit review
              </button>
              <button className="button-ghost button-small" onClick={deleteReview} disabled={deleting}>
                {deleting ? 'Deleting...' : 'Delete review'}
              </button>
            </div>
          )}
        </>
      )}
      {error && <p className="error compact-message">{error}</p>}
    </section>
  );
}
