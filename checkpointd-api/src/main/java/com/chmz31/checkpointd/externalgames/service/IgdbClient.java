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
				+ "fields id,name,slug,cover.url,first_release_date; "
				+ "limit 20;";
	}

	private String fetchByIdBody(String externalId) {
		return "fields id,name,slug,cover.url,first_release_date; "
				+ "where id = " + externalId + "; "
				+ "limit 1;";
	}

	private String escape(String query) {
		return query.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	private List<ExternalGameSearchResult> normalize(Map<?, ?>[] response) {
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
					releaseDate(item.get("first_release_date"))));
		}

		return results;
	}

	private String coverUrl(Object cover) {
		if (!(cover instanceof Map<?, ?> coverMap)) {
			return null;
		}
		String url = stringValue(coverMap.get("url"));
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
