import { useEffect, useState } from 'react';
import { api, clearStoredToken, getStoredToken, setStoredToken } from './api';
import { AuthPanel } from './components/AuthPanel';
import { Header } from './components/Header';
import { LibraryView } from './components/LibraryView';
import { SearchView } from './components/SearchView';
import type { AppView, AuthMode } from './constants';
import type { CurrentUser } from './types';

export default function App() {
  const [token, setToken] = useState(getStoredToken());
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [authMode, setAuthMode] = useState<AuthMode>('login');
  const [view, setView] = useState<AppView>('search');
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
    setView('search');
  }

  return (
    <main className="app-shell">
      <Header user={user} view={view} setView={setView} onLogout={logout} />
      {!token ? (
        <AuthPanel mode={authMode} setMode={setAuthMode} onToken={handleToken} />
      ) : (
        <section className="workspace">
          {view === 'search' ? (
            <SearchView onLibraryChange={() => setLibraryRefreshKey((key) => key + 1)} />
          ) : (
            <LibraryView refreshKey={libraryRefreshKey} />
          )}
        </section>
      )}
    </main>
  );
}
