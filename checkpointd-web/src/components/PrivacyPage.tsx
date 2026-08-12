export function PrivacyPage() {
  return (
    <section className="panel">
      <div className="section-heading">
        <h2>Privacy</h2>
      </div>
      <div className="detail-section">
        <h3>What we collect</h3>
        <p>
          When you register, checkpointd stores your email address, username, and a hashed copy of your
          password. Everything else — your library entries, reviews, lists, comments, likes, and follows — is
          the content you create by using the app.
        </p>
      </div>
      <div className="detail-section">
        <h3>Passwords</h3>
        <p>
          Passwords are hashed with bcrypt before they're ever stored. This is a one-way process — nobody,
          including the site operator, can recover your actual password from what's stored in the database.
        </p>
      </div>
      <div className="detail-section">
        <h3>Who has access</h3>
        <p>
          checkpointd is currently operated by a single admin, who has direct access to the database to run
          and maintain the site. That access does not extend to your password itself (see above), but does
          mean the operator can, technically, view or modify other account data as needed to operate the
          service. Your data is never sold or shared with third parties.
        </p>
      </div>
      <div className="detail-section">
        <h3>External services</h3>
        <p>
          Game search and metadata are fetched from IGDB via the Twitch API, from checkpointd's backend only —
          your browser never contacts IGDB directly, and your account credentials are never sent to it. Search
          queries aren't stored beyond the local game catalog checkpointd builds from the results.
        </p>
      </div>
      <div className="detail-section">
        <h3>Deleting your account</h3>
        <p>
          You can permanently delete your account and everything you've posted at any time from your profile's
          "Edit profile" screen, under "Danger zone." This is immediate and cannot be undone.
        </p>
      </div>
      <div className="detail-section">
        <h3>Contact</h3>
        <p>
          Questions about your data, or requests we haven't covered above, can go to{' '}
          <a className="inline-link" href="mailto:cmanriquezanetti@gmail.com">
            cmanriquezanetti@gmail.com
          </a>
          .
        </p>
      </div>
    </section>
  );
}
