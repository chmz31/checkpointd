import { useState } from 'react';
import type { LikeStatus } from '../types';

export function LikeButton({
  liked,
  likeCount,
  onLike,
  onUnlike,
  onChange,
}: {
  liked: boolean;
  likeCount: number;
  onLike: () => Promise<LikeStatus>;
  onUnlike: () => Promise<LikeStatus>;
  onChange: (status: LikeStatus) => void;
}) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function toggle() {
    setLoading(true);
    setError(null);

    try {
      const next = liked ? await onUnlike() : await onLike();
      onChange(next);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Could not update like');
    } finally {
      setLoading(false);
    }
  }

  return (
    <span className="like-button">
      <button className="button-ghost button-small" onClick={toggle} disabled={loading}>
        {loading ? 'Saving...' : liked ? 'Unlike' : 'Like'} ({likeCount})
      </button>
      {error && <span className="error compact-message">{error}</span>}
    </span>
  );
}
