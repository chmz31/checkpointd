import { useState } from 'react';
import { Link } from 'react-router-dom';
import { libraryEntryPath } from '../routePaths';
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
    <article className={`library-entry-card${editing ? ' expanded' : ''}`}>
      <div className="library-entry">
        <Link className="cover-link" to={libraryEntryPath(entry)} aria-label={`View ${entry.gameTitle} details`}>
          <CoverImage src={entry.gameCoverUrl} title={entry.gameTitle} />
        </Link>
        <div className="entry-card-body">
          <h3>{entry.gameTitle}</h3>
          <p className="status-line">
            <span className="status-badge">{entry.status}</span>
          </p>
          <GameMetadata
            summary={entry.gameSummary}
            genres={entry.gameGenres}
            platforms={entry.gamePlatforms}
          />
          {entry.notes && <p className="notes entry-notes">{entry.notes}</p>}
          {message && <p className="success compact-message">{message}</p>}
        </div>
        <div className="entry-actions">
          <Link className="nav-link button-small" to={libraryEntryPath(entry)}>
            Details
          </Link>
          <button onClick={startEditing} disabled={deleting}>
            {editing ? 'Close' : 'Edit'}
          </button>
          <button className="button-danger" onClick={deleteEntry} disabled={deleting}>
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
