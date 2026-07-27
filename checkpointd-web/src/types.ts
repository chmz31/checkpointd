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
  developers: string[];
  publishers: string[];
  gameModes: string[];
  themes: string[];
  playerPerspectives: string[];
  websites: GameWebsite[];
  externalRating?: number | null;
  externalRatingCount?: number | null;
  screenshotUrls: string[];
  artworkUrls: string[];
  backdropUrl?: string | null;
};

export type GameWebsite = {
  label: string;
  url: string;
  trusted: boolean;
};

export type MetadataSyncStatus = 'NEVER_SYNCED' | 'SUCCESS' | 'REFRESHING' | 'FAILED';

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
  developers: string[];
  publishers: string[];
  gameModes: string[];
  themes: string[];
  playerPerspectives: string[];
  websites: GameWebsite[];
  externalRating?: number | null;
  externalRatingCount?: number | null;
  screenshotUrls: string[];
  artworkUrls: string[];
  backdropUrl?: string | null;
  metadataSyncedAt?: string | null;
  metadataSyncAttemptedAt?: string | null;
  metadataSyncStatus?: MetadataSyncStatus | null;
  metadataSyncError?: string | null;
  metadataStale: boolean;
  createdAt?: string | null;
  updatedAt?: string | null;
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
  gameDevelopers: string[];
  gamePublishers: string[];
  gameModes: string[];
  gameThemes: string[];
  gamePlayerPerspectives: string[];
  gameWebsites: GameWebsite[];
  gameExternalRating?: number | null;
  gameExternalRatingCount?: number | null;
  gameScreenshotUrls: string[];
  gameArtworkUrls: string[];
  gameBackdropUrl?: string | null;
  gameMetadataSyncAvailable: boolean;
  gameMetadataSyncedAt?: string | null;
  gameMetadataSyncAttemptedAt?: string | null;
  gameMetadataSyncStatus?: MetadataSyncStatus | null;
  gameMetadataSyncError?: string | null;
  gameMetadataStale: boolean;
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
  rating?: number | null;
  notes?: string | null;
  startedAt?: string | null;
  completedAt?: string | null;
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
