import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { api, isApiErrorStatus } from '../api';

type Status = 'verifying' | 'success' | 'error';

export function VerifyEmailPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const [status, setStatus] = useState<Status>('verifying');
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!token) {
      setStatus('error');
      setError('This verification link is missing its token.');
      return;
    }

    api
      .verifyEmail(token)
      .then(() => setStatus('success'))
      .catch((caught) => {
        setStatus('error');
        setError(
          isApiErrorStatus(caught, 400)
            ? caught instanceof Error
              ? caught.message
              : 'Verification link is invalid or expired.'
            : 'Something went wrong verifying your email. Try again in a moment.',
        );
      });
  }, [token]);

  return (
    <section className="panel">
      <div className="section-heading">
        <h2>Email verification</h2>
      </div>
      {status === 'verifying' && <p className="muted">Verifying your email...</p>}
      {status === 'success' && (
        <div className="empty-state catalog-empty">
          <h3>Email verified</h3>
          <p>Your email is confirmed. You're all set.</p>
          <Link className="nav-link button-small" to="/library">
            Back to your library
          </Link>
        </div>
      )}
      {status === 'error' && (
        <div className="empty-state catalog-empty">
          <h3>Couldn't verify that link</h3>
          <p>{error}</p>
          <Link className="nav-link button-small" to="/library">
            Back to your library
          </Link>
        </div>
      )}
    </section>
  );
}
