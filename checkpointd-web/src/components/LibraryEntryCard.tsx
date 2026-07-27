import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api';
import type { LibraryEntry } from '../types';
import { CoverImage } from './CoverImage';
import { GameMetadata } from './GameMetadata';
import { LibraryEntryEditForm } from './LibraryEntryEditForm';

export function LibraryEntryCard({
  entry,
  onUpdated,
  onDelete,
}: {
  entry: LibraryEntry;
  onUpdated: (entry: LibraryEntry) => void;
  onDelete: () => Promise<void>;
}) {
  const [editing, setEditing] = useState(false);
  const [syncing, setSyncing] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  function startEditing() {
    setEditing((current) => !current);
    setMessage(null);
    setError(null);
  }

  function handleSaved(updatedEntry: LibraryEntry) {
    onUpdated(updatedEntry);
    setEditing(false);
    setMessage('Saved.');
  }

  async function syncMetadata() {
    setSyncing(true);
    setMessage(null);
    setError(null);

    try {
      const updatedEntry = await api.syncLibraryEntryMetadata(entry.id);
      onUpdated(updatedEntry);
      setMessage('Metadata refreshed.');
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Could not refresh metadata');
    } finally {
      setSyncing(false);
    }
  }

  async function deleteEntry() {
    setDeleting(true);
    setMessage(null);
    setError(null);

    try {
      await onDelete();
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Could not remove entry');
      setDeleting(false);
    }
  }

  return (
    <article className="library-entry-card">
      <div className="library-entry">
        <CoverImage src={entry.gameCoverUrl} title={entry.gameTitle} />
        <div>
          <h3>{entry.gameTitle}</h3>
          <p className="status-line">
            <span>{entry.status}</span>
            {entry.rating && <span>{entry.rating}/10</span>}
          </p>
          <GameMetadata
            summary={entry.gameSummary}
            genres={entry.gameGenres}
            platforms={entry.gamePlatforms}
          />
          {entry.notes && <p className="notes">{entry.notes}</p>}
          {message && <p className="success compact-message">{message}</p>}
        </div>
        <div className="entry-actions">
          {entry.gameMetadataSyncAvailable && (
            <button className="button-ghost button-small" onClick={syncMetadata} disabled={syncing || deleting}>
              {syncing ? 'Syncing...' : 'Sync'}
            </button>
          )}
          <Link className="nav-link button-small" to={`/library/${entry.id}`}>
            Details
          </Link>
          <button onClick={startEditing} disabled={syncing || deleting}>
            {editing ? 'Close' : 'Edit'}
          </button>
          <button onClick={deleteEntry} disabled={syncing || deleting}>
            {deleting ? 'Deleting...' : 'Delete'}
          </button>
        </div>
      </div>
      {error && !editing && <p className="error compact-message card-message">{error}</p>}

      {editing && (
        <LibraryEntryEditForm entry={entry} onSaved={handleSaved} onCancel={() => setEditing(false)} />
      )}
    </article>
  );
}
