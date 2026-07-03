import { useEffect, useMemo, useState } from 'react';
import { api } from '../api';
import { libraryStatuses } from '../constants';
import type { LibraryEntry, LibraryStats, LibraryStatus } from '../types';
import { LibraryEntryCard } from './LibraryEntryCard';
import { LibraryStatsPanel } from './LibraryStatsPanel';

export function LibraryView({ refreshKey }: { refreshKey: number }) {
  const [entries, setEntries] = useState<LibraryEntry[]>([]);
  const [stats, setStats] = useState<LibraryStats | null>(null);
  const [statusFilter, setStatusFilter] = useState<LibraryStatus | 'ALL'>('ALL');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const groupedCount = useMemo(() => entries.length, [entries]);

  async function loadLibrary() {
    setLoading(true);
    setError(null);

    try {
      const [nextEntries, nextStats] = await Promise.all([
        api.listLibrary(statusFilter === 'ALL' ? undefined : statusFilter),
        api.getLibraryStats(),
      ]);
      setEntries(nextEntries);
      setStats(nextStats);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Could not load library');
    } finally {
      setLoading(false);
    }
  }

  async function deleteEntry(entryId: string) {
    setError(null);
    try {
      await api.deleteLibraryEntry(entryId);
      setEntries((current) => current.filter((entry) => entry.id !== entryId));
      await loadStats();
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Could not remove entry');
    }
  }

  async function loadStats() {
    setStats(await api.getLibraryStats());
  }

  function replaceEntry(updatedEntry: LibraryEntry) {
    setEntries((current) => {
      if (statusFilter !== 'ALL' && updatedEntry.status !== statusFilter) {
        return current.filter((entry) => entry.id !== updatedEntry.id);
      }

      return current.map((entry) => (entry.id === updatedEntry.id ? updatedEntry : entry));
    });
    loadStats().catch((caught) => {
      setError(caught instanceof Error ? caught.message : 'Could not load library stats');
    });
  }

  useEffect(() => {
    loadLibrary();
  }, [refreshKey, statusFilter]);

  return (
    <section className="panel">
      <div className="section-heading">
        <h2>Library</h2>
        <div className="inline-actions">
          <span className="muted">{groupedCount} entries</span>
          <select
            className="compact-select"
            value={statusFilter}
            onChange={(event) => setStatusFilter(event.target.value as LibraryStatus | 'ALL')}
            aria-label="Filter library by status"
          >
            <option value="ALL">ALL</option>
            {libraryStatuses.map((libraryStatus) => (
              <option key={libraryStatus} value={libraryStatus}>
                {libraryStatus}
              </option>
            ))}
          </select>
          <button onClick={loadLibrary} disabled={loading}>
            Refresh
          </button>
        </div>
      </div>
      <LibraryStatsPanel stats={stats} />
      {error && <p className="error">{error}</p>}
      {loading && <p className="muted">Loading...</p>}
      {!loading && entries.length === 0 && <p className="empty-state">Your library is empty.</p>}
      <div className="library-list">
        {entries.map((entry) => (
          <LibraryEntryCard
            key={entry.id}
            entry={entry}
            onUpdated={replaceEntry}
            onDelete={() => deleteEntry(entry.id)}
          />
        ))}
      </div>
    </section>
  );
}
