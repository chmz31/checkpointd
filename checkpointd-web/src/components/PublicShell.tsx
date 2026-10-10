import { Link, NavLink, Outlet } from 'react-router-dom';

export function PublicShell() {
  return (
    <main className="app-shell">
      <header className="app-header">
        <div className="app-header-inner">
          <Link className="brand brand-link" to="/">
            <p className="brand-wordmark">checkpointd</p>
            <p className="brand-tagline">your save file for every game you play.</p>
          </Link>
          <nav className="app-nav" aria-label="Public">
            <NavLink
              className={({ isActive }) => `primary-nav-link${isActive ? ' active' : ''}`}
              to="/lists/popular"
            >
              Popular
            </NavLink>
            <Link className="primary-nav-link" to="/login">
              Login
            </Link>
            <Link className="primary-nav-link" to="/register">
              Register
            </Link>
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
