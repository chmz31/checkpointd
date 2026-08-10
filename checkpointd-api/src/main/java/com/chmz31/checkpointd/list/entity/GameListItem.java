package com.chmz31.checkpointd.list.entity;

import com.chmz31.checkpointd.game.entity.Game;
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
@Table(name = "game_list_items", uniqueConstraints = {
		@UniqueConstraint(name = "uk_game_list_items_list_game", columnNames = {"list_id", "game_id"})
})
public class GameListItem {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "list_id", nullable = false)
	private GameList list;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "game_id", nullable = false)
	private Game game;

	@Column(nullable = false)
	private int position;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected GameListItem() {
	}

	public GameListItem(GameList list, Game game, int position) {
		this.list = list;
		this.game = game;
		this.position = position;
	}

	@PrePersist
	void onCreate() {
		this.createdAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public GameList getList() {
		return list;
	}

	public Game getGame() {
		return game;
	}

	public int getPosition() {
		return position;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
