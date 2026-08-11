package com.chmz31.checkpointd.notification.entity;

import com.chmz31.checkpointd.list.entity.GameList;
import com.chmz31.checkpointd.notification.model.NotificationType;
import com.chmz31.checkpointd.review.entity.Review;
import com.chmz31.checkpointd.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "recipient_id", nullable = false)
	private User recipient;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "actor_id", nullable = false)
	private User actor;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private NotificationType type;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "list_id")
	private GameList list;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "review_id")
	private Review review;

	@Column(nullable = false)
	private boolean read;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected Notification() {
	}

	public Notification(User recipient, User actor, NotificationType type, GameList list, Review review) {
		this.recipient = recipient;
		this.actor = actor;
		this.type = type;
		this.list = list;
		this.review = review;
	}

	@PrePersist
	void onCreate() {
		this.createdAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public User getRecipient() {
		return recipient;
	}

	public User getActor() {
		return actor;
	}

	public NotificationType getType() {
		return type;
	}

	public GameList getList() {
		return list;
	}

	public Review getReview() {
		return review;
	}

	public boolean isRead() {
		return read;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
