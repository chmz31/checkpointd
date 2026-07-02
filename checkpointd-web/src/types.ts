export type LibraryStatus =
  | 'WISHLIST'
  | 'BACKLOG'
  | 'PLAYING'
  | 'COMPLETED'
  | 'DROPPED'
  | 'PAUSED';

export type AuthResponse = {
  accessToken: string;
  tokenType: string;
  expiresInMinutes: number;
};

export type CurrentUser = {
  id: string;
  email: string;
  username: string;
  role: string;
};

export type ExternalGameSearchResult = {
  provider: string;
  externalId: string;
  title: string;
  slug?: string | null;
  coverUrl?: string | null;
  releaseDate?: string | null;
};

export type Game = {
  id: string;
  externalProvider?: string | null;
  externalId?: string | null;
  title: string;
  slug?: string | null;
  coverUrl?: string | null;
  releaseDate?: string | null;
};

export type LibraryEntry = {
  id: string;
  gameId: string;
  gameTitle: string;
  gameSlug?: string | null;
  gameCoverUrl?: string | null;
  status: LibraryStatus;
  rating?: number | null;
  notes?: string | null;
  startedAt?: string | null;
  completedAt?: string | null;
  createdAt: string;
  updatedAt: string;
};
