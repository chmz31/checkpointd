package com.chmz31.checkpointd.like.entity;

import com.chmz31.checkpointd.list.entity.GameList;
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
@Table(name = "list_likes", uniqueConstraints = {
		@UniqueConstraint(name = "uk_list_likes_user_list", columnNames = {"user_id", "list_id"})
})
public class ListLike {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "list_id", nullable = false)
	private GameList list;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected ListLike() {
	}

	public ListLike(User user, GameList list) {
		this.user = user;
		this.list = list;
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

	public GameList getList() {
		return list;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
