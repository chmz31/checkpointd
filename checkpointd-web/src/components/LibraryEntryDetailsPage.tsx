import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { api } from '../api';
import type { LibraryEntry } from '../types';
import { CoverImage } from './CoverImage';
import { LibraryEntryEditForm } from './LibraryEntryEditForm';

export function LibraryEntryDetailsPage() {
  const { entryId } = useParams();
  const navigate = useNavigate();
  const [entry, setEntry] = useState<LibraryEntry | null>(null);
  const [editing, setEditing] = useState(false);
  const [loading, setLoading] = useState(true);
  const [syncing, setSyncing] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!entryId) return;

    setLoading(true);
    setError(null);
    api
      .getLibraryEntry(entryId)
      .then(setEntry)
      .catch((caught) => {
        setError(caught instanceof Error ? caught.message : 'Could not load library entry');
      })
      .finally(() => setLoading(false));
  }, [entryId]);

  async function syncMetadata() {
    if (!entry) return;

    setSyncing(true);
    setMessage(null);
    setError(null);

    try {
      const updatedEntry = await api.syncLibraryEntryMetadata(entry.id);
      setEntry(updatedEntry);
      setMessage('Metadata synced.');
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Could not sync metadata');
    } finally {
      setSyncing(false);
    }
  }

  async function deleteEntry() {
    if (!entry) return;

    setDeleting(true);
    setMessage(null);
    setError(null);

    try {
      await api.deleteLibraryEntry(entry.id);
      navigate('/library');
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Could not remove entry');
      setDeleting(false);
    }
  }

  function handleSaved(updatedEntry: LibraryEntry) {
    setEntry(updatedEntry);
    setEditing(false);
    setMessage('Saved.');
  }

  if (loading) {
    return <p className="muted">Loading entry...</p>;
  }

  if (!entry) {
    return (
      <section className="panel">
        <p className="error">{error || 'Library entry not found'}</p>
        <Link className="nav-link" to="/library">
          Back to library
        </Link>
      </section>
    );
  }

  return (
    <article className="detail-page">
      {entry.gameCoverUrl && (
        <div
          className="detail-backdrop"
          aria-hidden="true"
          style={{ backgroundImage: `url(${entry.gameCoverUrl})` }}
        />
      )}
      <div className="detail-content">
        <aside className="detail-poster">
          <CoverImage src={entry.gameCoverUrl} title={entry.gameTitle} />
        </aside>
        <section className="detail-main">
          <div className="detail-heading">
            <div>
              <p className="eyebrow">Library entry</p>
              <h2>{entry.gameTitle}</h2>
              <p className="muted">{entry.gameSlug || 'Library entry'}</p>
            </div>
            <div className="inline-actions detail-actions">
              <Link className="nav-link button-small" to="/library">
                Back to library
              </Link>
              {entry.gameMetadataSyncAvailable && (
                <button className="button-ghost button-small" onClick={syncMetadata} disabled={syncing || deleting}>
                  {syncing ? 'Syncing...' : 'Sync'}
                </button>
              )}
              <button onClick={() => setEditing((current) => !current)} disabled={deleting}>
                {editing ? 'Close' : 'Edit'}
              </button>
              <button onClick={deleteEntry} disabled={deleting}>
                {deleting ? 'Deleting...' : 'Delete'}
              </button>
            </div>
          </div>

          <div className="detail-facts">
            <Fact label="Status" value={entry.status} />
            <Fact label="Rating" value={entry.rating ? `${entry.rating}/10` : 'Unrated'} />
            <Fact label="Started" value={formatDate(entry.startedAt)} />
            <Fact label="Completed" value={formatDate(entry.completedAt)} />
            <Fact label="Added" value={formatDate(entry.createdAt)} />
            <Fact label="Updated" value={formatDate(entry.updatedAt)} />
          </div>

          {message && <p className="success compact-message">{message}</p>}
          {error && <p className="error compact-message">{error}</p>}

          {editing && (
            <LibraryEntryEditForm entry={entry} onSaved={handleSaved} onCancel={() => setEditing(false)} />
          )}

          {entry.gameSummary && (
            <section className="detail-section">
              <h3>Summary</h3>
              <p>{entry.gameSummary}</p>
            </section>
          )}

          {(entry.gameGenres.length > 0 || entry.gamePlatforms.length > 0) && (
            <section className="detail-section">
              <h3>Metadata</h3>
              {entry.gameGenres.length > 0 && <ChipGroup label="Genres" values={entry.gameGenres} />}
              {entry.gamePlatforms.length > 0 && <ChipGroup label="Platforms" values={entry.gamePlatforms} />}
            </section>
          )}

          {entry.notes && (
            <section className="detail-section">
              <h3>Notes</h3>
              <p>{entry.notes}</p>
            </section>
          )}
        </section>
      </div>
    </article>
  );
}

function Fact({ label, value }: { label: string; value: string }) {
  return (
    <div className="detail-fact">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function ChipGroup({ label, values }: { label: string; values: string[] }) {
  return (
    <div className="detail-chip-group">
      <p className="muted">{label}</p>
      <div className="chip-list">
        {values.map((value, index) => (
          <span className="metadata-chip" key={`${label}-${value}-${index}`}>
            {value}
          </span>
        ))}
      </div>
    </div>
  );
}

function formatDate(value?: string | null) {
  if (!value) {
    return 'Not set';
  }

  return new Intl.DateTimeFormat(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  }).format(new Date(value));
}
