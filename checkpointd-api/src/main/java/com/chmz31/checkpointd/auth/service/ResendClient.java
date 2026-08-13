package com.chmz31.checkpointd.auth.service;

import com.chmz31.checkpointd.common.exception.ServiceUnavailableException;
import java.util.List;
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
public class ResendClient {

	private static final Logger log = LoggerFactory.getLogger(ResendClient.class);
	private static final String BASE_URL = "https://api.resend.com";

	private final RestClient restClient;
	private final String apiKey;
	private final String fromEmail;

	public ResendClient(
			RestClient.Builder restClientBuilder,
			@Value("${checkpointd.integrations.resend.api-key}") String apiKey,
			@Value("${checkpointd.integrations.resend.from-email}") String fromEmail) {
		this.restClient = restClientBuilder.baseUrl(BASE_URL).build();
		this.apiKey = apiKey;
		this.fromEmail = fromEmail;
	}

	void sendVerificationEmail(String toEmail, String verifyUrl) {
		if (apiKey == null || apiKey.isBlank()) {
			throw new ServiceUnavailableException("Email provider is not configured");
		}

		try {
			restClient.post()
					.uri("/emails")
					.header("Authorization", "Bearer " + apiKey)
					.contentType(MediaType.APPLICATION_JSON)
					.body(Map.of(
							"from", fromEmail,
							"to", List.of(toEmail),
							"subject", "Verify your checkpointd account",
							"html", verificationEmailHtml(verifyUrl)))
					.retrieve()
					.toBodilessEntity();
		}
		catch (RestClientResponseException exception) {
			log.warn("Resend request failed with status {} and body: {}",
					exception.getStatusCode(), exception.getResponseBodyAsString());
			throw new ServiceUnavailableException("Email provider is unavailable", exception);
		}
		catch (RestClientException exception) {
			throw new ServiceUnavailableException("Email provider is unavailable", exception);
		}
	}

	private String verificationEmailHtml(String verifyUrl) {
		return "<p>Welcome to checkpointd — confirm your email to finish securing your account.</p>"
				+ "<p><a href=\"" + verifyUrl + "\">Verify your email</a></p>"
				+ "<p>This link expires in 24 hours. If you didn't create a checkpointd account, you can ignore this email.</p>";
	}
}
