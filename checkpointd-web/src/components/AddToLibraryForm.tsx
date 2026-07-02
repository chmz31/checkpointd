import { FormEvent, useState } from 'react';
import { api } from '../api';
import { libraryStatuses } from '../constants';
import type { Game, LibraryStatus } from '../types';
import { CoverImage } from './CoverImage';

export function AddToLibraryForm({ game, onAdded }: { game: Game; onAdded: () => void }) {
  const [status, setStatus] = useState<LibraryStatus>('BACKLOG');
  const [rating, setRating] = useState('');
  const [notes, setNotes] = useState('');
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setLoading(true);
    setMessage(null);
    setError(null);

    try {
      await api.addLibraryEntry({
        gameId: game.id,
        status,
        rating: rating ? Number(rating) : null,
        notes: notes.trim() || undefined,
      });
      setMessage('Added to library.');
      onAdded();
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Could not add to library');
    } finally {
      setLoading(false);
    }
  }

  return (
    <form className="stack" onSubmit={submit}>
      <article className="game-row selected">
        <CoverImage src={game.coverUrl} title={game.title} />
        <div>
          <h3>{game.title}</h3>
          <p className="muted">{[game.slug, game.releaseDate].filter(Boolean).join(' | ')}</p>
        </div>
      </article>
      <label>
        Status
        <select value={status} onChange={(event) => setStatus(event.target.value as LibraryStatus)}>
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
          onChange={(event) => setRating(event.target.value)}
          min={1}
          max={10}
          type="number"
          placeholder="1-10"
        />
      </label>
      <label>
        Notes
        <textarea value={notes} onChange={(event) => setNotes(event.target.value)} rows={4} />
      </label>
      {message && <p className="success">{message}</p>}
      {error && <p className="error">{error}</p>}
      <button type="submit" disabled={loading}>
        {loading ? 'Adding...' : 'Add to Library'}
      </button>
    </form>
  );
}
