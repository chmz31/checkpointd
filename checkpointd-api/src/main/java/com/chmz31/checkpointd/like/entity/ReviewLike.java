package com.chmz31.checkpointd.like.entity;

import com.chmz31.checkpointd.review.entity.Review;
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
@Table(name = "review_likes", uniqueConstraints = {
		@UniqueConstraint(name = "uk_review_likes_user_review", columnNames = {"user_id", "review_id"})
})
public class ReviewLike {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "review_id", nullable = false)
	private Review review;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected ReviewLike() {
	}

	public ReviewLike(User user, Review review) {
		this.user = user;
		this.review = review;
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

	public Review getReview() {
		return review;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
