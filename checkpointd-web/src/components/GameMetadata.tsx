export function GameMetadata({
  summary,
  genres,
  platforms,
}: {
  summary?: string | null;
  genres?: string[];
  platforms?: string[];
}) {
  const chips = [
    ...(genres || []).map((name) => ({ kind: 'Genre', name })),
    ...(platforms || []).map((name) => ({ kind: 'Platform', name })),
  ];

  if (!summary && chips.length === 0) {
    return null;
  }

  return (
    <div className="metadata-block">
      {chips.length > 0 && (
        <div className="chip-list" aria-label="Game metadata">
          {chips.slice(0, 6).map((chip, index) => (
            <span className="metadata-chip" key={`${chip.kind}-${chip.name}-${index}`}>
              {chip.name}
            </span>
          ))}
        </div>
      )}
      {summary && <p className="summary-preview">{summary}</p>}
    </div>
  );
}
