import { useEffect, useState } from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import { api } from '../api';
import { userProfilePath } from '../routePaths';
import type { CurrentUser } from '../types';

const UNREAD_POLL_INTERVAL_MS = 30000;

export function AppShell({ user, onLogout }: { user: CurrentUser | null; onLogout: () => void }) {
  const [unreadCount, setUnreadCount] = useState(0);

  useEffect(() => {
    if (!user) {
      setUnreadCount(0);
      return;
    }

    let cancelled = false;

    function poll() {
      api
        .getUnreadNotificationCount()
        .then((result) => {
          if (!cancelled) setUnreadCount(result.count);
        })
        .catch(() => {
          // Transient network hiccups shouldn't clear the badge.
        });
    }

    poll();
    const interval = window.setInterval(poll, UNREAD_POLL_INTERVAL_MS);

    return () => {
      cancelled = true;
      window.clearInterval(interval);
    };
  }, [user]);

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
            <NavLink className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`} to="/lists/popular">
              Popular
            </NavLink>
            {user && (
              <NavLink className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`} to="/notifications">
                Notifications{unreadCount > 0 ? ` (${unreadCount})` : ''}
              </NavLink>
            )}
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
