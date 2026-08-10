package com.chmz31.checkpointd.list.dto;

import com.chmz31.checkpointd.list.entity.GameListItem;
import java.time.Instant;
import java.util.UUID;

public record GameListItemResponse(
		UUID id,
		UUID gameId,
		String gameSlug,
		String gameTitle,
		String gameCoverUrl,
		Instant addedAt) {

	public static GameListItemResponse from(GameListItem item) {
		return new GameListItemResponse(
				item.getId(),
				item.getGame().getId(),
				item.getGame().getSlug(),
				item.getGame().getTitle(),
				item.getGame().getCoverUrl(),
				item.getCreatedAt());
	}
}
