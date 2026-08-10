package com.chmz31.checkpointd.list.dto;

import com.chmz31.checkpointd.list.model.ListVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GameListRequest(
		@NotBlank
		@Size(max = 200)
		String name,
		@Size(max = 2000)
		String description,
		ListVisibility visibility) {
}
