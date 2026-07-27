package com.chmz31.checkpointd.profile.dto;

import com.chmz31.checkpointd.user.model.ProfileVisibility;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
		@Size(max = 80)
		String displayName,
		@Size(max = 1000)
		String bio,
		ProfileVisibility profileVisibility) {
}
