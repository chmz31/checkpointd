import { FormEvent, useState } from 'react';
import { api } from '../api';
import type { AuthMode } from '../constants';

export function AuthPanel({
  mode,
  setMode,
  onToken,
}: {
  mode: AuthMode;
  setMode: (mode: AuthMode) => void;
  onToken: (token: string) => void;
}) {
  const [email, setEmail] = useState('');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setLoading(true);
    setError(null);

    try {
      const response =
        mode === 'login'
          ? await api.login(email, password)
          : await api.register(email, username, password);
      onToken(response.accessToken);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Authentication failed');
    } finally {
      setLoading(false);
    }
  }

  return (
    <section className="auth-layout">
      <form className="panel auth-panel" onSubmit={submit}>
        <div className="section-heading">
          <h2>{mode === 'login' ? 'Login' : 'Create account'}</h2>
          <button
            type="button"
            className="link-button"
            onClick={() => setMode(mode === 'login' ? 'register' : 'login')}
          >
            {mode === 'login' ? 'Register' : 'Login'}
          </button>
        </div>
        <label>
          Email
          <input value={email} onChange={(event) => setEmail(event.target.value)} type="email" required />
        </label>
        {mode === 'register' && (
          <label>
            Username
            <input value={username} onChange={(event) => setUsername(event.target.value)} required />
          </label>
        )}
        <label>
          Password
          <input
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            type="password"
            minLength={8}
            required
          />
        </label>
        {error && <p className="error">{error}</p>}
        <button type="submit" disabled={loading}>
          {loading ? 'Working...' : mode === 'login' ? 'Login' : 'Register'}
        </button>
      </form>
    </section>
  );
}
