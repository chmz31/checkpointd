import { FormEvent, useEffect, useState } from 'react';
import { api } from '../api';
import { libraryStatuses } from '../constants';
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
  const [rating, setRating] = useState(entry.rating ? String(entry.rating) : '');
  const [notes, setNotes] = useState(entry.notes || '');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setStatus(entry.status);
    setRating(entry.rating ? String(entry.rating) : '');
    setNotes(entry.notes || '');
  }, [entry]);

  function clearFeedback() {
    setError(null);
  }

  async function save(event: FormEvent) {
    event.preventDefault();
    setLoading(true);
    setError(null);

    try {
      const updatedEntry = await api.updateLibraryEntry(entry.id, {
        status,
        ...(rating ? { rating: Number(rating) } : {}),
        notes,
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
      {error && <p className="error compact-message">{error}</p>}
      <div className="inline-actions edit-actions">
        <button type="submit" disabled={loading}>
          {loading ? 'Saving...' : 'Save'}
        </button>
        <button type="button" onClick={onCancel} disabled={loading}>
          Cancel
        </button>
      </div>
    </form>
  );
}
