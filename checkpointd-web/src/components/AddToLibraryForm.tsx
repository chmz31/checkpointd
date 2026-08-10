import { FormEvent, useState } from 'react';
import { libraryStatuses } from '../constants';
import {
  dateOrderValidationMessage,
  optionalDate,
  optionalNotes,
} from '../formHelpers';
import type { LibraryStatus } from '../types';

export type AddToLibraryFormValues = {
  status: LibraryStatus;
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
  const [notes, setNotes] = useState('');
  const [startedAt, setStartedAt] = useState('');
  const [completedAt, setCompletedAt] = useState('');
  const dateError = dateOrderValidationMessage(startedAt, completedAt);

  function clearFeedback() {
    onChange?.();
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (dateError) {
      return;
    }

    await onSubmit({
      status,
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
      {(dateError || error) && <p className="error compact-message">{dateError || error}</p>}
      <div className="inline-actions direct-add-actions">
        <button type="submit" disabled={submitting || Boolean(dateError)}>
          {submitting ? submittingLabel : submitLabel}
        </button>
      </div>
    </form>
  );
}
