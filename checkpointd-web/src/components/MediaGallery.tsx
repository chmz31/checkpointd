import { useEffect, useState } from 'react';

type MediaItem = {
  kind: 'Artwork' | 'Screenshot';
  url: string;
};

export function MediaGallery({
  title,
  artworkUrls,
  screenshotUrls,
  limit = 12,
}: {
  title: string;
  artworkUrls: string[];
  screenshotUrls: string[];
  limit?: number;
}) {
  const [activeIndex, setActiveIndex] = useState<number | null>(null);
  const mediaItems: MediaItem[] = [
    ...artworkUrls.map((url) => ({ kind: 'Artwork' as const, url })),
    ...screenshotUrls.map((url) => ({ kind: 'Screenshot' as const, url })),
  ].slice(0, limit);
  const activeItem = activeIndex == null ? null : mediaItems[activeIndex];
  const activePosition = activeIndex == null ? 0 : activeIndex + 1;

  useEffect(() => {
    if (activeIndex == null) {
      return;
    }

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        setActiveIndex(null);
      }
      if (event.key === 'ArrowLeft') {
        setActiveIndex((current) => previousIndex(current, mediaItems.length));
      }
      if (event.key === 'ArrowRight') {
        setActiveIndex((current) => nextIndex(current, mediaItems.length));
      }
    }

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [activeIndex, mediaItems.length]);

  if (mediaItems.length === 0) {
    return null;
  }

  return (
    <section className="detail-section">
      <h3>Media</h3>
      <div className="media-grid">
        {mediaItems.map((item, index) => (
          <figure className="media-tile" key={`${item.kind}-${item.url}-${index}`}>
            <button
              className="media-thumb-button"
              type="button"
              onClick={() => setActiveIndex(index)}
              aria-label={`Open ${title} ${item.kind.toLowerCase()} ${index + 1}`}
            >
              <img src={item.url} alt={`${title} ${item.kind.toLowerCase()}`} loading="lazy" />
            </button>
            <figcaption>{item.kind}</figcaption>
          </figure>
        ))}
      </div>
      {activeItem && (
        <div className="lightbox" role="dialog" aria-modal="true" aria-label={`${title} media viewer`}>
          <button
            className="lightbox-backdrop"
            type="button"
            onClick={() => setActiveIndex(null)}
            aria-label="Close media viewer"
          />
          <div className="lightbox-content">
            <div className="lightbox-topbar">
              <p>{`${activeItem.kind} ${activePosition} / ${mediaItems.length}`}</p>
              <button className="button-ghost button-small" type="button" onClick={() => setActiveIndex(null)}>
                Close
              </button>
            </div>
            <div className="lightbox-stage">
              <button
                className="lightbox-nav lightbox-prev"
                type="button"
                onClick={() => setActiveIndex((current) => previousIndex(current, mediaItems.length))}
                aria-label="Previous media item"
              >
                Previous
              </button>
              <img src={activeItem.url} alt={`${title} ${activeItem.kind.toLowerCase()}`} />
              <button
                className="lightbox-nav lightbox-next"
                type="button"
                onClick={() => setActiveIndex((current) => nextIndex(current, mediaItems.length))}
                aria-label="Next media item"
              >
                Next
              </button>
            </div>
          </div>
        </div>
      )}
    </section>
  );
}

function previousIndex(current: number | null, total: number) {
  if (current == null || total === 0) {
    return current;
  }

  return current === 0 ? total - 1 : current - 1;
}

function nextIndex(current: number | null, total: number) {
  if (current == null || total === 0) {
    return current;
  }

  return current === total - 1 ? 0 : current + 1;
}
