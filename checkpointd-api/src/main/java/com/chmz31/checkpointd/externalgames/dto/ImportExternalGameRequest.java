package com.chmz31.checkpointd.externalgames.dto;

import jakarta.validation.constraints.NotBlank;

public record ImportExternalGameRequest(
		@NotBlank String provider,
		@NotBlank String externalId) {
}
