package com.chmz31.checkpointd.review.entity;

import com.chmz31.checkpointd.game.entity.Game;
import com.chmz31.checkpointd.review.model.ReviewVisibility;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reviews", uniqueConstraints = {
		@UniqueConstraint(name = "uk_reviews_user_game", columnNames = {"user_id", "game_id"})
})
public class Review {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "game_id", nullable = false)
	private Game game;

	private Integer rating;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String body;

	@Column(name = "contains_spoilers", nullable = false)
	private boolean containsSpoilers;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private ReviewVisibility visibility = ReviewVisibility.PUBLIC;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Review() {
	}

	public Review(User user, Game game, String body) {
		this.user = user;
		this.game = game;
		this.body = body;
	}

	@PrePersist
	void onCreate() {
		Instant now = Instant.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		this.updatedAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public Game getGame() {
		return game;
	}

	public Integer getRating() {
		return rating;
	}

	public void setRating(Integer rating) {
		this.rating = rating;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public boolean isContainsSpoilers() {
		return containsSpoilers;
	}

	public void setContainsSpoilers(boolean containsSpoilers) {
		this.containsSpoilers = containsSpoilers;
	}

	public ReviewVisibility getVisibility() {
		return visibility;
	}

	public void setVisibility(ReviewVisibility visibility) {
		this.visibility = visibility;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
