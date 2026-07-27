import { useEffect, useState } from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { api, clearStoredToken, getStoredToken, setStoredToken } from './api';
import { AuthPanel } from './components/AuthPanel';
import { AppShell } from './components/AppShell';
import { GameDetailsPage } from './components/GameDetailsPage';
import { LibraryEntryDetailsPage } from './components/LibraryEntryDetailsPage';
import { LibraryView } from './components/LibraryView';
import { PublicProfilePage } from './components/PublicProfilePage';
import { PublicShell } from './components/PublicShell';
import { SearchView } from './components/SearchView';
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
          <Route path="/u/:username" element={<PublicProfilePage currentUser={user} />} />
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
          <Route path="/library/:entryId/:slug" element={<LibraryEntryDetailsPage />} />
          <Route path="/library/:entryId" element={<LibraryEntryDetailsPage />} />
          <Route path="/games/:gameId/:slug" element={<GameDetailsPage />} />
          <Route path="/games/:gameId" element={<GameDetailsPage />} />
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
