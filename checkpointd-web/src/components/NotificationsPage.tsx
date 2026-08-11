import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api';
import { formatDate } from '../dateUtils';
import { listPath, userGameReviewPath, userProfilePath } from '../routePaths';
import type { CurrentUser, NotificationItem, PaginatedResponse } from '../types';

export function NotificationsPage({ currentUser }: { currentUser: CurrentUser | null }) {
  const [notifications, setNotifications] = useState<PaginatedResponse<NotificationItem> | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setLoading(true);
    setError(null);
    api
      .getNotifications(page, 20)
      .then(setNotifications)
      .catch((caught) => setError(caught instanceof Error ? caught.message : 'Could not load notifications'))
      .finally(() => setLoading(false));
  }, [page]);

  useEffect(() => {
    api.markAllNotificationsAsRead().catch(() => {
      // Best-effort: an unread badge that doesn't clear immediately isn't worth surfacing an error for.
    });
  }, []);

  if (!currentUser) {
    return <p className="muted">Loading...</p>;
  }

  return (
    <section className="panel review-page">
      <div className="section-heading">
        <h2>Notifications</h2>
      </div>
      {error && <p className="error compact-message">{error}</p>}
      {loading && <p className="muted">Loading notifications...</p>}
      {!loading && notifications?.content.length === 0 && (
        <div className="empty-state catalog-empty">
          <h3>No notifications yet</h3>
          <p>Follows, likes, comments, and replies will show up here.</p>
        </div>
      )}
      <div className="review-list">
        {notifications?.content.map((notification) => {
          const { message, to } = describeNotification(notification, currentUser.username);
          return (
            <Link
              key={notification.id}
              className={`comment-item notification-item${notification.read ? '' : ' unread'}`}
              to={to}
            >
              <p className="review-text">{message}</p>
              <p className="muted">{formatDate(notification.createdAt)}</p>
            </Link>
          );
        })}
      </div>
      {notifications && notifications.totalPages > 1 && (
        <div className="pagination-controls">
          <button onClick={() => setPage((current) => Math.max(current - 1, 0))} disabled={notifications.first}>
            Previous
          </button>
          <span className="muted">
            Page {notifications.page + 1} of {notifications.totalPages}
          </span>
          <button onClick={() => setPage((current) => current + 1)} disabled={notifications.last}>
            Next
          </button>
        </div>
      )}
    </section>
  );
}

function describeNotification(notification: NotificationItem, currentUsername: string) {
  const actor = notification.actorDisplayName || notification.actorUsername;

  switch (notification.type) {
    case 'FOLLOW':
      return { message: `${actor} started following you`, to: userProfilePath(notification.actorUsername) };
    case 'LIST_LIKE':
      return { message: `${actor} liked your list "${notification.listName}"`, to: listLink(notification, currentUsername) };
    case 'REVIEW_LIKE':
      return {
        message: `${actor} liked your review of ${notification.gameTitle}`,
        to: reviewLink(notification, currentUsername),
      };
    case 'LIST_COMMENT':
      return {
        message: `${actor} commented on your list "${notification.listName}"`,
        to: listLink(notification, currentUsername),
      };
    case 'REVIEW_COMMENT':
      return {
        message: `${actor} commented on your review of ${notification.gameTitle}`,
        to: reviewLink(notification, currentUsername),
      };
    case 'COMMENT_REPLY':
      return notification.listId
        ? {
            message: `${actor} replied to your comment on "${notification.listName}"`,
            to: listLink(notification, currentUsername),
          }
        : {
            message: `${actor} replied to your comment on your review of ${notification.gameTitle}`,
            to: reviewLink(notification, currentUsername),
          };
    case 'LIST_COMMENT_LIKE':
      return {
        message: `${actor} liked your comment on "${notification.listName}"`,
        to: listLink(notification, currentUsername),
      };
    case 'REVIEW_COMMENT_LIKE':
      return {
        message: `${actor} liked your comment on your review of ${notification.gameTitle}`,
        to: reviewLink(notification, currentUsername),
      };
    default:
      return { message: actor, to: userProfilePath(notification.actorUsername) };
  }
}

function listLink(notification: NotificationItem, username: string) {
  if (!notification.listId || !notification.listName) {
    return userProfilePath(username);
  }
  return listPath(username, { id: notification.listId, name: notification.listName });
}

function reviewLink(notification: NotificationItem, username: string) {
  if (!notification.reviewId || !notification.gameId) {
    return userProfilePath(username);
  }
  return userGameReviewPath(username, {
    id: notification.gameId,
    slug: notification.gameSlug ?? null,
    title: notification.gameTitle || 'game',
  });
}
