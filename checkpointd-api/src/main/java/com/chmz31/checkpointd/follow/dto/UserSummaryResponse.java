package com.chmz31.checkpointd.follow.dto;

import com.chmz31.checkpointd.user.entity.User;

public record UserSummaryResponse(String username, String displayName) {

	public static UserSummaryResponse from(User user) {
		return new UserSummaryResponse(user.getUsername(), user.getDisplayName());
	}
}
