import { FormEvent, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api';
import { libraryStatuses } from '../constants';
import type { Game, LibraryEntry, LibraryStatus } from '../types';
import { CoverImage } from './CoverImage';

export function GameDetailsPage() {
  const { gameId } = useParams();
  const [game, setGame] = useState<Game | null>(null);
  const [status, setStatus] = useState<LibraryStatus>('BACKLOG');
  const [rating, setRating] = useState('');
  const [notes, setNotes] = useState('');
  const [loading, setLoading] = useState(true);
  const [adding, setAdding] = useState(false);
  const [libraryEntry, setLibraryEntry] = useState<LibraryEntry | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!gameId) return;
    const currentGameId = gameId;

    setLoading(true);
    setGame(null);
    setLibraryEntry(null);
    setMessage(null);
    setError(null);

    async function load() {
      try {
        const loadedGame = await api.getGame(currentGameId);
        setGame(loadedGame);

        try {
          setLibraryEntry(await api.getLibraryEntryByGame(loadedGame.id));
        } catch (caught) {
          const message = caught instanceof Error ? caught.message : '';
          if (message !== 'Library entry not found') {
            setError(message || 'Could not check library status');
          }
        }
      } catch (caught) {
        setError(caught instanceof Error ? caught.message : 'Could not load game');
      } finally {
        setLoading(false);
      }
    }

    load();
  }, [gameId]);

  function clearAddFeedback() {
    setMessage(null);
    setError(null);
  }

  async function addToLibrary(event: FormEvent) {
    event.preventDefault();
    if (!game) return;

    setAdding(true);
    setMessage(null);
    setError(null);

    try {
      const entry = await api.addLibraryEntry({
        gameId: game.id,
        status,
        rating: rating ? Number(rating) : null,
        notes: notes.trim() || undefined,
      });
      setLibraryEntry(entry);
      setMessage('Added to library.');
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Could not add to library');
    } finally {
      setAdding(false);
    }
  }

  if (loading) {
    return <p className="muted">Loading game...</p>;
  }

  if (!game) {
    return (
      <section className="panel">
        <p className="error">{error || 'Game not found'}</p>
        <Link className="nav-link" to="/library">
          Back to library
        </Link>
      </section>
    );
  }

  const backdropUrl = game.backdropUrl || game.coverUrl;
  const mediaItems = [
    ...game.artworkUrls.map((url) => ({ kind: 'Artwork', url })),
    ...game.screenshotUrls.map((url) => ({ kind: 'Screenshot', url })),
  ].slice(0, 12);

  return (
    <article className="detail-page">
      {backdropUrl && (
        <div
          className="detail-backdrop"
          aria-hidden="true"
          style={{ backgroundImage: `url(${backdropUrl})` }}
        />
      )}
      <div className="detail-content">
        <aside className="detail-poster">
          <CoverImage src={game.coverUrl} title={game.title} />
        </aside>
        <section className="detail-main">
          <div className="detail-heading">
            <div>
              <p className="eyebrow">Catalog game</p>
              <h2>{game.title}</h2>
              <p className="muted">{game.slug || 'Catalog game'}</p>
            </div>
            <div className="inline-actions detail-actions">
              <Link className="nav-link button-small" to="/library">
                Back to library
              </Link>
            </div>
          </div>

          <div className="detail-facts">
            <Fact label="Release" value={formatDate(game.releaseDate)} />
            <Fact label="Provider" value={game.externalProvider || 'Local'} />
            {game.createdAt && <Fact label="Added" value={formatDate(game.createdAt)} />}
            {game.updatedAt && <Fact label="Updated" value={formatDate(game.updatedAt)} />}
          </div>

          {error && <p className="error compact-message">{error}</p>}
          {message && (
            <p className="success compact-message">
              {message}
              {libraryEntry && (
                <>
                  {' '}
                  <Link className="inline-link" to={`/library/${libraryEntry.id}`}>
                    View library entry
                  </Link>
                </>
              )}
            </p>
          )}

          {libraryEntry ? (
            <section className="detail-section">
              <h3>Library</h3>
              <p>
                Already in your library.{' '}
                <Link className="inline-link" to={`/library/${libraryEntry.id}`}>
                  View library entry
                </Link>
              </p>
            </section>
          ) : (
            <section className="detail-section">
              <h3>Add to Library</h3>
              <form className="direct-add-form detail-add-form" onSubmit={addToLibrary}>
                <label>
                  Status
                  <select
                    value={status}
                    onChange={(event) => {
                      setStatus(event.target.value as LibraryStatus);
                      clearAddFeedback();
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
                      clearAddFeedback();
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
                      clearAddFeedback();
                    }}
                    rows={3}
                  />
                </label>
                <div className="inline-actions direct-add-actions">
                  <button type="submit" disabled={adding}>
                    {adding ? 'Adding...' : 'Add to Library'}
                  </button>
                </div>
              </form>
            </section>
          )}

          {game.summary && (
            <section className="detail-section">
              <h3>Summary</h3>
              <p>{game.summary}</p>
            </section>
          )}

          {(game.genres.length > 0 || game.platforms.length > 0) && (
            <section className="detail-section">
              <h3>Metadata</h3>
              {game.genres.length > 0 && <ChipGroup label="Genres" values={game.genres} />}
              {game.platforms.length > 0 && <ChipGroup label="Platforms" values={game.platforms} />}
            </section>
          )}

          {mediaItems.length > 0 && (
            <section className="detail-section">
              <h3>Media</h3>
              <div className="media-grid">
                {mediaItems.map((item, index) => (
                  <figure className="media-tile" key={`${item.kind}-${item.url}-${index}`}>
                    <img src={item.url} alt={`${game.title} ${item.kind.toLowerCase()}`} loading="lazy" />
                    <figcaption>{item.kind}</figcaption>
                  </figure>
                ))}
              </div>
            </section>
          )}
        </section>
      </div>
    </article>
  );
}

function Fact({ label, value }: { label: string; value: string }) {
  return (
    <div className="detail-fact">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function ChipGroup({ label, values }: { label: string; values: string[] }) {
  return (
    <div className="detail-chip-group">
      <p className="muted">{label}</p>
      <div className="chip-list">
        {values.map((value, index) => (
          <span className="metadata-chip" key={`${label}-${value}-${index}`}>
            {value}
          </span>
        ))}
      </div>
    </div>
  );
}

function formatDate(value?: string | null) {
  if (!value) {
    return 'Not set';
  }

  return new Intl.DateTimeFormat(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  }).format(new Date(value));
}
