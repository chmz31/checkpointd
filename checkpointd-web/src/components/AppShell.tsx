import { NavLink, Outlet } from 'react-router-dom';
import type { CurrentUser } from '../types';

export function AppShell({ user, onLogout }: { user: CurrentUser | null; onLogout: () => void }) {
  return (
    <main className="app-shell">
      <header className="topbar">
        <div>
          <p className="eyebrow">checkpointd</p>
          <h1>your save file for every game you play.</h1>
        </div>
        <nav className="nav-actions" aria-label="Primary">
          <NavLink className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`} to="/search">
            Search
          </NavLink>
          <NavLink className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`} to="/library">
            Library
          </NavLink>
          {user && <span className="user-pill">{user.username}</span>}
          <button onClick={onLogout}>Logout</button>
        </nav>
      </header>
      <section className="workspace">
        <Outlet />
      </section>
    </main>
  );
}
