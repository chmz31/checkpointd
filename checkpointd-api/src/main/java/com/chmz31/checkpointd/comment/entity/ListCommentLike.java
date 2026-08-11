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
@Table(name = "list_comment_likes", uniqueConstraints = {
		@UniqueConstraint(name = "uk_list_comment_likes_user_comment", columnNames = {"user_id", "comment_id"})
})
public class ListCommentLike {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "comment_id", nullable = false)
	private ListComment comment;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected ListCommentLike() {
	}

	public ListCommentLike(User user, ListComment comment) {
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

	public ListComment getComment() {
		return comment;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
