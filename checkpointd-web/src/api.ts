import type {
  AuthResponse,
  CurrentUser,
  ExternalGameSearchResult,
  Game,
  LibraryEntry,
  LibraryEntryUpdateInput,
  LibrarySortOption,
  LibraryStats,
  LibraryStatus,
  PaginatedResponse,
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

  addLibraryEntry(input: {
    gameId: string;
    status: LibraryStatus;
    rating?: number | null;
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
