package com.chmz31.checkpointd.game.entity;

import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.BatchSize;
import com.chmz31.checkpointd.game.model.MetadataSyncStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
	@BatchSize(size = 20)
	@CollectionTable(name = "game_genres", joinColumns = @JoinColumn(name = "game_id"))
	@OrderColumn(name = "genre_order")
	@Column(name = "genre", nullable = false)
	private List<String> genres = new ArrayList<>();

	@ElementCollection
	@BatchSize(size = 20)
	@CollectionTable(name = "game_platforms", joinColumns = @JoinColumn(name = "game_id"))
	@OrderColumn(name = "platform_order")
	@Column(name = "platform", nullable = false)
	private List<String> platforms = new ArrayList<>();

	@ElementCollection
	@BatchSize(size = 20)
	@CollectionTable(name = "game_developers", joinColumns = @JoinColumn(name = "game_id"))
	@OrderColumn(name = "developer_order")
	@Column(name = "developer", nullable = false)
	private List<String> developers = new ArrayList<>();

	@ElementCollection
	@BatchSize(size = 20)
	@CollectionTable(name = "game_publishers", joinColumns = @JoinColumn(name = "game_id"))
	@OrderColumn(name = "publisher_order")
	@Column(name = "publisher", nullable = false)
	private List<String> publishers = new ArrayList<>();

	@ElementCollection
	@BatchSize(size = 20)
	@CollectionTable(name = "game_modes", joinColumns = @JoinColumn(name = "game_id"))
	@OrderColumn(name = "mode_order")
	@Column(name = "mode", nullable = false)
	private List<String> gameModes = new ArrayList<>();

	@ElementCollection
	@BatchSize(size = 20)
	@CollectionTable(name = "game_themes", joinColumns = @JoinColumn(name = "game_id"))
	@OrderColumn(name = "theme_order")
	@Column(name = "theme", nullable = false)
	private List<String> themes = new ArrayList<>();

	@ElementCollection
	@BatchSize(size = 20)
	@CollectionTable(name = "game_player_perspectives", joinColumns = @JoinColumn(name = "game_id"))
	@OrderColumn(name = "perspective_order")
	@Column(name = "player_perspective", nullable = false)
	private List<String> playerPerspectives = new ArrayList<>();

	@ElementCollection
	@BatchSize(size = 20)
	@CollectionTable(name = "game_websites", joinColumns = @JoinColumn(name = "game_id"))
	@OrderColumn(name = "website_order")
	private List<GameWebsite> websites = new ArrayList<>();

	@ElementCollection
	@BatchSize(size = 20)
	@CollectionTable(name = "game_screenshots", joinColumns = @JoinColumn(name = "game_id"))
	@OrderColumn(name = "screenshot_order")
	@Column(name = "screenshot_url", nullable = false, length = 2048)
	private List<String> screenshotUrls = new ArrayList<>();

	@ElementCollection
	@BatchSize(size = 20)
	@CollectionTable(name = "game_artworks", joinColumns = @JoinColumn(name = "game_id"))
	@OrderColumn(name = "artwork_order")
	@Column(name = "artwork_url", nullable = false, length = 2048)
	private List<String> artworkUrls = new ArrayList<>();

	@Column(name = "backdrop_url", length = 2048)
	private String backdropUrl;

	@Column(name = "external_rating")
	private Double externalRating;

	@Column(name = "external_rating_count")
	private Integer externalRatingCount;

	@Column(name = "metadata_synced_at")
	private Instant metadataSyncedAt;

	@Column(name = "metadata_sync_attempted_at")
	private Instant metadataSyncAttemptedAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "metadata_sync_status", length = 40)
	private MetadataSyncStatus metadataSyncStatus;

	@Column(name = "metadata_sync_error", columnDefinition = "TEXT")
	private String metadataSyncError;

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

	public List<String> getDevelopers() {
		return developers;
	}

	public void setDevelopers(Collection<String> developers) {
		this.developers = cleanMetadataList(developers);
	}

	public List<String> getPublishers() {
		return publishers;
	}

	public void setPublishers(Collection<String> publishers) {
		this.publishers = cleanMetadataList(publishers);
	}

	public List<String> getGameModes() {
		return gameModes;
	}

	public void setGameModes(Collection<String> gameModes) {
		this.gameModes = cleanMetadataList(gameModes);
	}

	public List<String> getThemes() {
		return themes;
	}

	public void setThemes(Collection<String> themes) {
		this.themes = cleanMetadataList(themes);
	}

	public List<String> getPlayerPerspectives() {
		return playerPerspectives;
	}

	public void setPlayerPerspectives(Collection<String> playerPerspectives) {
		this.playerPerspectives = cleanMetadataList(playerPerspectives);
	}

	public List<GameWebsite> getWebsites() {
		return websites;
	}

	public void setWebsites(Collection<GameWebsite> websites) {
		this.websites = cleanWebsites(websites);
	}

	public List<String> getScreenshotUrls() {
		return screenshotUrls;
	}

	public void setScreenshotUrls(Collection<String> screenshotUrls) {
		this.screenshotUrls = cleanMetadataList(screenshotUrls);
	}

	public List<String> getArtworkUrls() {
		return artworkUrls;
	}

	public void setArtworkUrls(Collection<String> artworkUrls) {
		this.artworkUrls = cleanMetadataList(artworkUrls);
	}

	public String getBackdropUrl() {
		return backdropUrl;
	}

	public void setBackdropUrl(String backdropUrl) {
		this.backdropUrl = backdropUrl;
	}

	public Double getExternalRating() {
		return externalRating;
	}

	public void setExternalRating(Double externalRating) {
		this.externalRating = externalRating;
	}

	public Integer getExternalRatingCount() {
		return externalRatingCount;
	}

	public void setExternalRatingCount(Integer externalRatingCount) {
		this.externalRatingCount = externalRatingCount;
	}

	public Instant getMetadataSyncedAt() {
		return metadataSyncedAt;
	}

	public void setMetadataSyncedAt(Instant metadataSyncedAt) {
		this.metadataSyncedAt = metadataSyncedAt;
	}

	public Instant getMetadataSyncAttemptedAt() {
		return metadataSyncAttemptedAt;
	}

	public void setMetadataSyncAttemptedAt(Instant metadataSyncAttemptedAt) {
		this.metadataSyncAttemptedAt = metadataSyncAttemptedAt;
	}

	public MetadataSyncStatus getMetadataSyncStatus() {
		return metadataSyncStatus;
	}

	public void setMetadataSyncStatus(MetadataSyncStatus metadataSyncStatus) {
		this.metadataSyncStatus = metadataSyncStatus;
	}

	public String getMetadataSyncError() {
		return metadataSyncError;
	}

	public void setMetadataSyncError(String metadataSyncError) {
		this.metadataSyncError = metadataSyncError;
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

	private List<GameWebsite> cleanWebsites(Collection<GameWebsite> values) {
		if (values == null) {
			return new ArrayList<>();
		}

		Map<String, GameWebsite> byUrl = new LinkedHashMap<>();
		for (GameWebsite website : values) {
			if (website == null || website.getUrl() == null || website.getUrl().isBlank()) {
				continue;
			}
			String url = website.getUrl().trim();
			String label = website.getLabel() == null || website.getLabel().isBlank()
					? "Website" : website.getLabel().trim();
			byUrl.putIfAbsent(normalizeUrl(url), new GameWebsite(label, url, website.isTrusted()));
		}

		return new ArrayList<>(byUrl.values());
	}

	private String normalizeUrl(String url) {
		return url.trim().replaceAll("/+$", "").toLowerCase(Locale.ROOT);
	}
}
