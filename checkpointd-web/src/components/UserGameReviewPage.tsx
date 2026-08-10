import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api';
import { gamePath, userProfilePath } from '../routePaths';
import type { Review } from '../types';
import { CoverImage } from './CoverImage';
import { ReviewCard } from './ReviewCard';

export function UserGameReviewPage() {
  const { username, gameId } = useParams();
  const [review, setReview] = useState<Review | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!username || !gameId) return;
    setLoading(true);
    setError(null);
    api
      .getPublicUserGameReview(username, gameId)
      .then(setReview)
      .catch((caught) => setError(caught instanceof Error ? caught.message : 'Review not found'))
      .finally(() => setLoading(false));
  }, [username, gameId]);

  if (loading) {
    return <p className="muted">Loading review...</p>;
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
      <ReviewCard review={review} showGame={false} />
    </section>
  );
}
