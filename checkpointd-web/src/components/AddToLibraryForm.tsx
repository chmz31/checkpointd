import { FormEvent, useState } from 'react';
import { libraryStatuses } from '../constants';
import {
  optionalDate,
  optionalNotes,
  optionalRating,
  ratingValidationMessage,
} from '../formHelpers';
import type { LibraryStatus } from '../types';

export type AddToLibraryFormValues = {
  status: LibraryStatus;
  rating: number | null;
  notes: string | null;
  startedAt: string | null;
  completedAt: string | null;
};

export function AddToLibraryForm({
  onSubmit,
  submitting,
  submitLabel,
  submittingLabel = 'Adding...',
  className = 'direct-add-form',
  message,
  error,
  onChange,
}: {
  onSubmit: (values: AddToLibraryFormValues) => Promise<void>;
  submitting: boolean;
  submitLabel: string;
  submittingLabel?: string;
  className?: string;
  message?: string | null;
  error?: string | null;
  onChange?: () => void;
}) {
  const [status, setStatus] = useState<LibraryStatus>('BACKLOG');
  const [rating, setRating] = useState('');
  const [notes, setNotes] = useState('');
  const [startedAt, setStartedAt] = useState('');
  const [completedAt, setCompletedAt] = useState('');
  const ratingError = ratingValidationMessage(rating);

  function clearFeedback() {
    onChange?.();
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (ratingError) {
      return;
    }

    await onSubmit({
      status,
      rating: optionalRating(rating),
      notes: optionalNotes(notes),
      startedAt: optionalDate(startedAt),
      completedAt: optionalDate(completedAt),
    });
  }

  return (
    <form className={className} onSubmit={submit}>
      <label>
        Status
        <select
          value={status}
          onChange={(event) => {
            setStatus(event.target.value as LibraryStatus);
            clearFeedback();
          }}
        >
          {libraryStatuses.map((libraryStatus) => (
            <option key={libraryStatus} value={libraryStatus}>
              {libraryStatus}
            </option>
          ))}
        </select>
      </label>
      <label>
        Rating
        <input
          value={rating}
          onChange={(event) => {
            setRating(event.target.value);
            clearFeedback();
          }}
          min={1}
          max={10}
          type="number"
          placeholder="1-10"
          step={1}
        />
      </label>
      <label>
        Started
        <input
          value={startedAt}
          onChange={(event) => {
            setStartedAt(event.target.value);
            clearFeedback();
          }}
          type="date"
        />
      </label>
      <label>
        Completed
        <input
          value={completedAt}
          onChange={(event) => {
            setCompletedAt(event.target.value);
            clearFeedback();
          }}
          type="date"
        />
      </label>
      <label className="notes-field">
        Notes
        <textarea
          value={notes}
          onChange={(event) => {
            setNotes(event.target.value);
            clearFeedback();
          }}
          rows={3}
        />
      </label>
      {message && <p className="success compact-message">{message}</p>}
      {(ratingError || error) && <p className="error compact-message">{ratingError || error}</p>}
      <div className="inline-actions direct-add-actions">
        <button type="submit" disabled={submitting || Boolean(ratingError)}>
          {submitting ? submittingLabel : submitLabel}
        </button>
      </div>
    </form>
  );
}
