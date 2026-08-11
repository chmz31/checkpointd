import { NavLink, Outlet } from 'react-router-dom';
import { userProfilePath } from '../routePaths';
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
            <NavLink className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`} to="/lists">
              Lists
            </NavLink>
            {user?.role === 'ADMIN' && (
              <NavLink className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`} to="/admin/comments">
                Moderation
              </NavLink>
            )}
            {user && (
              <NavLink className="user-pill" to={userProfilePath(user.username)}>
                {user.username}
              </NavLink>
            )}
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
