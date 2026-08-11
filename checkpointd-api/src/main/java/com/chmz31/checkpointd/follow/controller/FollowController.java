package com.chmz31.checkpointd.follow.controller;

import com.chmz31.checkpointd.common.dto.PaginatedResponse;
import com.chmz31.checkpointd.follow.dto.FollowStatusResponse;
import com.chmz31.checkpointd.follow.dto.UserSummaryResponse;
import com.chmz31.checkpointd.follow.service.FollowService;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/follows")
public class FollowController {

	private final FollowService followService;

	public FollowController(FollowService followService) {
		this.followService = followService;
	}

	@PostMapping("/users/{username}")
	FollowStatusResponse follow(@AuthenticationPrincipal Jwt jwt, @PathVariable String username) {
		return followService.follow(currentUserId(jwt), username);
	}

	@DeleteMapping("/users/{username}")
	FollowStatusResponse unfollow(@AuthenticationPrincipal Jwt jwt, @PathVariable String username) {
		return followService.unfollow(currentUserId(jwt), username);
	}

	@GetMapping("/users/{username}/status")
	FollowStatusResponse status(@AuthenticationPrincipal Jwt jwt, @PathVariable String username) {
		return followService.getStatus(currentUserId(jwt), username);
	}

	@GetMapping("/users/{username}/followers")
	PaginatedResponse<UserSummaryResponse> followers(
			@PathVariable String username,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		var followers = followService.getFollowers(username, page, size);
		return PaginatedResponse.from(followers, followers.getContent());
	}

	@GetMapping("/users/{username}/following")
	PaginatedResponse<UserSummaryResponse> following(
			@PathVariable String username,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		var following = followService.getFollowing(username, page, size);
		return PaginatedResponse.from(following, following.getContent());
	}

	private UUID currentUserId(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}
}
