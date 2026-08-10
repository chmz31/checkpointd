import { FormEvent, useEffect, useState } from 'react';
import { api } from '../api';
import { libraryStatuses } from '../constants';
import {
  dateInputValue,
  dateOrderValidationMessage,
  optionalDate,
  optionalNotes,
} from '../formHelpers';
import type { LibraryEntry, LibraryStatus } from '../types';

export function LibraryEntryEditForm({
  entry,
  onSaved,
  onCancel,
}: {
  entry: LibraryEntry;
  onSaved: (entry: LibraryEntry) => void;
  onCancel: () => void;
}) {
  const [status, setStatus] = useState<LibraryStatus>(entry.status);
  const [notes, setNotes] = useState(entry.notes || '');
  const [startedAt, setStartedAt] = useState(dateInputValue(entry.startedAt));
  const [completedAt, setCompletedAt] = useState(dateInputValue(entry.completedAt));
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const dateError = dateOrderValidationMessage(startedAt, completedAt);

  useEffect(() => {
    setStatus(entry.status);
    setNotes(entry.notes || '');
    setStartedAt(dateInputValue(entry.startedAt));
    setCompletedAt(dateInputValue(entry.completedAt));
  }, [entry]);

  function clearFeedback() {
    setError(null);
  }

  async function save(event: FormEvent) {
    event.preventDefault();
    if (dateError) {
      setError(dateError);
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const updatedEntry = await api.updateLibraryEntry(entry.id, {
        status,
        notes: optionalNotes(notes),
        startedAt: optionalDate(startedAt),
        completedAt: optionalDate(completedAt),
      });
      onSaved(updatedEntry);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Could not update entry');
    } finally {
      setLoading(false);
    }
  }

  return (
    <form className="edit-form" onSubmit={save}>
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
      {(dateError || error) && <p className="error compact-message">{dateError || error}</p>}
      <div className="inline-actions edit-actions">
        <button type="submit" disabled={loading || Boolean(dateError)}>
          {loading ? 'Saving...' : 'Save'}
        </button>
        <button type="button" onClick={onCancel} disabled={loading}>
          Cancel
        </button>
      </div>
    </form>
  );
}
