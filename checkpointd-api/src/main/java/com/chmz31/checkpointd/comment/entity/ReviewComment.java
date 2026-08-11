package com.chmz31.checkpointd.comment.entity;

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
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "review_comments")
public class ReviewComment {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "review_id", nullable = false)
	private Review review;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String body;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected ReviewComment() {
	}

	public ReviewComment(User user, Review review, String body) {
		this.user = user;
		this.review = review;
		this.body = body;
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

	public String getBody() {
		return body;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
