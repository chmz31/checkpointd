package com.chmz31.checkpointd.game.dto;

import com.chmz31.checkpointd.game.entity.GameWebsite;

public record GameWebsiteResponse(
		String label,
		String url,
		boolean trusted) {

	public static GameWebsiteResponse from(GameWebsite website) {
		return new GameWebsiteResponse(website.getLabel(), website.getUrl(), website.isTrusted());
	}
}
