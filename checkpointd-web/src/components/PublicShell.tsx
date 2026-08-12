import { Link, Outlet } from 'react-router-dom';

export function PublicShell() {
  return (
    <main className="app-shell">
      <header className="app-header">
        <div className="app-header-inner">
          <div className="brand">
            <p className="brand-wordmark">checkpointd</p>
            <p className="brand-tagline">your save file for every game you play.</p>
          </div>
          <nav className="app-nav" aria-label="Public">
            <Link className="nav-link" to="/login">
              Login
            </Link>
            <Link className="nav-link" to="/register">
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
