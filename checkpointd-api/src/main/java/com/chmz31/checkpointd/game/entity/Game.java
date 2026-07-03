package com.chmz31.checkpointd.game.entity;

import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "games", uniqueConstraints = {
		@UniqueConstraint(name = "uk_games_external_provider_external_id",
				columnNames = {"external_provider", "external_id"})
})
public class Game {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "external_provider")
	private String externalProvider;

	@Column(name = "external_id")
	private String externalId;

	@Column(nullable = false)
	private String title;

	private String slug;

	@Column(name = "cover_url", length = 2048)
	private String coverUrl;

	@Column(name = "release_date")
	private LocalDate releaseDate;

	@Column(columnDefinition = "TEXT")
	private String summary;

	@ElementCollection
	@CollectionTable(name = "game_genres", joinColumns = @JoinColumn(name = "game_id"))
	@OrderColumn(name = "genre_order")
	@Column(name = "genre", nullable = false)
	private List<String> genres = new ArrayList<>();

	@ElementCollection
	@CollectionTable(name = "game_platforms", joinColumns = @JoinColumn(name = "game_id"))
	@OrderColumn(name = "platform_order")
	@Column(name = "platform", nullable = false)
	private List<String> platforms = new ArrayList<>();

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Game() {
	}

	public Game(String title) {
		this.title = title;
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

	public String getExternalProvider() {
		return externalProvider;
	}

	public void setExternalProvider(String externalProvider) {
		this.externalProvider = externalProvider;
	}

	public String getExternalId() {
		return externalId;
	}

	public void setExternalId(String externalId) {
		this.externalId = externalId;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getSlug() {
		return slug;
	}

	public void setSlug(String slug) {
		this.slug = slug;
	}

	public String getCoverUrl() {
		return coverUrl;
	}

	public void setCoverUrl(String coverUrl) {
		this.coverUrl = coverUrl;
	}

	public LocalDate getReleaseDate() {
		return releaseDate;
	}

	public void setReleaseDate(LocalDate releaseDate) {
		this.releaseDate = releaseDate;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public List<String> getGenres() {
		return genres;
	}

	public void setGenres(Collection<String> genres) {
		this.genres = cleanMetadataList(genres);
	}

	public List<String> getPlatforms() {
		return platforms;
	}

	public void setPlatforms(Collection<String> platforms) {
		this.platforms = cleanMetadataList(platforms);
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	private List<String> cleanMetadataList(Collection<String> values) {
		if (values == null) {
			return new ArrayList<>();
		}

		return values.stream()
				.filter(value -> value != null && !value.isBlank())
				.map(String::trim)
				.distinct()
				.collect(Collectors.toCollection(ArrayList::new));
	}
}
