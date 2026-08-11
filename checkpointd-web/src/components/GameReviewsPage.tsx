import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api';
import type { CurrentUser, LikeStatus, PaginatedResponse, Review } from '../types';
import { ReviewCard } from './ReviewCard';

export function GameReviewsPage({ currentUser }: { currentUser: CurrentUser | null }) {
  const { gameId } = useParams();
  const [reviews, setReviews] = useState<PaginatedResponse<Review> | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!gameId) return;
    setLoading(true);
    setError(null);
    api
      .getGameReviews(gameId, page, 10)
      .then(setReviews)
      .catch((caught) => setError(caught instanceof Error ? caught.message : 'Could not load reviews'))
      .finally(() => setLoading(false));
  }, [gameId, page]);

  function updateReviewLike(reviewId: string, status: LikeStatus) {
    setReviews((current) =>
      current
        ? {
            ...current,
            content: current.content.map((review) =>
              review.id === reviewId ? { ...review, liked: status.liked, likeCount: status.likeCount } : review,
            ),
          }
        : current,
    );
  }

  return (
    <section className="panel review-page">
      <div className="section-heading">
        <h2>Game reviews</h2>
        <Link className="nav-link button-small" to={gameId ? `/games/${gameId}` : '/library'}>
          Back to game
        </Link>
      </div>
      {error && <p className="error compact-message">{error}</p>}
      {loading && <p className="muted">Loading reviews...</p>}
      {!loading && reviews?.content.length === 0 && (
        <div className="empty-state catalog-empty">
          <h3>No public reviews yet</h3>
          <p>Public reviews for this game will appear here.</p>
        </div>
      )}
      <div className="review-list">
        {reviews?.content.map((review) => (
          <ReviewCard
            key={review.id}
            review={review}
            currentUsername={currentUser?.username ?? null}
            onLikeChange={(status) => updateReviewLike(review.id, status)}
          />
        ))}
      </div>
      {reviews && reviews.totalPages > 1 && (
        <div className="pagination-controls">
          <button onClick={() => setPage((current) => Math.max(current - 1, 0))} disabled={reviews.first}>
            Previous
          </button>
          <span className="muted">Page {reviews.page + 1} of {reviews.totalPages}</span>
          <button onClick={() => setPage((current) => current + 1)} disabled={reviews.last}>
            Next
          </button>
        </div>
      )}
    </section>
  );
}
