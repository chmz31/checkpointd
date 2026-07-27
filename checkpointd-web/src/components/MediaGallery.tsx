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
  const mediaItems: MediaItem[] = [
    ...artworkUrls.map((url) => ({ kind: 'Artwork' as const, url })),
    ...screenshotUrls.map((url) => ({ kind: 'Screenshot' as const, url })),
  ].slice(0, limit);

  if (mediaItems.length === 0) {
    return null;
  }

  return (
    <section className="detail-section">
      <h3>Media</h3>
      <div className="media-grid">
        {mediaItems.map((item, index) => (
          <figure className="media-tile" key={`${item.kind}-${item.url}-${index}`}>
            <img src={item.url} alt={`${title} ${item.kind.toLowerCase()}`} loading="lazy" />
            <figcaption>{item.kind}</figcaption>
          </figure>
        ))}
      </div>
    </section>
  );
}
