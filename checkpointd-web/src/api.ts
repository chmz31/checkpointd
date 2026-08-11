import type {
  AuthResponse,
  Comment,
  CommentRequest,
  CurrentUser,
  ExternalGameSearchResult,
  FollowStatus,
  Game,
  GameList,
  GameListDetail,
  GameListRequest,
  LibraryEntry,
  LibraryEntryUpdateInput,
  LibrarySortOption,
  LibraryStats,
  LibraryStatus,
  LikeStatus,
  PaginatedResponse,
  PublicProfile,
  ReportedListComment,
  ReportedReviewComment,
  Review,
  ReviewRequest,
  UpdateProfileInput,
  UserSummary,
} from './types';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
const TOKEN_KEY = 'checkpointd.accessToken';

export class ApiError extends Error {
  status: number;

  constructor(message: string, status: number) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

export function isApiErrorStatus(error: unknown, status: number) {
  return error instanceof ApiError && error.status === status;
}

export function getStoredToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function setStoredToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearStoredToken() {
  localStorage.removeItem(TOKEN_KEY);
}

async function apiRequest<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = getStoredToken();
  const headers = new Headers(options.headers);

  if (!headers.has('Content-Type') && options.body) {
    headers.set('Content-Type', 'application/json');
  }
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
  });

  if (!response.ok) {
    const message = await readErrorMessage(response);
    throw new ApiError(message, response.status);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}

async function readErrorMessage(response: Response) {
  try {
    const body = (await response.json()) as { message?: string };
    return body.message || `Request failed with status ${response.status}`;
  } catch {
    return `Request failed with status ${response.status}`;
  }
}

