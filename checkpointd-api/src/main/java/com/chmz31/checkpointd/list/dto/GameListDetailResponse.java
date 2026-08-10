package com.chmz31.checkpointd.list.dto;

import com.chmz31.checkpointd.list.entity.GameList;
import com.chmz31.checkpointd.list.model.ListVisibility;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GameListDetailResponse(
		UUID id,
		String username,
		String displayName,
		String name,
		String description,
		ListVisibility visibility,
		Instant createdAt,
		Instant updatedAt,
		boolean owner,
		List<GameListItemResponse> items) {

	public static GameListDetailResponse from(GameList list, List<GameListItemResponse> items, boolean owner) {
		return new GameListDetailResponse(
				list.getId(),
				list.getUser().getUsername(),
				list.getUser().getDisplayName(),
				list.getName(),
				list.getDescription(),
				list.getVisibility(),
				list.getCreatedAt(),
				list.getUpdatedAt(),
				owner,
				items);
	}
}
