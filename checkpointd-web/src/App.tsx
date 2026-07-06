import { useEffect, useState } from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { api, clearStoredToken, getStoredToken, setStoredToken } from './api';
import { AuthPanel } from './components/AuthPanel';
import { AppShell } from './components/AppShell';
import { LibraryView } from './components/LibraryView';
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
        </Route>
        <Route path="*" element={<Navigate to={token ? '/library' : '/'} replace />} />
      </Routes>
    </BrowserRouter>
  );
}

function AuthPage({ mode, onToken }: { mode: 'login' | 'register'; onToken: (token: string) => void }) {
  return (
    <main className="app-shell">
      <AuthPanel mode={mode} onToken={onToken} />
    </main>
  );
}
