export function ChipGroup({ label, values }: { label: string; values: string[] }) {
  if (values.length === 0) {
    return null;
  }

  return (
    <div className="detail-chip-group">
      <p className="muted">{label}</p>
      <div className="chip-list">
        {values.map((value, index) => (
          <span className="metadata-chip" key={`${label}-${value}-${index}`}>
            {value}
          </span>
        ))}
      </div>
    </div>
  );
}
