package com.chmz31.checkpointd.auth.entity;

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
@Table(name = "email_verification_tokens")
public class EmailVerificationToken {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false)
	private String token;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected EmailVerificationToken() {
	}

	public EmailVerificationToken(User user, String token, Instant expiresAt) {
		this.user = user;
		this.token = token;
		this.expiresAt = expiresAt;
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

	public String getToken() {
		return token;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
