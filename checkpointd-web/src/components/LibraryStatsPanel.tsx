import type { LibraryStats } from '../types';

const statItems: Array<{ label: string; value: (stats: LibraryStats) => string | number }> = [
  { label: 'Total', value: (stats) => stats.totalEntries },
  { label: 'Wishlist', value: (stats) => stats.wishlistCount },
  { label: 'Backlog', value: (stats) => stats.backlogCount },
  { label: 'Playing', value: (stats) => stats.playingCount },
  { label: 'Completed', value: (stats) => stats.completedCount },
  { label: 'Dropped', value: (stats) => stats.droppedCount },
  { label: 'Paused', value: (stats) => stats.pausedCount },
  { label: 'Rated', value: (stats) => stats.ratedCount },
  {
    label: 'Average',
    value: (stats) => (stats.averageRating == null ? 'N/A' : stats.averageRating.toFixed(1)),
  },
];

export function LibraryStatsPanel({ stats }: { stats: LibraryStats | null }) {
  if (!stats) {
    return <p className="muted">Loading stats...</p>;
  }

  return (
    <div className="stats-grid" aria-label="Library stats">
      {statItems.map((item) => (
        <div className="stat-tile" key={item.label}>
          <span>{item.label}</span>
          <strong>{item.value(stats)}</strong>
        </div>
      ))}
    </div>
  );
}
