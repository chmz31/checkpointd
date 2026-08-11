import { FormEvent, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api';
import { gamePath, userProfilePath } from '../routePaths';
import type { CurrentUser, Game, GameListDetail, GameListRequest } from '../types';
import { CoverImage } from './CoverImage';
import { LikeButton } from './LikeButton';
import { ListForm } from './ListForm';

export function ListDetailPage({ currentUser }: { currentUser: CurrentUser | null }) {
  const { username, listId } = useParams();
  const isOwnList = Boolean(currentUser && username && currentUser.username === username);
  const [list, setList] = useState<GameListDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [deleted, setDeleted] = useState(false);
  const [shareMessage, setShareMessage] = useState<string | null>(null);

  useEffect(() => {
    if (!username || !listId) return;
    setLoading(true);
    setError(null);
    setDeleted(false);

    const request = isOwnList ? api.getMyList(listId) : api.getPublicList(username, listId);
    request
      .then(setList)
      .catch((caught) => setError(caught instanceof Error ? caught.message : 'List not found'))
      .finally(() => setLoading(false));
  }, [username, listId, isOwnList]);

  async function saveList(request: GameListRequest) {
    if (!listId) return;
    setSaving(true);
    setError(null);

    try {
      await api.updateList(listId, request);
      setList(await api.getMyList(listId));
      setEditing(false);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Could not save list');
    } finally {
      setSaving(false);
    }
  }

  async function deleteList() {
    if (!listId) return;
    setError(null);

    try {
      await api.deleteList(listId);
      setDeleted(true);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Could not delete list');
    }
  }

  async function addGame(game: Game) {
    if (!listId) return;
    setError(null);

    try {
      setList(await api.addListItem(listId, game.id));
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Could not add game');
    }
  }

  async function removeGame(gameId: string) {
    if (!listId) return;
    setError(null);

    try {
      setList(await api.removeListItem(listId, gameId));
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Could not remove game');
    }
  }

  async function shareList() {
    const url = window.location.href;

    if (navigator.share) {
      try {
        await navigator.share({ title: list?.name, url });
      } catch {
        // Share was cancelled or unsupported for this data; nothing to do.
      }
      return;
    }

    try {
      await navigator.clipboard.writeText(url);
      setShareMessage('Link copied to clipboard.');
    } catch {
      setShareMessage(url);
    } finally {
      window.setTimeout(() => setShareMessage(null), 3000);
    }
  }

  if (loading) {
    return <p className="muted">Loading list...</p>;
  }

  if (deleted) {
    return (
      <section className="panel">
        <div className="empty-state catalog-empty">
          <h3>List deleted</h3>
          <p>Your list has been removed.</p>
        </div>
      </section>
    );
  }

  if (!list) {
    return (
      <section className="panel">
        <div className="empty-state catalog-empty">
          <h3>List not found</h3>
          <p>{error || 'This list is private or does not exist.'}</p>
        </div>
      </section>
    );
  }

  return (
    <section className="panel review-page">
      <div className="section-heading">
        <div>
          <h2>{list.name}</h2>
          <p className="muted">
            by <Link className="inline-link" to={userProfilePath(list.username)}>@{list.username}</Link>
            {' '}· {list.itemCount} {list.itemCount === 1 ? 'game' : 'games'}
            {list.visibility === 'PRIVATE' && ' · Private'}
          </p>
        </div>
        <div className="inline-actions">
          {currentUser && (
            <LikeButton
              liked={list.liked}
              likeCount={list.likeCount}
              onLike={() => api.likeList(list.id)}
              onUnlike={() => api.unlikeList(list.id)}
              onChange={(status) =>
                setList((current) => (current ? { ...current, liked: status.liked, likeCount: status.likeCount } : current))
              }
            />
          )}
          <button className="button-ghost button-small" onClick={shareList}>
            Share
          </button>
          {isOwnList && (
            <>
              <button className="button-ghost button-small" onClick={() => setEditing((current) => !current)}>
                {editing ? 'Close' : 'Edit list'}
              </button>
              <button className="button-ghost button-small" onClick={deleteList}>
                Delete list
              </button>
            </>
          )}
        </div>
      </div>

      {editing ? (
        <ListForm list={list} submitting={saving} onSubmit={saveList} />
      ) : (
        list.description && <p className="profile-bio">{list.description}</p>
      )}

      {shareMessage && <p className="success compact-message">{shareMessage}</p>}
      {error && <p className="error compact-message">{error}</p>}

      {isOwnList && <AddGameToList existingGameIds={list.items.map((item) => item.gameId)} onAdd={addGame} />}

      {list.items.length === 0 ? (
        <div className="empty-state catalog-empty">
          <h3>No games yet</h3>
          <p>{isOwnList ? 'Add a game to start curating this list.' : 'This list has no games yet.'}</p>
        </div>
      ) : (
        <div className="profile-game-grid">
          {list.items.map((item) => (
            <div className="profile-game-card" key={item.id}>
              <Link to={gamePath({ id: item.gameId, slug: item.gameSlug, title: item.gameTitle })}>
                <CoverImage src={item.gameCoverUrl} title={item.gameTitle} />
              </Link>
              <div>
                <Link className="inline-link" to={gamePath({ id: item.gameId, slug: item.gameSlug, title: item.gameTitle })}>
                  <h3>{item.gameTitle}</h3>
                </Link>
                {isOwnList && (
                  <button className="button-ghost button-small" onClick={() => removeGame(item.gameId)}>
                    Remove
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}

function AddGameToList({
  existingGameIds,
  onAdd,
}: {
  existingGameIds: string[];
  onAdd: (game: Game) => Promise<void>;
}) {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<Game[]>([]);
  const [searching, setSearching] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function search(event: FormEvent) {
    event.preventDefault();
    const cleanQuery = query.trim();
    if (!cleanQuery) {
      return;
    }

    setSearching(true);
    setError(null);

    try {
      setResults(await api.searchLocalGames(cleanQuery));
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Could not search games');
    } finally {
      setSearching(false);
    }
  }

  return (
    <div className="callout-section">
      <form className="inline-actions" onSubmit={search}>
        <input
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="Search your game catalog"
        />
        <button type="submit" disabled={searching}>
          {searching ? 'Searching...' : 'Search'}
        </button>
      </form>
      {error && <p className="error compact-message">{error}</p>}
      {results.length > 0 && (
        <ul className="list-search-results">
          {results.map((game) => {
            const alreadyAdded = existingGameIds.includes(game.id);
            return (
              <li key={game.id} className="inline-actions">
                <span>{game.title}</span>
                <button
                  className="button-ghost button-small"
                  disabled={alreadyAdded}
                  onClick={() => onAdd(game)}
                >
                  {alreadyAdded ? 'Already added' : 'Add'}
                </button>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
