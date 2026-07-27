import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api, isApiErrorStatus } from '../api';
import { formatDate } from '../dateUtils';
import type { Game, LibraryEntry } from '../types';
import { AddToLibraryForm, type AddToLibraryFormValues } from './AddToLibraryForm';
import { ChipGroup } from './ChipGroup';
import { CoverImage } from './CoverImage';
import { DetailFact } from './DetailFact';
import { MediaGallery } from './MediaGallery';

export function GameDetailsPage() {
  const { gameId } = useParams();
  const [game, setGame] = useState<Game | null>(null);
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
          if (!isApiErrorStatus(caught, 404)) {
            const message = caught instanceof Error ? caught.message : '';
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

  async function addToLibrary(values: AddToLibraryFormValues) {
    if (!game) return;

    setAdding(true);
    setMessage(null);
    setError(null);

    try {
      const entry = await api.addLibraryEntry({
        gameId: game.id,
        ...values,
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
            <DetailFact label="Release" value={formatDate(game.releaseDate)} />
            <DetailFact label="Provider" value={game.externalProvider || 'Local'} />
            {game.createdAt && <DetailFact label="Added" value={formatDate(game.createdAt)} />}
            {game.updatedAt && <DetailFact label="Updated" value={formatDate(game.updatedAt)} />}
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
              <AddToLibraryForm
                onSubmit={addToLibrary}
                submitting={adding}
                submitLabel="Add to Library"
                className="direct-add-form detail-add-form"
                onChange={clearAddFeedback}
              />
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
              <ChipGroup label="Genres" values={game.genres} />
              <ChipGroup label="Platforms" values={game.platforms} />
            </section>
          )}

          <MediaGallery title={game.title} artworkUrls={game.artworkUrls} screenshotUrls={game.screenshotUrls} />
        </section>
      </div>
    </article>
  );
}
