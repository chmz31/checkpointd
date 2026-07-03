import { FormEvent, useEffect, useState } from 'react';
import { api } from '../api';
import { libraryStatuses } from '../constants';
import type { LibraryEntry, LibraryStatus } from '../types';
import { CoverImage } from './CoverImage';
import { GameMetadata } from './GameMetadata';

export function LibraryEntryCard({
  entry,
  onUpdated,
  onDelete,
}: {
  entry: LibraryEntry;
  onUpdated: (entry: LibraryEntry) => void;
  onDelete: () => void;
}) {
  const [editing, setEditing] = useState(false);
  const [status, setStatus] = useState<LibraryStatus>(entry.status);
  const [rating, setRating] = useState(entry.rating ? String(entry.rating) : '');
  const [notes, setNotes] = useState(entry.notes || '');
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setStatus(entry.status);
    setRating(entry.rating ? String(entry.rating) : '');
    setNotes(entry.notes || '');
  }, [entry]);

  function clearFeedback() {
    setMessage(null);
    setError(null);
  }

  async function save(event: FormEvent) {
    event.preventDefault();
    setLoading(true);
    setMessage(null);
    setError(null);

    try {
      const updatedEntry = await api.updateLibraryEntry(entry.id, {
        status,
        ...(rating ? { rating: Number(rating) } : {}),
        notes,
      });
      onUpdated(updatedEntry);
      setEditing(false);
      setMessage('Saved.');
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Could not update entry');
    } finally {
      setLoading(false);
    }
  }

  function cancel() {
    setStatus(entry.status);
    setRating(entry.rating ? String(entry.rating) : '');
    setNotes(entry.notes || '');
    setEditing(false);
    setError(null);
  }

  return (
    <article className="library-entry-card">
      <div className="library-entry">
        <CoverImage src={entry.gameCoverUrl} title={entry.gameTitle} />
        <div>
          <h3>{entry.gameTitle}</h3>
          <p className="status-line">
            <span>{entry.status}</span>
            {entry.rating && <span>{entry.rating}/10</span>}
          </p>
          <GameMetadata
            summary={entry.gameSummary}
            genres={entry.gameGenres}
            platforms={entry.gamePlatforms}
          />
          {entry.notes && <p className="notes">{entry.notes}</p>}
          {message && <p className="success compact-message">{message}</p>}
        </div>
        <div className="entry-actions">
          <button onClick={() => setEditing((current) => !current)}>{editing ? 'Close' : 'Edit'}</button>
          <button onClick={onDelete}>Delete</button>
        </div>
      </div>

      {editing && (
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
            <button type="button" onClick={cancel} disabled={loading}>
              Cancel
            </button>
          </div>
        </form>
      )}
    </article>
  );
}
