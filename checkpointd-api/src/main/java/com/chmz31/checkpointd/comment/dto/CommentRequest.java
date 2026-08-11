package com.chmz31.checkpointd.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CommentRequest(
		@NotBlank
		@Size(max = 2000)
		String body,
		UUID parentId) {
}
