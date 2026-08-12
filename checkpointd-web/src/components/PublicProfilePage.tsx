import { FormEvent, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api, clearStoredToken, isApiErrorStatus } from '../api';
import { gamePath, userFollowersPath, userFollowingPath, userListsPath, userReviewsPath } from '../routePaths';
import type { CurrentUser, FollowStatus, ProfileVisibility, PublicProfile } from '../types';
import { CoverImage } from './CoverImage';
import { ChipGroup } from './ChipGroup';
import { DetailFact } from './DetailFact';
import { ReviewCard } from './ReviewCard';
import { formatDate } from '../dateUtils';

export function PublicProfilePage({ currentUser }: { currentUser: CurrentUser | null }) {
  const { username } = useParams();
  const [profile, setProfile] = useState<PublicProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [editing, setEditing] = useState(false);
  const [followStatus, setFollowStatus] = useState<FollowStatus | null>(null);
  const [followLoading, setFollowLoading] = useState(false);
  const [followError, setFollowError] = useState<string | null>(null);

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

  useEffect(() => {
    if (!currentUser || !profile || isOwnProfile) {
      setFollowStatus(null);
      return;
    }

    api
      .getFollowStatus(profile.username)
      .then(setFollowStatus)
      .catch(() => setFollowStatus(null));
  }, [currentUser, profile, isOwnProfile]);

  function handleUpdated(nextProfile: PublicProfile) {
    setProfile(nextProfile);
    setEditing(false);
  }

  async function toggleFollow() {
    if (!profile) return;
    setFollowLoading(true);
    setFollowError(null);

    try {
      const next = followStatus?.following
        ? await api.unfollowUser(profile.username)
        : await api.followUser(profile.username);
      setFollowStatus(next);
      setProfile((current) =>
        current
          ? {
              ...current,
              stats: { ...current.stats, followerCount: next.followerCount, followingCount: next.followingCount },
            }
          : current,
      );
    } catch (caught) {
      setFollowError(caught instanceof Error ? caught.message : 'Could not update follow status');
    } finally {
      setFollowLoading(false);
    }
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
        <div className="profile-identity">
          <h2>{profile.displayName || profile.username}</h2>
          <p className="muted">@{profile.username}</p>
        </div>
        {isOwnProfile && (
          <div className="inline-actions">
            <Link className="nav-link button-small" to={userReviewsPath(profile.username)}>
              Reviews
            </Link>
            <Link className="nav-link button-small" to={userListsPath(profile.username)}>
              Lists
            </Link>
            <Link className="nav-link button-small" to={userFollowersPath(profile.username)}>
              Followers
            </Link>
            <Link className="nav-link button-small" to={userFollowingPath(profile.username)}>
              Following
            </Link>
            <button className="button-ghost" onClick={() => setEditing((current) => !current)}>
              {editing ? 'Close' : 'Edit profile'}
            </button>
          </div>
        )}
        {!isOwnProfile && (
          <div className="inline-actions">
            <Link className="nav-link button-small" to={userReviewsPath(profile.username)}>
              Reviews
            </Link>
            <Link className="nav-link button-small" to={userListsPath(profile.username)}>
              Lists
            </Link>
            <Link className="nav-link button-small" to={userFollowersPath(profile.username)}>
              Followers
            </Link>
            <Link className="nav-link button-small" to={userFollowingPath(profile.username)}>
              Following
            </Link>
            {currentUser && followStatus && (
              <button className="button-ghost button-small" onClick={toggleFollow} disabled={followLoading}>
                {followLoading ? 'Saving...' : followStatus.following ? 'Unfollow' : 'Follow'}
              </button>
            )}
          </div>
        )}
      </div>

      {followError && <p className="error compact-message">{followError}</p>}
      {profile.bio && <p className="profile-bio">{profile.bio}</p>}

      {isOwnProfile && editing && <ProfileEditForm profile={profile} onUpdated={handleUpdated} />}

      {isOwnProfile && editing && <DeleteAccountSection />}

      <div className="detail-facts profile-stats">
        <DetailFact label="Games" value={String(profile.stats.totalGames)} />
        <DetailFact label="Completed" value={String(profile.stats.completedGames)} />
        <DetailFact label="Rated" value={String(profile.stats.ratedGames)} />
        <DetailFact
          label="Average"
          value={profile.stats.averageRating == null ? 'Not set' : profile.stats.averageRating.toFixed(1)}
        />
        <DetailFact label="Reviews" value={String(profile.stats.reviewCount)} />
        <DetailFact label="Followers" value={String(profile.stats.followerCount)} />
        <DetailFact label="Following" value={String(profile.stats.followingCount)} />
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
                  </p>
                  <ChipGroup label="Metadata" values={[...game.genres, ...game.platforms].slice(0, 4)} />
                </div>
              </Link>
            ))}
          </div>
        )}
      </div>

      <div className="detail-section">
        <div className="section-heading">
          <h2>Recent reviews</h2>
          <Link className="nav-link" to={userReviewsPath(profile.username)}>
            View all
          </Link>
        </div>
        {profile.recentReviews.length === 0 ? (
          <div className="empty-state catalog-empty">
            <h3>No reviews yet</h3>
            <p>This profile has not published any public reviews.</p>
          </div>
        ) : (
          <div className="review-list compact-review-list">
            {profile.recentReviews.map((review) => (
              <ReviewCard key={review.id} review={review} />
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

function DeleteAccountSection() {
  const [confirming, setConfirming] = useState(false);
  const [password, setPassword] = useState('');
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setDeleting(true);
    setError(null);

    try {
      await api.deleteMyAccount(password);
      clearStoredToken();
      window.location.href = '/';
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Could not delete account');
      setDeleting(false);
    }
  }

  return (
    <section className="detail-section secondary-actions-section">
      <h3>Danger zone</h3>
      {!confirming ? (
        <>
          <p className="muted">Permanently delete your account and everything you&apos;ve posted.</p>
          <button className="button-danger button-small" onClick={() => setConfirming(true)}>
            Delete my account
          </button>
        </>
      ) : (
        <form className="edit-form" onSubmit={submit}>
          <label className="full-width">
            Confirm your password to permanently delete your account
            <input
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              placeholder="Password"
              required
            />
          </label>
          <p className="muted full-width">
            This immediately and permanently deletes your account, library, reviews, lists, comments, and
            follows. This cannot be undone.
          </p>
          {error && <p className="error compact-message full-width">{error}</p>}
          <div className="inline-actions full-width">
            <button type="submit" className="button-danger" disabled={deleting || !password}>
              {deleting ? 'Deleting...' : 'Permanently delete my account'}
            </button>
            <button
              type="button"
              className="button-ghost"
              onClick={() => {
                setConfirming(false);
                setPassword('');
                setError(null);
              }}
              disabled={deleting}
            >
              Cancel
            </button>
          </div>
        </form>
      )}
    </section>
  );
}
