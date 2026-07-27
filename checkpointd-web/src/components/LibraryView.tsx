import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { api } from '../api';
import { libraryStatuses } from '../constants';
import type { LibraryEntry, LibrarySortOption, LibraryStats, LibraryStatus, PaginatedResponse } from '../types';
import { LibraryEntryCard } from './LibraryEntryCard';
import { LibraryStatsPanel } from './LibraryStatsPanel';

const PAGE_SIZE = 24;

export function LibraryView({ refreshKey }: { refreshKey: number }) {
  const [entries, setEntries] = useState<LibraryEntry[]>([]);
  const [pageData, setPageData] = useState<PaginatedResponse<LibraryEntry> | null>(null);
  const [stats, setStats] = useState<LibraryStats | null>(null);
  const [statusFilter, setStatusFilter] = useState<LibraryStatus | 'ALL'>('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [activeSearchQuery, setActiveSearchQuery] = useState('');
  const [sortOption, setSortOption] = useState<LibrarySortOption>('UPDATED_DESC');
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const visibleCount = entries.length;
  const totalElements = pageData?.totalElements ?? 0;
  const totalPages = pageData?.totalPages ?? 0;
  const currentPage = pageData?.page ?? page;
  const hasActiveControls = statusFilter !== 'ALL' || Boolean(activeSearchQuery);

  async function loadLibrary() {
    setLoading(true);
    setError(null);

    try {
      const [nextPage, nextStats] = await Promise.all([
        api.listLibrary({
          page,
          size: PAGE_SIZE,
          status: statusFilter === 'ALL' ? undefined : statusFilter,
          q: activeSearchQuery,
          sort: sortOption,
        }),
        api.getLibraryStats(),
      ]);
      setEntries(nextPage.content);
      setPageData(nextPage);
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
      if (entries.length === 1 && page > 0) {
        setPage((current) => Math.max(current - 1, 0));
      } else {
        await loadLibrary();
      }
      await loadStats();
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Could not remove entry');
      throw caught;
    }
  }

  async function loadStats() {
    setStats(await api.getLibraryStats());
  }

  function replaceEntry(updatedEntry: LibraryEntry) {
    setEntries((current) => current.map((entry) => (entry.id === updatedEntry.id ? updatedEntry : entry)));
    loadLibrary().catch((caught) => {
      setError(caught instanceof Error ? caught.message : 'Could not refresh library');
    });
    loadStats().catch((caught) => {
      setError(caught instanceof Error ? caught.message : 'Could not load library stats');
    });
  }

  function handleSearchSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPage(0);
    setActiveSearchQuery(searchQuery.trim());
  }

  function handleStatusChange(nextStatus: LibraryStatus | 'ALL') {
    setStatusFilter(nextStatus);
    setPage(0);
  }

  function handleSortChange(nextSort: LibrarySortOption) {
    setSortOption(nextSort);
    setPage(0);
  }

  useEffect(() => {
    loadLibrary();
  }, [refreshKey, statusFilter, activeSearchQuery, sortOption, page]);

  return (
    <section className="panel">
      <div className="section-heading">
        <h2>Library</h2>
        <div className="inline-actions">
          <span className="muted">
            {visibleCount} visible of {totalElements}
          </span>
          <select
            className="compact-select"
            value={statusFilter}
            onChange={(event) => handleStatusChange(event.target.value as LibraryStatus | 'ALL')}
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
            {loading ? 'Refreshing...' : 'Refresh'}
          </button>
        </div>
      </div>
      <LibraryStatsPanel stats={stats} />
      <form className="library-controls" onSubmit={handleSearchSubmit}>
        <label>
          Search
          <input
            value={searchQuery}
            onChange={(event) => setSearchQuery(event.target.value)}
            placeholder="Title or notes"
          />
        </label>
        <button type="submit" disabled={loading}>
          Search
        </button>
        <label>
          Sort
          <select
            value={sortOption}
            onChange={(event) => handleSortChange(event.target.value as LibrarySortOption)}
          >
            <option value="UPDATED_DESC">Recently updated</option>
            <option value="TITLE_ASC">Title A-Z</option>
            <option value="RATING_DESC">Rating high-low</option>
            <option value="RATING_ASC">Rating low-high</option>
            <option value="STATUS_ASC">Status</option>
          </select>
        </label>
      </form>
      {error && <p className="error">{error}</p>}
      {loading && (
        <div className="empty-state catalog-empty">
          <h3>Loading library...</h3>
          <p>Pulling your latest checkpoint cards.</p>
        </div>
      )}
      {!loading && entries.length === 0 && !hasActiveControls && (
        <div className="empty-state catalog-empty">
          <h3>Your library is ready for its first game</h3>
          <p>Search for a game and add it to start tracking your status, notes, and play dates.</p>
        </div>
      )}
      {!loading && entries.length === 0 && hasActiveControls && (
        <div className="empty-state catalog-empty">
          <h3>No matching entries</h3>
          <p>Adjust the search, filter, or sort controls to bring more of your library back into view.</p>
        </div>
      )}
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
      {!loading && totalPages > 1 && (
        <div className="pagination-controls" aria-label="Library pagination">
          <button onClick={() => setPage((current) => Math.max(current - 1, 0))} disabled={pageData?.first ?? true}>
            Previous
          </button>
          <span className="muted">
            Page {currentPage + 1} of {totalPages}
          </span>
          <button
            onClick={() => setPage((current) => current + 1)}
            disabled={pageData?.last ?? true}
          >
            Next
          </button>
        </div>
      )}
    </section>
  );
}
