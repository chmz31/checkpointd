import { FormEvent, useEffect, useState } from 'react';
import type { Review, ReviewRequest, ReviewVisibility } from '../types';

export function ReviewForm({
  review,
  submitting,
  onSubmit,
}: {
  review?: Review | null;
  submitting: boolean;
  onSubmit: (request: ReviewRequest) => Promise<void>;
}) {
  const [rating, setRating] = useState(review?.rating?.toString() || '');
  const [body, setBody] = useState(review?.body || '');
  const [containsSpoilers, setContainsSpoilers] = useState(Boolean(review?.containsSpoilers));
  const [visibility, setVisibility] = useState<ReviewVisibility>(review?.visibility || 'PUBLIC');
  const [clientError, setClientError] = useState<string | null>(null);

  useEffect(() => {
    setRating(review?.rating?.toString() || '');
    setBody(review?.body || '');
    setContainsSpoilers(Boolean(review?.containsSpoilers));
    setVisibility(review?.visibility || 'PUBLIC');
  }, [review]);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const cleanBody = body.trim();
    const parsedRating = rating === '' ? null : Number(rating);

    if (!cleanBody) {
      setClientError('Review text is required.');
      return;
    }
    if (parsedRating != null && (!Number.isInteger(parsedRating) || parsedRating < 1 || parsedRating > 10)) {
      setClientError('Rating must be between 1 and 10.');
      return;
    }

    setClientError(null);
    await onSubmit({
      rating: parsedRating,
      body: cleanBody,
      containsSpoilers,
      visibility,
    });
  }

  return (
    <form className="edit-form review-form" onSubmit={submit}>
      <label>
        Rating
        <input
          type="number"
          min={1}
          max={10}
          value={rating}
          onChange={(event) => {
            setRating(event.target.value);
            setClientError(null);
          }}
          placeholder="1-10"
        />
      </label>
      <label>
        Visibility
        <select value={visibility} onChange={(event) => setVisibility(event.target.value as ReviewVisibility)}>
          <option value="PUBLIC">PUBLIC</option>
          <option value="PRIVATE">PRIVATE</option>
        </select>
      </label>
      <label className="full-width">
        Review
        <textarea
          value={body}
          maxLength={5000}
          rows={6}
          onChange={(event) => {
            setBody(event.target.value);
            setClientError(null);
          }}
          placeholder="Write your public opinion here. Private library notes stay separate."
        />
      </label>
      <label className="checkbox-row full-width">
        <input
          type="checkbox"
          checked={containsSpoilers}
          onChange={(event) => setContainsSpoilers(event.target.checked)}
        />
        Contains spoilers
      </label>
      {clientError && <p className="error compact-message full-width">{clientError}</p>}
      <button type="submit" disabled={submitting}>
        {submitting ? 'Saving...' : review ? 'Save review' : 'Post review'}
      </button>
    </form>
  );
}
