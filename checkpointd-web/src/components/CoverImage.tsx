export function CoverImage({ src, title }: { src?: string | null; title: string }) {
  if (!src) {
    return <div className="cover placeholder" aria-label={`${title} cover`} />;
  }

  return <img className="cover" src={src} alt={`${title} cover`} loading="lazy" />;
}
