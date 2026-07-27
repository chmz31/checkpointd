import { NavLink, Outlet } from 'react-router-dom';
import type { CurrentUser } from '../types';

export function AppShell({ user, onLogout }: { user: CurrentUser | null; onLogout: () => void }) {
  return (
    <main className="app-shell">
      <header className="app-header">
        <div className="app-header-inner">
          <div className="brand">
            <p className="brand-wordmark">checkpointd</p>
            <p className="brand-tagline">your save file for every game you play.</p>
          </div>
          <nav className="app-nav" aria-label="Primary">
            <NavLink className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`} to="/search">
              Search
            </NavLink>
            <NavLink className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`} to="/library">
              Library
            </NavLink>
            {user && <span className="user-pill">{user.username}</span>}
            <button onClick={onLogout}>Logout</button>
          </nav>
        </div>
      </header>
      <section className="workspace">
        <Outlet />
      </section>
    </main>
  );
}
