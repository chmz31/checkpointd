package com.chmz31.checkpointd.list.dto;

import com.chmz31.checkpointd.list.entity.GameList;
import com.chmz31.checkpointd.list.model.ListVisibility;
import java.time.Instant;
import java.util.UUID;

public record GameListResponse(
		UUID id,
		String username,
		String displayName,
		String name,
		String description,
		ListVisibility visibility,
		long itemCount,
		Instant createdAt,
		Instant updatedAt,
		boolean owner) {

	public static GameListResponse from(GameList list, long itemCount, boolean owner) {
		return new GameListResponse(
				list.getId(),
				list.getUser().getUsername(),
				list.getUser().getDisplayName(),
				list.getName(),
				list.getDescription(),
				list.getVisibility(),
				itemCount,
				list.getCreatedAt(),
				list.getUpdatedAt(),
				owner);
	}
}
