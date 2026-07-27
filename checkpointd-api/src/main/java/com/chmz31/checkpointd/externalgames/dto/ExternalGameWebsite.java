package com.chmz31.checkpointd.externalgames.dto;

public record ExternalGameWebsite(
		String label,
		String url,
		boolean trusted) {
}
