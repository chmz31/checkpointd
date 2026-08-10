import { useState } from 'react';
import { Link } from 'react-router-dom';
import { formatDate } from '../dateUtils';
import { gamePath, userGameReviewPath, userProfilePath } from '../routePaths';
import type { Review } from '../types';
import { CoverImage } from './CoverImage';

export function ReviewCard({ review, showGame = true }: { review: Review; showGame?: boolean }) {
  const [revealed, setRevealed] = useState(!review.containsSpoilers);
  const authorName = review.displayName || review.username;

  return (
    <article className="review-card">
      {showGame && (
        <Link className="review-cover-link" to={gamePath({ id: review.gameId, slug: review.gameSlug, title: review.gameTitle })}>
          <CoverImage src={review.gameCoverUrl} title={review.gameTitle} />
        </Link>
      )}
      <div className="review-body">
        <div className="review-heading">
          <div>
            {showGame && <h3>{review.gameTitle}</h3>}
            <p className="muted">
              by <Link className="inline-link" to={userProfilePath(review.username)}>{authorName}</Link>
              {' '}· {formatDate(review.updatedAt)}
            </p>
          </div>
          {review.rating && <span className="rating-badge">{review.rating}/10</span>}
        </div>
        {review.containsSpoilers && !revealed ? (
          <div className="spoiler-block">
            <p>This review contains spoilers.</p>
            <button className="button-ghost button-small" onClick={() => setRevealed(true)}>
              Reveal spoilers
            </button>
          </div>
        ) : (
          <p className="review-text">{review.body}</p>
        )}
        <div className="inline-actions">
          {review.containsSpoilers && <span className="status-badge">Spoilers</span>}
          <Link
            className="inline-link"
            to={userGameReviewPath(review.username, {
              id: review.gameId,
              slug: review.gameSlug,
              title: review.gameTitle,
            })}
          >
            View review
          </Link>
        </div>
      </div>
    </article>
  );
}
