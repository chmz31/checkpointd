import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { api } from '../api';
import { formatDate } from '../dateUtils';
import { canonicalGamePath, libraryEntryPath, slugify } from '../routePaths';
import type { LibraryEntry } from '../types';
import { CoverImage } from './CoverImage';
import { DetailFact } from './DetailFact';
import { LibraryEntryEditForm } from './LibraryEntryEditForm';

export function LibraryEntryDetailsPage() {
  const { entryId, slug } = useParams();
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
      .then((loadedEntry) => {
        setEntry(loadedEntry);
        const canonicalPath = libraryEntryPath(loadedEntry);
        const canonicalSlug = slugify(loadedEntry.gameSlug || loadedEntry.gameTitle || 'game');
        if (slug !== canonicalSlug) {
          navigate(canonicalPath, { replace: true });
        }
      })
      .catch((caught) => {
        setError(caught instanceof Error ? caught.message : 'Could not load library entry');
      })
      .finally(() => setLoading(false));
  }, [entryId, navigate, slug]);

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
      <div className="detail-content">
        <aside className="detail-poster">
          <CoverImage src={entry.gameCoverUrl} title={entry.gameTitle} />
        </aside>
        <section className="detail-main">
          <div className="detail-heading">
            <div>
              <p className="eyebrow">Your checkpoint</p>
              <h2>{entry.gameTitle}</h2>
              <p className="muted">{entry.gameSlug || 'Library entry'}</p>
            </div>
            <div className="inline-actions detail-actions">
              <Link className="nav-link button-small" to="/library">
                Back to library
              </Link>
              <Link
                className="nav-link button-small"
                to={canonicalGamePath(entry.gameId, entry.gameSlug, entry.gameTitle)}
              >
                View game page
              </Link>
              <button onClick={() => setEditing((current) => !current)} disabled={deleting}>
                {editing ? 'Close' : 'Edit'}
              </button>
              <button className="button-danger" onClick={deleteEntry} disabled={deleting}>
                {deleting ? 'Deleting...' : 'Delete'}
              </button>
            </div>
          </div>

          <div className="detail-facts">
            <DetailFact label="Status" value={entry.status} />
            <DetailFact label="Rating" value={entry.rating ? `${entry.rating}/10` : 'Unrated'} />
            <DetailFact label="Started" value={formatDate(entry.startedAt)} />
            <DetailFact label="Completed" value={formatDate(entry.completedAt)} />
            <DetailFact label="Added" value={formatDate(entry.createdAt)} />
            <DetailFact label="Updated" value={formatDate(entry.updatedAt)} />
          </div>

          <MetadataStatus entry={entry} syncing={syncing} deleting={deleting} onRetry={syncMetadata} />

          {message && <p className="success compact-message">{message}</p>}
          {error && <p className="error compact-message">{error}</p>}

          {editing && (
            <LibraryEntryEditForm entry={entry} onSaved={handleSaved} onCancel={() => setEditing(false)} />
          )}

          {entry.notes && (
            <section className="detail-section notes-section">
              <h3>Notes</h3>
              <p>{entry.notes}</p>
            </section>
          )}

          {entry.gameMetadataSyncAvailable && entry.gameMetadataSyncStatus !== 'FAILED' && (
            <section className="detail-section secondary-actions-section">
              <h3>Metadata</h3>
              <p className="muted">Catalog details refresh automatically when they look stale.</p>
              <div className="inline-actions">
                <button className="button-ghost button-small" onClick={syncMetadata} disabled={syncing || deleting}>
                  {syncing ? 'Refreshing...' : 'Refresh metadata'}
                </button>
              </div>
            </section>
          )}
        </section>
      </div>
    </article>
  );
}

function MetadataStatus({
  entry,
  syncing,
  deleting,
  onRetry,
}: {
  entry: LibraryEntry;
  syncing: boolean;
  deleting: boolean;
  onRetry: () => void;
}) {
  if (entry.gameMetadataSyncStatus === 'REFRESHING') {
    return <p className="metadata-status">Catalog metadata is refreshing...</p>;
  }
  if (entry.gameMetadataSyncStatus === 'FAILED') {
    return (
      <div className="metadata-status metadata-status-warning metadata-status-row">
        <span>Metadata refresh failed. {entry.gameMetadataSyncError || 'You can retry now.'}</span>
        {entry.gameMetadataSyncAvailable && (
          <button className="button-ghost button-small" onClick={onRetry} disabled={syncing || deleting}>
            {syncing ? 'Retrying...' : 'Retry'}
          </button>
        )}
      </div>
    );
  }
  if (entry.gameMetadataSyncStatus === 'SUCCESS' && entry.gameMetadataSyncedAt && !entry.gameMetadataStale) {
    return <p className="metadata-status">Catalog metadata updated {formatDate(entry.gameMetadataSyncedAt)}.</p>;
  }
  if (entry.gameMetadataStale) {
    return <p className="metadata-status">Catalog metadata is queued for refresh.</p>;
  }

  return null;
}
