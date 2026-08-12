export function AboutPage() {
  return (
    <section className="panel">
      <div className="section-heading">
        <h2>About</h2>
      </div>
      <div className="detail-section">
        {/* Placeholder — replace with your own framing/bio. */}
        <p>
          checkpointd is a video game backlog and library app — track what you're playing, write reviews,
          build lists, and follow other players.
        </p>
        <p>It's an early, actively-developed solo project. Expect frequent changes.</p>
        <p>
          Source code:{' '}
          <a className="inline-link" href="https://github.com/chmz31/checkpointd" target="_blank" rel="noreferrer">
            github.com/chmz31/checkpointd
          </a>
        </p>
      </div>
    </section>
  );
}
