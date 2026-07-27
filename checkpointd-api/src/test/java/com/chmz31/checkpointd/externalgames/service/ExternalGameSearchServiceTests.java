package com.chmz31.checkpointd.externalgames.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chmz31.checkpointd.common.exception.ServiceUnavailableException;
import com.chmz31.checkpointd.externalgames.dto.ExternalGameSearchResult;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

@ExtendWith(MockitoExtension.class)
class ExternalGameSearchServiceTests {

	@Mock
	private IgdbClient igdbClient;

	@InjectMocks
	private ExternalGameSearchService externalGameSearchService;

	@Test
	void searchTrimsQueryAndReturnsResults() {
		List<ExternalGameSearchResult> results = List.of(new ExternalGameSearchResult(
				"igdb", "123", "Chrono Trigger", "chrono-trigger", "https://images.example/cover.jpg",
				LocalDate.of(1995, 3, 11)));

		when(igdbClient.search("chrono")).thenReturn(results);

		assertThat(externalGameSearchService.search(" chrono ")).isSameAs(results);
		verify(igdbClient).search("chrono");
	}

	@Test
	void twitchTokenClientRejectsMissingConfiguration() {
		TwitchTokenClient client = new TwitchTokenClient(RestClient.builder(), "", "secret", "https://token.example");

		assertThatThrownBy(client::accessToken)
				.isInstanceOf(ServiceUnavailableException.class)
				.hasMessage("External game provider is not configured");
	}

	@Test
	void twitchTokenClientRejectsMalformedTokenResponse() {
		TwitchTokenClient client = new TwitchTokenClient(
				RestClient.builder(), "client-id", "secret", "https://token.example");

		assertThatThrownBy(() -> client.parseTokenResponse(Map.of("access_token", 123, "expires_in", "bad")))
				.isInstanceOf(ServiceUnavailableException.class)
				.hasMessage("External game provider is unavailable");
	}

	@Test
	void igdbClientRejectsMissingConfiguration() {
		IgdbClient client = new IgdbClient(RestClient.builder(), new TwitchTokenClient(
				RestClient.builder(), "client-id", "secret", "https://token.example"), "");

		assertThatThrownBy(() -> client.search("chrono"))
				.isInstanceOf(ServiceUnavailableException.class)
				.hasMessage("External game provider is not configured");
	}

	@Test
	void igdbClientMapsMetadata() {
		IgdbClient client = new IgdbClient(RestClient.builder(), new TwitchTokenClient(
				RestClient.builder(), "client-id", "secret", "https://token.example"), "https://igdb.example");

		List<ExternalGameSearchResult> results = client.normalize(new Map<?, ?>[] {
				Map.of(
						"id", 123,
						"name", "Chrono Trigger",
						"slug", "chrono-trigger",
						"cover", Map.of("url", "//images.example/low-res-cover.jpg", "image_id", "cover-image-id"),
						"summary", "Time travel RPG",
						"genres", List.of(Map.of("name", "RPG")),
						"platforms", List.of(Map.of("name", "SNES")),
						"screenshots", List.of(Map.of("url", "//images.example/screenshot.jpg")),
						"artworks", List.of(Map.of("image_id", "artwork-image-id")),
						"first_release_date", 795225600)
		});

		assertThat(results).hasSize(1);
		ExternalGameSearchResult result = results.getFirst();
		assertThat(result.coverUrl())
				.isEqualTo("https://images.igdb.com/igdb/image/upload/t_cover_big_2x/cover-image-id.jpg");
		assertThat(result.summary()).isEqualTo("Time travel RPG");
		assertThat(result.genres()).containsExactly("RPG");
		assertThat(result.platforms()).containsExactly("SNES");
		assertThat(result.screenshotUrls()).containsExactly("https://images.example/screenshot.jpg");
		assertThat(result.artworkUrls())
				.containsExactly("https://images.igdb.com/igdb/image/upload/t_1080p/artwork-image-id.jpg");
		assertThat(result.backdropUrl()).isEqualTo("https://images.example/screenshot.jpg");
	}
}
