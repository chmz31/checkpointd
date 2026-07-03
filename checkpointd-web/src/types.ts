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
  summary?: string | null;
  genres: string[];
  platforms: string[];
};

export type Game = {
  id: string;
  externalProvider?: string | null;
  externalId?: string | null;
  title: string;
  slug?: string | null;
  coverUrl?: string | null;
  releaseDate?: string | null;
  summary?: string | null;
  genres: string[];
  platforms: string[];
};

export type LibraryEntry = {
  id: string;
  gameId: string;
  gameTitle: string;
  gameSlug?: string | null;
  gameCoverUrl?: string | null;
  gameSummary?: string | null;
  gameGenres: string[];
  gamePlatforms: string[];
  status: LibraryStatus;
  rating?: number | null;
  notes?: string | null;
  startedAt?: string | null;
  completedAt?: string | null;
  createdAt: string;
  updatedAt: string;
};

export type LibraryEntryUpdateInput = {
  status?: LibraryStatus;
  rating?: number;
  notes?: string;
};

export type LibraryStats = {
  totalEntries: number;
  wishlistCount: number;
  backlogCount: number;
  playingCount: number;
  completedCount: number;
  droppedCount: number;
  pausedCount: number;
  ratedCount: number;
  averageRating: number | null;
};
