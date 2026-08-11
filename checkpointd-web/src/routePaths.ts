import type { Game, LibraryEntry } from './types';

type GamePathInput = Pick<Game, 'id' | 'slug' | 'title'>;
type LibraryEntryPathInput = Pick<LibraryEntry, 'id' | 'gameSlug' | 'gameTitle'>;

export function gamePath(game: GamePathInput) {
  return `/games/${game.id}/${slugSegment(game.slug || game.title || 'game')}`;
}

export function libraryEntryPath(entry: LibraryEntryPathInput) {
  return `/library/${entry.id}/${slugSegment(entry.gameSlug || entry.gameTitle || 'game')}`;
}

export function canonicalGamePath(gameId: string, slug?: string | null, title?: string | null) {
  return `/games/${gameId}/${slugSegment(slug || title || 'game')}`;
}

export function canonicalLibraryEntryPath(entryId: string, gameSlug?: string | null, gameTitle?: string | null) {
  return `/library/${entryId}/${slugSegment(gameSlug || gameTitle || 'game')}`;
}

export function userProfilePath(username: string) {
  return `/u/${encodeURIComponent(username)}`;
}

export function gameReviewsPath(game: GamePathInput) {
  return `${gamePath(game)}/reviews`;
}

export function userReviewsPath(username: string) {
  return `/u/${encodeURIComponent(username)}/reviews`;
}

export function userGameReviewPath(username: string, game: GamePathInput) {
  return `/u/${encodeURIComponent(username)}/games/${game.id}/${slugSegment(game.slug || game.title || 'game')}`;
}

export function userListsPath(username: string) {
  return `/u/${encodeURIComponent(username)}/lists`;
}

export function userFollowersPath(username: string) {
  return `/u/${encodeURIComponent(username)}/followers`;
}

export function userFollowingPath(username: string) {
  return `/u/${encodeURIComponent(username)}/following`;
}

export function listPath(username: string, list: { id: string; name: string }) {
  return `/u/${encodeURIComponent(username)}/lists/${list.id}/${slugSegment(list.name || 'list')}`;
}

export function slugify(value: string) {
  const slug = value
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');

  return slug || 'game';
}

function slugSegment(value: string) {
  return encodeURIComponent(slugify(value));
}
