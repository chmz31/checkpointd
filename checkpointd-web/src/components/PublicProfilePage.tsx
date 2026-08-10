import { FormEvent, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api, isApiErrorStatus } from '../api';
import { gamePath, userReviewsPath } from '../routePaths';
import type { CurrentUser, ProfileVisibility, PublicProfile } from '../types';
import { CoverImage } from './CoverImage';
import { ChipGroup } from './ChipGroup';
import { DetailFact } from './DetailFact';
import { formatDate } from '../dateUtils';

export function PublicProfilePage({ currentUser }: { currentUser: CurrentUser | null }) {
  const { username } = useParams();
  const [profile, setProfile] = useState<PublicProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [editing, setEditing] = useState(false);

  const isOwnProfile = Boolean(currentUser && profile && currentUser.username === profile.username);

  useEffect(() => {
    async function loadProfile() {
      if (!username) {
        setError('Profile not found');
        setLoading(false);
        return;
      }

      setLoading(true);
      setError(null);
      setEditing(false);

      try {
        setProfile(await api.getPublicProfile(username));
      } catch (caught) {
        if (currentUser?.username === username && isApiErrorStatus(caught, 404)) {
          try {
            setProfile(await api.getMyProfile());
          } catch (ownProfileError) {
            setProfile(null);
            setError(ownProfileError instanceof Error ? ownProfileError.message : 'Profile not found');
          }
        } else {
          setProfile(null);
          setError(caught instanceof Error ? caught.message : 'Profile not found');
        }
      } finally {
        setLoading(false);
      }
    }

    loadProfile();
  }, [username, currentUser]);

  function handleUpdated(nextProfile: PublicProfile) {
    setProfile(nextProfile);
    setEditing(false);
  }

  if (loading) {
    return (
      <section className="panel">
        <div className="empty-state catalog-empty">
          <h3>Loading profile...</h3>
          <p>Pulling this save file into view.</p>
        </div>
      </section>
    );
  }

  if (!profile) {
    return (
      <section className="panel">
        <div className="empty-state catalog-empty">
          <h3>Profile not found</h3>
          <p>{error || 'This profile is private or does not exist.'}</p>
        </div>
      </section>
    );
  }

  return (
    <section className="panel profile-page">
      <div className="profile-heading">
        <div>
          <h2>{profile.displayName || profile.username}</h2>
          <p className="muted">@{profile.username}</p>
        </div>
        {isOwnProfile && (
          <div className="inline-actions">
            <Link className="nav-link button-small" to={userReviewsPath(profile.username)}>
              Reviews
            </Link>
            <button className="button-ghost" onClick={() => setEditing((current) => !current)}>
              {editing ? 'Close' : 'Edit profile'}
            </button>
          </div>
        )}
        {!isOwnProfile && (
          <Link className="nav-link button-small" to={userReviewsPath(profile.username)}>
            Reviews
          </Link>
        )}
      </div>

      {profile.bio && <p className="profile-bio">{profile.bio}</p>}

      {isOwnProfile && editing && <ProfileEditForm profile={profile} onUpdated={handleUpdated} />}

      <div className="detail-facts profile-stats">
        <DetailFact label="Games" value={String(profile.stats.totalGames)} />
        <DetailFact label="Completed" value={String(profile.stats.completedGames)} />
        <DetailFact label="Rated" value={String(profile.stats.ratedGames)} />
        <DetailFact
          label="Average"
          value={profile.stats.averageRating == null ? 'Not set' : profile.stats.averageRating.toFixed(1)}
        />
        <DetailFact label="Joined" value={formatDate(profile.joinedAt)} />
        {isOwnProfile && <DetailFact label="Visibility" value={profile.profileVisibility} />}
      </div>

      <div className="detail-section">
        <div className="section-heading">
          <h2>Recent games</h2>
        </div>
        {profile.recentGames.length === 0 ? (
          <div className="empty-state catalog-empty">
            <h3>No games yet</h3>
            <p>This profile has not shared any recent games.</p>
          </div>
        ) : (
          <div className="profile-game-grid">
            {profile.recentGames.map((game) => (
              <Link
                className="profile-game-card"
                key={game.libraryEntryId}
                to={gamePath({ id: game.gameId, slug: game.gameSlug, title: game.gameTitle })}
              >
                <CoverImage src={game.gameCoverUrl} title={game.gameTitle} />
                <div>
                  <h3>{game.gameTitle}</h3>
                  <p className="status-line">
                    <span className="status-badge">{game.status}</span>
                    {game.rating && <span className="rating-badge">{game.rating}/10</span>}
                  </p>
                  <ChipGroup label="Metadata" values={[...game.genres, ...game.platforms].slice(0, 4)} />
                </div>
              </Link>
            ))}
          </div>
        )}
      </div>
    </section>
  );
}

function ProfileEditForm({
  profile,
  onUpdated,
}: {
  profile: PublicProfile;
  onUpdated: (profile: PublicProfile) => void;
}) {
  const [displayName, setDisplayName] = useState(profile.displayName || '');
  const [bio, setBio] = useState(profile.bio || '');
  const [profileVisibility, setProfileVisibility] = useState<ProfileVisibility>(profile.profileVisibility);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setError(null);

    try {
      onUpdated(await api.updateMyProfile({
        displayName: displayName.trim() || null,
        bio: bio.trim() || null,
        profileVisibility,
      }));
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Could not update profile');
    } finally {
      setSaving(false);
    }
  }

  return (
    <form className="edit-form profile-edit-form" onSubmit={submit}>
      <label>
        Display name
        <input
          value={displayName}
          maxLength={80}
          onChange={(event) => setDisplayName(event.target.value)}
          placeholder="How your name appears publicly"
        />
      </label>
      <label>
        Visibility
        <select
          value={profileVisibility}
          onChange={(event) => setProfileVisibility(event.target.value as ProfileVisibility)}
        >
          <option value="PUBLIC">PUBLIC</option>
          <option value="PRIVATE">PRIVATE</option>
        </select>
      </label>
      <label className="full-width">
        Bio
        <textarea
          value={bio}
          maxLength={1000}
          rows={4}
          onChange={(event) => setBio(event.target.value)}
          placeholder="A short note about what you play"
        />
      </label>
      {error && <p className="error compact-message full-width">{error}</p>}
      <button type="submit" disabled={saving}>
        {saving ? 'Saving...' : 'Save profile'}
      </button>
    </form>
  );
}
