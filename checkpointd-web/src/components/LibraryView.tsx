import { useEffect, useMemo, useState } from 'react';
import { api } from '../api';
import { libraryStatuses } from '../constants';
import type { LibraryEntry, LibraryStats, LibraryStatus } from '../types';
import { LibraryEntryCard } from './LibraryEntryCard';
import { LibraryStatsPanel } from './LibraryStatsPanel';

type LibrarySortOption = 'updatedDesc' | 'titleAsc' | 'ratingDesc' | 'ratingAsc' | 'status';

export function LibraryView({ refreshKey }: { refreshKey: number }) {
  const [entries, setEntries] = useState<LibraryEntry[]>([]);
  const [stats, setStats] = useState<LibraryStats | null>(null);
  const [statusFilter, setStatusFilter] = useState<LibraryStatus | 'ALL'>('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [sortOption, setSortOption] = useState<LibrarySortOption>('updatedDesc');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const visibleEntries = useMemo(() => {
    const query = searchQuery.trim().toLowerCase();
    const searchedEntries = query
      ? entries.filter((entry) => {
          const title = entry.gameTitle.toLowerCase();
          const notes = entry.notes?.toLowerCase() || '';

          return title.includes(query) || notes.includes(query);
        })
      : entries;

    return [...searchedEntries].sort((first, second) => compareEntries(first, second, sortOption));
  }, [entries, searchQuery, sortOption]);

  const groupedCount = useMemo(() => visibleEntries.length, [visibleEntries]);

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
      <div className="library-controls">
        <label>
          Search
          <input
            value={searchQuery}
            onChange={(event) => setSearchQuery(event.target.value)}
            placeholder="Title or notes"
          />
        </label>
        <label>
          Sort
          <select
            value={sortOption}
            onChange={(event) => setSortOption(event.target.value as LibrarySortOption)}
          >
            <option value="updatedDesc">Recently updated</option>
            <option value="titleAsc">Title A-Z</option>
            <option value="ratingDesc">Rating high-low</option>
            <option value="ratingAsc">Rating low-high</option>
            <option value="status">Status</option>
          </select>
        </label>
      </div>
      {error && <p className="error">{error}</p>}
      {loading && <p className="muted">Loading...</p>}
      {!loading && entries.length === 0 && <p className="empty-state">Your library is empty.</p>}
      {!loading && entries.length > 0 && visibleEntries.length === 0 && (
        <p className="empty-state">No entries match your search.</p>
      )}
      <div className="library-list">
        {visibleEntries.map((entry) => (
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

function compareEntries(first: LibraryEntry, second: LibraryEntry, sortOption: LibrarySortOption) {
  switch (sortOption) {
    case 'titleAsc':
      return first.gameTitle.localeCompare(second.gameTitle);
    case 'ratingDesc':
      return ratingValue(second.rating, -1) - ratingValue(first.rating, -1);
    case 'ratingAsc':
      return ratingValue(first.rating, Number.MAX_SAFE_INTEGER) - ratingValue(second.rating, Number.MAX_SAFE_INTEGER);
    case 'status':
      return first.status.localeCompare(second.status) || first.gameTitle.localeCompare(second.gameTitle);
    case 'updatedDesc':
    default:
      return new Date(second.updatedAt).getTime() - new Date(first.updatedAt).getTime();
  }
}

function ratingValue(rating: number | null | undefined, fallback: number) {
  return rating == null ? fallback : rating;
}
