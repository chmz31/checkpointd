package com.chmz31.checkpointd.library.entity;

import com.chmz31.checkpointd.game.entity.Game;
import com.chmz31.checkpointd.library.model.LibraryStatus;
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
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "library_entries", uniqueConstraints = {
		@UniqueConstraint(name = "uk_library_entries_user_game", columnNames = {"user_id", "game_id"})
})
public class LibraryEntry {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "game_id", nullable = false)
	private Game game;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private LibraryStatus status;

	@Column(columnDefinition = "TEXT")
	private String notes;

	@Column(name = "started_at")
	private LocalDate startedAt;

	@Column(name = "completed_at")
	private LocalDate completedAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected LibraryEntry() {
	}

	public LibraryEntry(User user, Game game, LibraryStatus status) {
		this.user = user;
		this.game = game;
		this.status = status;
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

	public void setUser(User user) {
		this.user = user;
	}

	public Game getGame() {
		return game;
	}

	public void setGame(Game game) {
		this.game = game;
	}

	public LibraryStatus getStatus() {
		return status;
	}

	public void setStatus(LibraryStatus status) {
		this.status = status;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public LocalDate getStartedAt() {
		return startedAt;
	}

	public void setStartedAt(LocalDate startedAt) {
		this.startedAt = startedAt;
	}

	public LocalDate getCompletedAt() {
		return completedAt;
	}

	public void setCompletedAt(LocalDate completedAt) {
		this.completedAt = completedAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
