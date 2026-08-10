package com.chmz31.checkpointd.review.dto;

import com.chmz31.checkpointd.review.model.ReviewVisibility;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReviewRequest(
		@Min(1)
		@Max(10)
		Integer rating,
		@NotBlank
		@Size(max = 5000)
		String body,
		Boolean containsSpoilers,
		ReviewVisibility visibility) {
}
