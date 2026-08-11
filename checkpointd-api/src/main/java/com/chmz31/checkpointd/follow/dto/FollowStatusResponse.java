package com.chmz31.checkpointd.follow.dto;

public record FollowStatusResponse(boolean following, long followerCount, long followingCount) {
}
