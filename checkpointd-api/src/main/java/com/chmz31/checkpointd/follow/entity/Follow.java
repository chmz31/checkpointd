package com.chmz31.checkpointd.follow.entity;

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
@Table(name = "follows", uniqueConstraints = {
		@UniqueConstraint(name = "uk_follows_follower_followee", columnNames = {"follower_id", "followee_id"})
})
public class Follow {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "follower_id", nullable = false)
	private User follower;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "followee_id", nullable = false)
	private User followee;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected Follow() {
	}

	public Follow(User follower, User followee) {
		this.follower = follower;
		this.followee = followee;
	}

	@PrePersist
	void onCreate() {
		this.createdAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public User getFollower() {
		return follower;
	}

	public User getFollowee() {
		return followee;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
