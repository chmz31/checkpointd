import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { api, isApiErrorStatus } from '../api';
import { formatDate } from '../dateUtils';
import { gamePath, libraryEntryPath, slugify } from '../routePaths';
import type { Game, LibraryEntry } from '../types';
import { AddToLibraryForm, type AddToLibraryFormValues } from './AddToLibraryForm';
import { ChipGroup } from './ChipGroup';
import { CoverImage } from './CoverImage';
import { DetailFact } from './DetailFact';
import { MediaGallery } from './MediaGallery';

export function GameDetailsPage() {
  const { gameId, slug } = useParams();
  const navigate = useNavigate();
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
        const canonicalPath = gamePath(loadedGame);
        const canonicalSlug = slugify(loadedGame.slug || loadedGame.title || 'game');
        if (slug !== canonicalSlug) {
          navigate(canonicalPath, { replace: true });
        }

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
  }, [gameId, navigate, slug]);

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

  const backdropUrl = game.backdropUrl || game.artworkUrls[0] || game.screenshotUrls[0];
  const hasCatalogDetails =
    game.developers.length > 0 ||
    game.publishers.length > 0 ||
    game.gameModes.length > 0 ||
    game.themes.length > 0 ||
    game.playerPerspectives.length > 0 ||
    game.websites.length > 0 ||
    game.externalRating != null;
  const visibleWebsites = labeledWebsites(game.websites).slice(0, 8);

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
                  <Link className="inline-link" to={libraryEntryPath(libraryEntry)}>
                    View library entry
                  </Link>
                </>
              )}
            </p>
          )}

          {libraryEntry ? (
            <section className="detail-section callout-section">
              <h3>Library</h3>
              <p>
                Already in your library.{' '}
                <Link className="inline-link" to={libraryEntryPath(libraryEntry)}>
                  View library entry
                </Link>
              </p>
            </section>
          ) : (
            <section className="detail-section callout-section">
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

          {hasCatalogDetails && (
            <section className="detail-section">
              <h3>Catalog details</h3>
              <ChipGroup label="Developers" values={game.developers} />
              <ChipGroup label="Publishers" values={game.publishers} />
              <ChipGroup label="Game modes" values={game.gameModes} />
              <ChipGroup label="Themes" values={game.themes} />
              <ChipGroup label="Player perspectives" values={game.playerPerspectives} />
              {game.externalRating != null && (
                <div className="detail-chip-group">
                  <p className="muted">External rating</p>
                  <p>
                    {Math.round(game.externalRating)}/100
                    {game.externalRatingCount ? ` from ${game.externalRatingCount.toLocaleString()} ratings` : ''}
                  </p>
                </div>
              )}
              {visibleWebsites.length > 0 && (
                <div className="detail-chip-group">
                  <p className="muted">Websites</p>
                  <div className="chip-list">
                    {visibleWebsites.map((website) => (
                      <a
                        className="metadata-chip metadata-link-chip"
                        href={website.url}
                        key={website.key}
                        target="_blank"
                        rel="noreferrer"
                        title={website.url}
                      >
                        {website.label}
                      </a>
                    ))}
                  </div>
                </div>
              )}
            </section>
          )}

          <MediaGallery title={game.title} artworkUrls={game.artworkUrls} screenshotUrls={game.screenshotUrls} />
        </section>
      </div>
    </article>
  );
}

function labeledWebsites(websites: Game['websites']) {
  const labelCounts = new Map<string, number>();

  return websites
    .filter((website) => website.url)
    .map((website, index) => {
      const baseLabel = website.label?.trim() || inferWebsiteLabel(website.url) || 'Website';
      const count = (labelCounts.get(baseLabel) || 0) + 1;
      labelCounts.set(baseLabel, count);

      return {
        key: `${website.url}-${index}`,
        label: count === 1 ? baseLabel : `${baseLabel} ${count}`,
        url: website.url,
      };
    });
}

function inferWebsiteLabel(url: string) {
  try {
    const parsed = new URL(url);
    const domain = parsed.hostname.replace(/^www\./, '');
    if (!domain) return 'Website';

    const knownLabels: Array<[string, string]> = [
      ['steampowered.com', 'Steam'],
      ['gog.com', 'GOG'],
      ['epicgames.com', 'Epic Games'],
      ['playstation.com', 'PlayStation'],
      ['xbox.com', 'Xbox'],
      ['nintendo.com', 'Nintendo'],
      ['youtube.com', 'YouTube'],
      ['youtu.be', 'YouTube'],
      ['twitch.tv', 'Twitch'],
      ['discord.gg', 'Discord'],
      ['discord.com', 'Discord'],
      ['reddit.com', 'Reddit'],
      ['wikipedia.org', 'Wikipedia'],
    ];
    const match = knownLabels.find(([host]) => domain.endsWith(host));
    if (match) return match[1];

    return domain.split('.')[0]?.replace(/(^|-)([a-z])/g, (_, separator: string, char: string) =>
      `${separator}${char.toUpperCase()}`,
    );
  } catch {
    return 'Website';
  }
}
