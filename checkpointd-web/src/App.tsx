import { useEffect, useState } from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { api, clearStoredToken, getStoredToken, setStoredToken } from './api';
import { AboutPage } from './components/AboutPage';
import { AdminReportedCommentsPage } from './components/AdminReportedCommentsPage';
import { AuthPanel } from './components/AuthPanel';
import { AppShell } from './components/AppShell';
import { FollowListPage } from './components/FollowListPage';
import { GameDetailsPage } from './components/GameDetailsPage';
import { GameReviewsPage } from './components/GameReviewsPage';
import { LibraryEntryDetailsPage } from './components/LibraryEntryDetailsPage';
import { LibraryView } from './components/LibraryView';
import { ListDetailPage } from './components/ListDetailPage';
import { MyListsPage } from './components/MyListsPage';
import { NotificationsPage } from './components/NotificationsPage';
import { PopularListsPage } from './components/PopularListsPage';
import { PrivacyPage } from './components/PrivacyPage';
import { PublicProfilePage } from './components/PublicProfilePage';
import { PublicShell } from './components/PublicShell';
import { SearchView } from './components/SearchView';
import { UserGameReviewPage } from './components/UserGameReviewPage';
import { UserListsPage } from './components/UserListsPage';
import { UserReviewsPage } from './components/UserReviewsPage';
import { VerifyEmailPage } from './components/VerifyEmailPage';
import type { CurrentUser } from './types';

export default function App() {
  const [token, setToken] = useState(getStoredToken());
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [libraryRefreshKey, setLibraryRefreshKey] = useState(0);

  useEffect(() => {
    if (!token) {
      setUser(null);
      return;
    }

    api
      .me()
      .then(setUser)
      .catch(() => {
        clearStoredToken();
        setToken(null);
      });
  }, [token]);

  function handleToken(nextToken: string) {
    setStoredToken(nextToken);
    setToken(nextToken);
  }

  function logout() {
    clearStoredToken();
    setToken(null);
    setUser(null);
  }

  return (
    <BrowserRouter>
      <Routes>
        <Route
          path="/"
          element={<Navigate to={token ? '/library' : '/login'} replace />}
        />
        <Route
          path="/login"
          element={
            token ? <Navigate to="/library" replace /> : <AuthPage mode="login" onToken={handleToken} />
          }
        />
        <Route
          path="/register"
          element={
            token ? <Navigate to="/library" replace /> : <AuthPage mode="register" onToken={handleToken} />
          }
        />
        <Route
          element={token ? <AppShell user={user} onLogout={logout} /> : <PublicShell />}
        >
          <Route path="/games/:gameId/:slug/reviews" element={<GameReviewsPage currentUser={user} />} />
          <Route path="/games/:gameId/reviews" element={<GameReviewsPage currentUser={user} />} />
          <Route path="/u/:username" element={<PublicProfilePage currentUser={user} />} />
          <Route path="/u/:username/reviews" element={<UserReviewsPage currentUser={user} />} />
          <Route path="/u/:username/games/:gameId/:slug" element={<UserGameReviewPage currentUser={user} />} />
          <Route path="/u/:username/games/:gameId" element={<UserGameReviewPage currentUser={user} />} />
          <Route path="/u/:username/lists" element={<UserListsPage />} />
          <Route path="/u/:username/lists/:listId/:slug" element={<ListDetailPage currentUser={user} />} />
          <Route path="/u/:username/lists/:listId" element={<ListDetailPage currentUser={user} />} />
          <Route path="/u/:username/followers" element={<FollowListPage mode="followers" />} />
          <Route path="/u/:username/following" element={<FollowListPage mode="following" />} />
          <Route path="/privacy" element={<PrivacyPage />} />
          <Route path="/about" element={<AboutPage />} />
          <Route path="/verify-email" element={<VerifyEmailPage />} />
        </Route>
        <Route
          element={
            token ? (
              <AppShell user={user} onLogout={logout} />
            ) : (
              <Navigate to="/login" replace />
            )
          }
        >
          <Route
            path="/search"
            element={<SearchView onLibraryChange={() => setLibraryRefreshKey((key) => key + 1)} />}
          />
          <Route path="/library" element={<LibraryView refreshKey={libraryRefreshKey} />} />
          <Route path="/lists" element={<MyListsPage />} />
          <Route path="/lists/popular" element={<PopularListsPage />} />
          <Route path="/notifications" element={<NotificationsPage currentUser={user} />} />
          <Route path="/library/:entryId/:slug" element={<LibraryEntryDetailsPage />} />
          <Route path="/library/:entryId" element={<LibraryEntryDetailsPage />} />
          <Route path="/games/:gameId/:slug" element={<GameDetailsPage />} />
          <Route path="/games/:gameId" element={<GameDetailsPage />} />
          <Route
            path="/admin/comments"
            element={
              !user ? (
                <p className="muted">Loading...</p>
              ) : user.role === 'ADMIN' ? (
                <AdminReportedCommentsPage />
              ) : (
                <Navigate to="/library" replace />
              )
            }
          />
        </Route>
        <Route path="*" element={<Navigate to={token ? '/library' : '/'} replace />} />
      </Routes>
    </BrowserRouter>
  );
}

function AuthPage({ mode, onToken }: { mode: 'login' | 'register'; onToken: (token: string) => void }) {
  return (
    <main className="auth-page">
      <AuthPanel mode={mode} onToken={onToken} />
    </main>
  );
}
