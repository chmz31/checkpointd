package com.chmz31.checkpointd.comment.dto;

import jakarta.validation.constraints.Size;

public record ReportCommentRequest(
		@Size(max = 500)
		String reason) {
}
