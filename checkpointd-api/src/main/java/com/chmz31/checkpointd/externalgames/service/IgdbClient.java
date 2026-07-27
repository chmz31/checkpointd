package com.chmz31.checkpointd.externalgames.service;

import com.chmz31.checkpointd.common.exception.ServiceUnavailableException;
import com.chmz31.checkpointd.externalgames.dto.ExternalGameSearchResult;
import com.chmz31.checkpointd.externalgames.dto.ExternalGameWebsite;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class IgdbClient {

	private static final Logger log = LoggerFactory.getLogger(IgdbClient.class);

	private final RestClient restClient;
	private final TwitchTokenClient twitchTokenClient;
	private final String baseUrl;

	public IgdbClient(
			RestClient.Builder restClientBuilder,
			TwitchTokenClient twitchTokenClient,
			@Value("${checkpointd.integrations.igdb.base-url}") String baseUrl) {
		this.restClient = restClientBuilder.build();
		this.twitchTokenClient = twitchTokenClient;
		this.baseUrl = baseUrl;
	}

	List<ExternalGameSearchResult> search(String query) {
		if (baseUrl == null || baseUrl.isBlank()) {
			throw new ServiceUnavailableException("External game provider is not configured");
		}

		try {
			Map<?, ?>[] response = restClient.post()
					.uri(baseUrl + "/games")
					.header("Client-ID", twitchTokenClient.clientId())
					.header("Authorization", "Bearer " + twitchTokenClient.accessToken())
					.contentType(MediaType.TEXT_PLAIN)
					.accept(MediaType.APPLICATION_JSON)
					.body(searchBody(query))
					.retrieve()
					.body(Map[].class);

			return normalize(response);
		}
		catch (RestClientResponseException exception) {
			logIgdbResponseError(exception);
			throw new ServiceUnavailableException("External game provider is unavailable", exception);
		}
		catch (RestClientException exception) {
			throw new ServiceUnavailableException("External game provider is unavailable", exception);
		}
	}

	ExternalGameSearchResult fetchById(String externalId) {
		if (baseUrl == null || baseUrl.isBlank()) {
			throw new ServiceUnavailableException("External game provider is not configured");
		}

		try {
			Map<?, ?>[] response = restClient.post()
					.uri(baseUrl + "/games")
					.header("Client-ID", twitchTokenClient.clientId())
					.header("Authorization", "Bearer " + twitchTokenClient.accessToken())
					.contentType(MediaType.TEXT_PLAIN)
					.accept(MediaType.APPLICATION_JSON)
					.body(fetchByIdBody(externalId))
					.retrieve()
					.body(Map[].class);

			List<ExternalGameSearchResult> results = normalize(response);

			return results.isEmpty() ? null : results.getFirst();
		}
		catch (RestClientResponseException exception) {
			logIgdbResponseError(exception);
			throw new ServiceUnavailableException("External game provider is unavailable", exception);
		}
		catch (RestClientException exception) {
			throw new ServiceUnavailableException("External game provider is unavailable", exception);
		}
	}

	private String searchBody(String query) {
		return "search \"" + escape(query) + "\"; "
				+ "fields id,name,slug,cover.url,cover.image_id,first_release_date,summary,genres.name,platforms.name; "
				+ "limit 20;";
	}

	private String fetchByIdBody(String externalId) {
		return "fields id,name,slug,cover.url,cover.image_id,first_release_date,summary,genres.name,platforms.name,"
				+ "involved_companies.company.name,involved_companies.developer,involved_companies.publisher,"
				+ "game_modes.name,themes.name,player_perspectives.name,websites.url,websites.trusted,websites.type.type,"
				+ "total_rating,total_rating_count,rating,rating_count,"
				+ "screenshots.url,screenshots.image_id,artworks.url,artworks.image_id; "
				+ "where id = " + externalId + "; "
				+ "limit 1;";
	}

	private String escape(String query) {
		return query.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	List<ExternalGameSearchResult> normalize(Map<?, ?>[] response) {
		List<ExternalGameSearchResult> results = new ArrayList<>();
		if (response == null) {
			return results;
		}

		for (Map<?, ?> item : response) {
			Object id = item.get("id");
			Object name = item.get("name");
			if (id == null || name == null) {
				continue;
			}

			results.add(new ExternalGameSearchResult(
					"igdb",
					String.valueOf(id),
					String.valueOf(name),
					stringValue(item.get("slug")),
					coverUrl(item.get("cover")),
					releaseDate(item.get("first_release_date")),
					stringValue(item.get("summary")),
					names(item.get("genres")),
					names(item.get("platforms")),
					companyNames(item.get("involved_companies"), "developer"),
					companyNames(item.get("involved_companies"), "publisher"),
					names(item.get("game_modes")),
					names(item.get("themes")),
					names(item.get("player_perspectives")),
					websites(item.get("websites")),
					doubleValue(firstPresent(item.get("total_rating"), item.get("rating"))),
					integerValue(firstPresent(item.get("total_rating_count"), item.get("rating_count"))),
					imageUrls(item.get("screenshots")),
					imageUrls(item.get("artworks")),
					backdropUrl(item.get("artworks"), item.get("screenshots"))));
		}

		return results;
	}

	private List<String> names(Object value) {
		if (!(value instanceof List<?> items)) {
			return List.of();
		}

		return items.stream()
				.filter(Map.class::isInstance)
				.map(Map.class::cast)
				.map(item -> stringValue(item.get("name")))
				.filter(name -> name != null && !name.isBlank())
				.map(String::trim)
				.distinct()
				.toList();
	}

	private List<String> companyNames(Object value, String roleFlag) {
		if (!(value instanceof List<?> items)) {
			return List.of();
		}

		return items.stream()
				.filter(Map.class::isInstance)
				.map(Map.class::cast)
				.filter(item -> Boolean.TRUE.equals(item.get(roleFlag)))
				.map(item -> item.get("company"))
				.filter(Map.class::isInstance)
				.map(Map.class::cast)
				.map(company -> stringValue(company.get("name")))
				.filter(name -> name != null && !name.isBlank())
				.map(String::trim)
				.distinct()
				.toList();
	}

	private List<ExternalGameWebsite> websites(Object value) {
		if (!(value instanceof List<?> items)) {
			return List.of();
		}

		Map<String, ExternalGameWebsite> byUrl = new LinkedHashMap<>();
		for (Object valueItem : items) {
			if (!(valueItem instanceof Map<?, ?> item)) {
				continue;
			}

			String url = cleanUrl(stringValue(item.get("url")));
			if (url == null) {
				continue;
			}

			String label = websiteLabel(item, url);
			boolean trusted = Boolean.TRUE.equals(item.get("trusted"));
			byUrl.putIfAbsent(normalizeUrl(url), new ExternalGameWebsite(label, url, trusted));
		}

		return List.copyOf(byUrl.values());
	}

	private String coverUrl(Object cover) {
		if (!(cover instanceof Map<?, ?> coverMap)) {
			return null;
		}
		return imageUrl(coverMap, "t_cover_big_2x");
	}

	private List<String> imageUrls(Object value) {
		if (!(value instanceof List<?> items)) {
			return List.of();
		}

		return items.stream()
				.filter(Map.class::isInstance)
				.map(Map.class::cast)
				.map(image -> imageUrl(image, "t_1080p"))
				.filter(url -> url != null && !url.isBlank())
				.distinct()
				.toList();
	}

	private String backdropUrl(Object artworks, Object screenshots) {
		List<String> screenshotUrls = imageUrls(screenshots);
		if (!screenshotUrls.isEmpty()) {
			return screenshotUrls.getFirst();
		}

		List<String> artworkUrls = imageUrls(artworks);
		return artworkUrls.isEmpty() ? null : artworkUrls.getFirst();
	}

	private String imageUrl(Map<?, ?> image, String size) {
		String imageId = stringValue(image.get("image_id"));
		if (imageId != null && !imageId.isBlank()) {
			return "https://images.igdb.com/igdb/image/upload/" + size + "/" + imageId.trim() + ".jpg";
		}

		String url = stringValue(image.get("url"));
		if (url == null) {
			return null;
		}
		if (url.startsWith("//")) {
			return "https:" + url;
		}

		return url;
	}

	private LocalDate releaseDate(Object value) {
		if (!(value instanceof Number epochSeconds)) {
			return null;
		}

		return Instant.ofEpochSecond(epochSeconds.longValue()).atZone(ZoneOffset.UTC).toLocalDate();
	}

	private String stringValue(Object value) {
		return value == null ? null : String.valueOf(value);
	}

	private String cleanUrl(String url) {
		if (url == null || url.isBlank()) {
			return null;
		}

		String cleaned = url.trim();
		if (cleaned.startsWith("//")) {
			return "https:" + cleaned;
		}

		return cleaned;
	}

	private String websiteLabel(Map<?, ?> item, String url) {
		String typeName = null;
		if (item.get("type") instanceof Map<?, ?> type) {
			typeName = stringValue(type.get("type"));
		}
		else if (item.get("type") instanceof String type) {
			typeName = type;
		}
		if (typeName != null && !typeName.isBlank()) {
			return typeName.trim();
		}

		String inferred = inferWebsiteLabel(url);
		return inferred == null || inferred.isBlank() ? "Website" : inferred;
	}

	private String inferWebsiteLabel(String url) {
		String domain = domain(url);
		if (domain == null || domain.isBlank()) {
			return "Website";
		}

		String normalizedDomain = domain.toLowerCase(Locale.ROOT);
		if (normalizedDomain.endsWith("steampowered.com")) {
			return "Steam";
		}
		if (normalizedDomain.endsWith("gog.com")) {
			return "GOG";
		}
		if (normalizedDomain.endsWith("epicgames.com")) {
			return "Epic Games";
		}
		if (normalizedDomain.endsWith("playstation.com")) {
			return "PlayStation";
		}
		if (normalizedDomain.endsWith("xbox.com")) {
			return "Xbox";
		}
		if (normalizedDomain.endsWith("nintendo.com")) {
			return "Nintendo";
		}
		if (normalizedDomain.endsWith("youtube.com") || normalizedDomain.endsWith("youtu.be")) {
			return "YouTube";
		}
		if (normalizedDomain.endsWith("twitch.tv")) {
			return "Twitch";
		}
		if (normalizedDomain.endsWith("discord.gg") || normalizedDomain.endsWith("discord.com")) {
			return "Discord";
		}
		if (normalizedDomain.endsWith("reddit.com")) {
			return "Reddit";
		}
		if (normalizedDomain.endsWith("wikipedia.org")) {
			return "Wikipedia";
		}

		return cleanDomainLabel(normalizedDomain);
	}

	private String domain(String url) {
		try {
			URI uri = URI.create(url.contains("://") ? url : "https://" + url);
			return uri.getHost();
		}
		catch (IllegalArgumentException exception) {
			return null;
		}
	}

	private String cleanDomainLabel(String domain) {
		String clean = domain.startsWith("www.") ? domain.substring(4) : domain;
		int firstDot = clean.indexOf('.');
		if (firstDot > 0) {
			clean = clean.substring(0, firstDot);
		}
		if (clean.isBlank()) {
			return "Website";
		}

		return clean.substring(0, 1).toUpperCase(Locale.ROOT) + clean.substring(1);
	}

	private String normalizeUrl(String url) {
		return url.trim().replaceAll("/+$", "").toLowerCase(Locale.ROOT);
	}

	private Object firstPresent(Object preferred, Object fallback) {
		return preferred == null ? fallback : preferred;
	}

	private Double doubleValue(Object value) {
		if (!(value instanceof Number number)) {
			return null;
		}

		return number.doubleValue();
	}

	private Integer integerValue(Object value) {
		if (!(value instanceof Number number)) {
			return null;
		}

		return number.intValue();
	}

	private void logIgdbResponseError(RestClientResponseException exception) {
		log.warn("IGDB request failed with status {} and body: {}",
				exception.getStatusCode(), exception.getResponseBodyAsString());
	}
}