export const api = {
  login(email: string, password: string) {
    return apiRequest<AuthResponse>('/api/v1/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    });
  },

  register(email: string, username: string, password: string) {
    return apiRequest<AuthResponse>('/api/v1/auth/register', {
      method: 'POST',
      body: JSON.stringify({ email, username, password }),
    });
  },

  me() {
    return apiRequest<CurrentUser>('/api/v1/users/me');
  },

  getPublicProfile(username: string) {
    return apiRequest<PublicProfile>(`/api/v1/profiles/${encodeURIComponent(username)}`);
  },

  getMyProfile() {
    return apiRequest<PublicProfile>('/api/v1/profiles/me');
  },

  updateMyProfile(input: UpdateProfileInput) {
    return apiRequest<PublicProfile>('/api/v1/profiles/me', {
      method: 'PATCH',
      body: JSON.stringify(input),
    });
  },

  searchExternalGames(query: string) {
    return apiRequest<ExternalGameSearchResult[]>(
      `/api/v1/external-games/search?q=${encodeURIComponent(query)}`,
    );
  },

  importExternalGame(provider: string, externalId: string) {
    return apiRequest<Game>('/api/v1/external-games/import', {
      method: 'POST',
      body: JSON.stringify({ provider, externalId }),
    });
  },

  getGame(gameId: string) {
    return apiRequest<Game>(`/api/v1/games/${gameId}`);
  },

  searchLocalGames(query: string) {
    return apiRequest<Game[]>(`/api/v1/games?q=${encodeURIComponent(query)}`);
  },

  addLibraryEntry(input: {
    gameId: string;
    status: LibraryStatus;
    notes?: string | null;
    startedAt?: string | null;
    completedAt?: string | null;
  }) {
    return apiRequest<LibraryEntry>('/api/v1/library', {
      method: 'POST',
      body: JSON.stringify(input),
    });
  },

  listLibrary(params: {
    page?: number;
    size?: number;
    status?: LibraryStatus;
    q?: string;
    sort?: LibrarySortOption;
  } = {}) {
    const query = new URLSearchParams();

    if (params.page != null) {
      query.set('page', String(params.page));
    }
    if (params.size != null) {
      query.set('size', String(params.size));
    }
    if (params.status) {
      query.set('status', params.status);
    }
    if (params.q?.trim()) {
      query.set('q', params.q.trim());
    }
    if (params.sort) {
      query.set('sort', params.sort);
    }

    const queryString = query.toString();
    return apiRequest<PaginatedResponse<LibraryEntry>>(`/api/v1/library${queryString ? `?${queryString}` : ''}`);
  },

  getLibraryEntry(entryId: string) {
    return apiRequest<LibraryEntry>(`/api/v1/library/${entryId}`);
  },

  getLibraryEntryByGame(gameId: string) {
    return apiRequest<LibraryEntry>(`/api/v1/library/by-game/${gameId}`);
  },

  getLibraryEntryByExternalGame(provider: string, externalId: string) {
    return apiRequest<LibraryEntry>(
      `/api/v1/library/by-external-game?provider=${encodeURIComponent(provider)}&externalId=${encodeURIComponent(externalId)}`,
    );
  },

  getLibraryStats() {
    return apiRequest<LibraryStats>('/api/v1/library/stats');
  },

  getGameReviews(gameId: string, page = 0, size = 10) {
    return apiRequest<PaginatedResponse<Review>>(
      `/api/v1/reviews/games/${gameId}?page=${page}&size=${size}`,
    );
  },

  getUserReviews(username: string, page = 0, size = 10) {
    return apiRequest<PaginatedResponse<Review>>(
      `/api/v1/reviews/users/${encodeURIComponent(username)}?page=${page}&size=${size}`,
    );
  },

  getPublicUserGameReview(username: string, gameId: string) {
    return apiRequest<Review>(`/api/v1/reviews/users/${encodeURIComponent(username)}/games/${gameId}`);
  },

  getMyGameReview(gameId: string) {
    return apiRequest<Review>(`/api/v1/reviews/me/games/${gameId}`);
  },

  saveMyGameReview(gameId: string, input: ReviewRequest) {
    return apiRequest<Review>(`/api/v1/reviews/me/games/${gameId}`, {
      method: 'PUT',
      body: JSON.stringify(input),
    });
  },

  deleteMyGameReview(gameId: string) {
    return apiRequest<void>(`/api/v1/reviews/me/games/${gameId}`, {
      method: 'DELETE',
    });
  },

  createList(input: GameListRequest) {
    return apiRequest<GameList>('/api/v1/lists', {
      method: 'POST',
      body: JSON.stringify(input),
    });
  },

  getMyLists(page = 0, size = 20) {
    return apiRequest<PaginatedResponse<GameList>>(`/api/v1/lists/me?page=${page}&size=${size}`);
  },

  getUserLists(username: string, page = 0, size = 20) {
    return apiRequest<PaginatedResponse<GameList>>(
      `/api/v1/lists/users/${encodeURIComponent(username)}?page=${page}&size=${size}`,
    );
  },

  getPopularLists(page = 0, size = 20) {
    return apiRequest<PaginatedResponse<GameList>>(`/api/v1/lists/popular?page=${page}&size=${size}`);
  },

  getMyList(listId: string) {
    return apiRequest<GameListDetail>(`/api/v1/lists/me/${listId}`);
  },

  getPublicList(username: string, listId: string) {
    return apiRequest<GameListDetail>(`/api/v1/lists/users/${encodeURIComponent(username)}/${listId}`);
  },

  updateList(listId: string, input: GameListRequest) {
    return apiRequest<GameList>(`/api/v1/lists/me/${listId}`, {
      method: 'PATCH',
      body: JSON.stringify(input),
    });
  },

  deleteList(listId: string) {
    return apiRequest<void>(`/api/v1/lists/me/${listId}`, {
      method: 'DELETE',
    });
  },

  addListItem(listId: string, gameId: string) {
    return apiRequest<GameListDetail>(`/api/v1/lists/me/${listId}/items`, {
      method: 'POST',
      body: JSON.stringify({ gameId }),
    });
  },

  removeListItem(listId: string, gameId: string) {
    return apiRequest<GameListDetail>(`/api/v1/lists/me/${listId}/items/${gameId}`, {
      method: 'DELETE',
    });
  },

  followUser(username: string) {
    return apiRequest<FollowStatus>(`/api/v1/follows/users/${encodeURIComponent(username)}`, {
      method: 'POST',
    });
  },

  unfollowUser(username: string) {
    return apiRequest<FollowStatus>(`/api/v1/follows/users/${encodeURIComponent(username)}`, {
      method: 'DELETE',
    });
  },

  getFollowStatus(username: string) {
    return apiRequest<FollowStatus>(`/api/v1/follows/users/${encodeURIComponent(username)}/status`);
  },

  getFollowers(username: string, page = 0, size = 20) {
    return apiRequest<PaginatedResponse<UserSummary>>(
      `/api/v1/follows/users/${encodeURIComponent(username)}/followers?page=${page}&size=${size}`,
    );
  },

  getFollowing(username: string, page = 0, size = 20) {
    return apiRequest<PaginatedResponse<UserSummary>>(
      `/api/v1/follows/users/${encodeURIComponent(username)}/following?page=${page}&size=${size}`,
    );
  },

  likeList(listId: string) {
    return apiRequest<LikeStatus>(`/api/v1/likes/lists/${listId}`, {
      method: 'POST',
    });
  },

  unlikeList(listId: string) {
    return apiRequest<LikeStatus>(`/api/v1/likes/lists/${listId}`, {
      method: 'DELETE',
    });
  },

  getListLikeStatus(listId: string) {
    return apiRequest<LikeStatus>(`/api/v1/likes/lists/${listId}/status`);
  },

  likeReview(reviewId: string) {
    return apiRequest<LikeStatus>(`/api/v1/likes/reviews/${reviewId}`, {
      method: 'POST',
    });
  },

  unlikeReview(reviewId: string) {
    return apiRequest<LikeStatus>(`/api/v1/likes/reviews/${reviewId}`, {
      method: 'DELETE',
    });
  },

  getReviewLikeStatus(reviewId: string) {
    return apiRequest<LikeStatus>(`/api/v1/likes/reviews/${reviewId}/status`);
  },

  getListComments(listId: string, page = 0, size = 20) {
    return apiRequest<PaginatedResponse<Comment>>(`/api/v1/comments/lists/${listId}?page=${page}&size=${size}`);
  },

  addListComment(listId: string, input: CommentRequest) {
    return apiRequest<Comment>(`/api/v1/comments/lists/${listId}`, {
      method: 'POST',
      body: JSON.stringify(input),
    });
  },

  deleteListComment(listId: string, commentId: string) {
    return apiRequest<void>(`/api/v1/comments/lists/${listId}/${commentId}`, {
      method: 'DELETE',
    });
  },

  reportListComment(listId: string, commentId: string, reason?: string) {
    return apiRequest<void>(`/api/v1/comments/lists/${listId}/${commentId}/report`, {
      method: 'POST',
      body: JSON.stringify({ reason }),
    });
  },

  getReviewComments(reviewId: string, page = 0, size = 20) {
    return apiRequest<PaginatedResponse<Comment>>(`/api/v1/comments/reviews/${reviewId}?page=${page}&size=${size}`);
  },

  addReviewComment(reviewId: string, input: CommentRequest) {
    return apiRequest<Comment>(`/api/v1/comments/reviews/${reviewId}`, {
      method: 'POST',
      body: JSON.stringify(input),
    });
  },

  deleteReviewComment(reviewId: string, commentId: string) {
    return apiRequest<void>(`/api/v1/comments/reviews/${reviewId}/${commentId}`, {
      method: 'DELETE',
    });
  },

  reportReviewComment(reviewId: string, commentId: string, reason?: string) {
    return apiRequest<void>(`/api/v1/comments/reviews/${reviewId}/${commentId}/report`, {
      method: 'POST',
      body: JSON.stringify({ reason }),
    });
  },

  getReportedListComments(page = 0, size = 20) {
    return apiRequest<PaginatedResponse<ReportedListComment>>(
      `/api/v1/admin/comments/lists/reported?page=${page}&size=${size}`,
    );
  },

  getReportedReviewComments(page = 0, size = 20) {
    return apiRequest<PaginatedResponse<ReportedReviewComment>>(
      `/api/v1/admin/comments/reviews/reported?page=${page}&size=${size}`,
    );
  },

  updateLibraryEntry(entryId: string, input: LibraryEntryUpdateInput) {
    return apiRequest<LibraryEntry>(`/api/v1/library/${entryId}`, {
      method: 'PATCH',
      body: JSON.stringify(input),
    });
  },

  syncLibraryEntryMetadata(entryId: string) {
    return apiRequest<LibraryEntry>(`/api/v1/library/${entryId}/sync-metadata`, {
      method: 'POST',
    });
  },

  deleteLibraryEntry(entryId: string) {
    return apiRequest<void>(`/api/v1/library/${entryId}`, {
      method: 'DELETE',
    });
  },
};
