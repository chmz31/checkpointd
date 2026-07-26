import { FormEvent, useState } from 'react';
import { api } from '../api';
import { libraryStatuses } from '../constants';
import type { ExternalGameSearchResult, LibraryEntry, LibraryStatus } from '../types';

export function AddSearchResultToLibrary({
  result,
  onAdded,
}: {
  result: ExternalGameSearchResult;
  onAdded: (entry: LibraryEntry) => void;
}) {
  const [status, setStatus] = useState<LibraryStatus>('BACKLOG');
  const [rating, setRating] = useState('');
  const [notes, setNotes] = useState('');
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  function clearFeedback() {
    setMessage(null);
    setError(null);
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    setLoading(true);
    setMessage(null);
    setError(null);

    try {
      const game = await api.importExternalGame(result.provider, result.externalId);
      const entry = await api.addLibraryEntry({
        gameId: game.id,
        status,
        rating: rating ? Number(rating) : null,
        notes: notes.trim() || undefined,
      });
      setMessage('Added to library.');
      onAdded(entry);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Could not add to library');
    } finally {
      setLoading(false);
    }
  }

  return (
    <form className="direct-add-form" onSubmit={submit}>
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
      {message && <p className="success compact-message">{message}</p>}
      {error && <p className="error compact-message">{error}</p>}
      <div className="inline-actions direct-add-actions">
        <button type="submit" disabled={loading}>
          {loading ? 'Adding...' : 'Add'}
        </button>
      </div>
    </form>
  );
}
