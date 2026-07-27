package com.chmz31.checkpointd.externalgames.service;

import com.chmz31.checkpointd.common.exception.ServiceUnavailableException;
import com.chmz31.checkpointd.externalgames.dto.ExternalGameSearchResult;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class IgdbClient {

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
}
