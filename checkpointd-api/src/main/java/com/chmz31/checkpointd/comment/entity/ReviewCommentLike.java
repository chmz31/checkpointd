package com.chmz31.checkpointd.comment.entity;

import com.chmz31.checkpointd.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "review_comment_likes", uniqueConstraints = {
		@UniqueConstraint(name = "uk_review_comment_likes_user_comment", columnNames = {"user_id", "comment_id"})
})
public class ReviewCommentLike {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "comment_id", nullable = false)
	private ReviewComment comment;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected ReviewCommentLike() {
	}

	public ReviewCommentLike(User user, ReviewComment comment) {
		this.user = user;
		this.comment = comment;
	}

	@PrePersist
	void onCreate() {
		this.createdAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public ReviewComment getComment() {
		return comment;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
