import type { AppView } from '../constants';
import type { CurrentUser } from '../types';

export function Header({
  user,
  view,
  setView,
  onLogout,
}: {
  user: CurrentUser | null;
  view: AppView;
  setView: (view: AppView) => void;
  onLogout: () => void;
}) {
  return (
    <header className="topbar">
      <div>
        <p className="eyebrow">checkpointd</p>
        <h1>your save file for every game you play.</h1>
      </div>
      {user && (
        <nav className="nav-actions" aria-label="Primary">
          <button className={view === 'search' ? 'active' : ''} onClick={() => setView('search')}>
            Search
          </button>
          <button className={view === 'library' ? 'active' : ''} onClick={() => setView('library')}>
            Library
          </button>
          <span className="user-pill">{user.username}</span>
          <button onClick={onLogout}>Logout</button>
        </nav>
      )}
    </header>
  );
}
