export type LibraryStatus =
  | 'WISHLIST'
  | 'BACKLOG'
  | 'PLAYING'
  | 'COMPLETED'
  | 'DROPPED'
  | 'PAUSED';

export type LibrarySortOption =
  | 'UPDATED_DESC'
  | 'ADDED_DESC'
  | 'TITLE_ASC'
  | 'TITLE_DESC'
  | 'STATUS_ASC';

export type PaginatedResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
};

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

export type ProfileVisibility = 'PUBLIC' | 'PRIVATE';

export type ReviewSortOption = 'newest' | 'oldest' | 'highest' | 'lowest';

export type PublicProfileStats = {
  totalGames: number;
  completedGames: number;
  ratedGames: number;
  averageRating: number | null;
  reviewCount: number;
  followerCount: number;
  followingCount: number;
};

export type PublicProfileGame = {
  libraryEntryId: string;
  gameId: string;
  gameSlug?: string | null;
  gameTitle: string;
  gameCoverUrl?: string | null;
  status: LibraryStatus;
  updatedAt: string;
  genres: string[];
  platforms: string[];
};

export type PublicProfile = {
  username: string;
  displayName?: string | null;
  bio?: string | null;
  profileVisibility: ProfileVisibility;
  joinedAt?: string | null;
  stats: PublicProfileStats;
  recentGames: PublicProfileGame[];
  recentReviews: Review[];
};

export type UpdateProfileInput = {
  displayName?: string | null;
  bio?: string | null;
  profileVisibility?: ProfileVisibility;
};

export type ListVisibility = 'PUBLIC' | 'PRIVATE';

export type GameList = {
  id: string;
  username: string;
  displayName?: string | null;
  name: string;
  description?: string | null;
  visibility: ListVisibility;
  itemCount: number;
  likeCount: number;
  liked: boolean;
  createdAt: string;
  updatedAt: string;
  owner: boolean;
};

export type GameListItem = {
  id: string;
  gameId: string;
  gameSlug?: string | null;
  gameTitle: string;
  gameCoverUrl?: string | null;
  addedAt: string;
};

export type GameListDetail = GameList & {
  items: GameListItem[];
};

export type GameListRequest = {
  name: string;
  description?: string | null;
  visibility?: ListVisibility;
};

export type FollowStatus = {
  following: boolean;
  followerCount: number;
  followingCount: number;
};

export type UserSummary = {
  username: string;
  displayName?: string | null;
};

export type LikeStatus = {
  liked: boolean;
  likeCount: number;
};

export type Comment = {
  id: string;
  username: string;
  displayName?: string | null;
  body: string;
  createdAt: string;
  owner: boolean;
  likeCount: number;
  liked: boolean;
  replies: Comment[];
};

export type CommentRequest = {
  body: string;
  parentId?: string;
};

export type ReportedListComment = {
  id: string;
  username: string;
  displayName?: string | null;
  body: string;
  createdAt: string;
  reportCount: number;
  listId: string;
  listName: string;
};

export type ReportedReviewComment = {
  id: string;
  username: string;
  displayName?: string | null;
  body: string;
  createdAt: string;
  reportCount: number;
  reviewId: string;
  gameId: string;
  gameTitle: string;
};

export type NotificationType =
  | 'FOLLOW'
  | 'LIST_LIKE'
  | 'REVIEW_LIKE'
  | 'LIST_COMMENT'
  | 'REVIEW_COMMENT'
  | 'COMMENT_REPLY'
  | 'LIST_COMMENT_LIKE'
  | 'REVIEW_COMMENT_LIKE';

export type NotificationItem = {
  id: string;
  type: NotificationType;
  actorUsername: string;
  actorDisplayName?: string | null;
  listId?: string | null;
  listName?: string | null;
  reviewId?: string | null;
  gameId?: string | null;
  gameSlug?: string | null;
  gameTitle?: string | null;
  read: boolean;
  createdAt: string;
};

export type UnreadCount = {
  count: number;
};

export type ReviewVisibility = 'PUBLIC' | 'PRIVATE';

export type Review = {
  id: string;
  username: string;
  displayName?: string | null;
  gameId: string;
  gameSlug?: string | null;
  gameTitle: string;
  gameCoverUrl?: string | null;
  rating?: number | null;
  body: string;
  containsSpoilers: boolean;
  visibility: ReviewVisibility;
  likeCount: number;
  liked: boolean;
  createdAt: string;
  updatedAt: string;
  owner: boolean;
};

export type ReviewRequest = {
  rating?: number | null;
  body: string;
  containsSpoilers?: boolean;
  visibility?: ReviewVisibility;
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
  notes?: string | null;
  startedAt?: string | null;
  completedAt?: string | null;
  createdAt: string;
  updatedAt: string;
};

export type LibraryEntryUpdateInput = {
  status?: LibraryStatus;
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
