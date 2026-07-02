package com.chmz31.checkpointd.externalgames.service;

import com.chmz31.checkpointd.common.exception.ServiceUnavailableException;
import java.time.Instant;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class TwitchTokenClient {

	private final RestClient restClient;
	private final String clientId;
	private final String clientSecret;
	private final String tokenUrl;
	private CachedToken cachedToken;

	public TwitchTokenClient(
			RestClient.Builder restClientBuilder,
			@Value("${checkpointd.integrations.igdb.client-id}") String clientId,
			@Value("${checkpointd.integrations.igdb.client-secret}") String clientSecret,
			@Value("${checkpointd.integrations.igdb.twitch-token-url}") String tokenUrl) {
		this.restClient = restClientBuilder.build();
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		this.tokenUrl = tokenUrl;
	}

	synchronized String accessToken() {
		if (cachedToken != null && cachedToken.expiresAt().isAfter(Instant.now().plusSeconds(60))) {
			return cachedToken.value();
		}

		requireConfigured(clientId);
		requireConfigured(clientSecret);
		requireConfigured(tokenUrl);

		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("client_id", clientId);
		form.add("client_secret", clientSecret);
		form.add("grant_type", "client_credentials");

		try {
			Map<?, ?> response = restClient.post()
					.uri(tokenUrl)
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.body(form)
					.retrieve()
					.body(Map.class);

			cachedToken = parseTokenResponse(response);
			return cachedToken.value();
		}
		catch (RestClientException exception) {
			throw new ServiceUnavailableException("External game provider is unavailable", exception);
		}
	}

	String clientId() {
		requireConfigured(clientId);

		return clientId;
	}

	private void requireConfigured(String value) {
		if (value == null || value.isBlank()) {
			throw new ServiceUnavailableException("External game provider is not configured");
		}
	}

	CachedToken parseTokenResponse(Map<?, ?> response) {
		if (response == null) {
			throw new ServiceUnavailableException("External game provider is unavailable");
		}
		Object token = response.get("access_token");
		Object expiresIn = response.get("expires_in");

		if (!(token instanceof String tokenValue) || tokenValue.isBlank() || !(expiresIn instanceof Number expiresInValue)) {
			throw new ServiceUnavailableException("External game provider is unavailable");
		}

		return new CachedToken(tokenValue, Instant.now().plusSeconds(expiresInValue.longValue()));
	}

	record CachedToken(String value, Instant expiresAt) {
	}
}
