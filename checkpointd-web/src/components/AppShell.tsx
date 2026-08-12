import { useEffect, useState } from 'react';
import { Link, NavLink, Outlet } from 'react-router-dom';
import { api } from '../api';
import { userProfilePath } from '../routePaths';
import type { CurrentUser } from '../types';

const UNREAD_POLL_INTERVAL_MS = 30000;

export function AppShell({ user, onLogout }: { user: CurrentUser | null; onLogout: () => void }) {
  const [unreadCount, setUnreadCount] = useState(0);
  const [menuOpen, setMenuOpen] = useState(false);

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

  function closeMenu() {
    setMenuOpen(false);
  }

  return (
    <main className="app-shell">
      <header className="app-header">
        <div className="app-header-inner">
          <div className="brand">
            <p className="brand-wordmark">checkpointd</p>
            <p className="brand-tagline">your save file for every game you play.</p>
          </div>
          <button
            type="button"
            className="nav-toggle"
            onClick={() => setMenuOpen((current) => !current)}
            aria-expanded={menuOpen}
            aria-label="Toggle navigation menu"
          >
            {menuOpen ? 'Close' : 'Menu'}
          </button>
          <nav className={`app-nav collapsible${menuOpen ? ' open' : ''}`} aria-label="Primary">
            <NavLink
              className={({ isActive }) => `primary-nav-link${isActive ? ' active' : ''}`}
              to="/search"
              onClick={closeMenu}
            >
              Search
            </NavLink>
            <NavLink
              className={({ isActive }) => `primary-nav-link${isActive ? ' active' : ''}`}
              to="/library"
              onClick={closeMenu}
            >
              Library
            </NavLink>
            <NavLink
              className={({ isActive }) => `primary-nav-link${isActive ? ' active' : ''}`}
              to="/lists"
              onClick={closeMenu}
            >
              Lists
            </NavLink>
            <NavLink
              className={({ isActive }) => `primary-nav-link${isActive ? ' active' : ''}`}
              to="/lists/popular"
              onClick={closeMenu}
            >
              Popular
            </NavLink>
            {user && (
              <NavLink
                className={({ isActive }) => `primary-nav-link${isActive ? ' active' : ''}`}
                to="/notifications"
                onClick={closeMenu}
              >
                Notifications
                {unreadCount > 0 && (
                  <span className="notification-badge">{unreadCount > 99 ? '99+' : unreadCount}</span>
                )}
              </NavLink>
            )}
            {user?.role === 'ADMIN' && (
              <NavLink
                className={({ isActive }) => `primary-nav-link${isActive ? ' active' : ''}`}
                to="/admin/comments"
                onClick={closeMenu}
              >
                Moderation
              </NavLink>
            )}
            {user && (
              <NavLink className="primary-nav-link" to={userProfilePath(user.username)} onClick={closeMenu}>
                {user.username}
              </NavLink>
            )}
            <button
              className="primary-nav-link"
              onClick={() => {
                closeMenu();
                onLogout();
              }}
            >
              Logout
            </button>
          </nav>
        </div>
      </header>
      <section className="workspace">
        <Outlet />
      </section>
      <footer className="app-footer">
        <Link className="inline-link" to="/about">
          About
        </Link>
        <Link className="inline-link" to="/privacy">
          Privacy
        </Link>
      </footer>
    </main>
  );
}
