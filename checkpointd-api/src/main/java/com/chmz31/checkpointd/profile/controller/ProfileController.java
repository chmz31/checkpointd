package com.chmz31.checkpointd.profile.controller;

import com.chmz31.checkpointd.common.dto.PaginatedResponse;
import com.chmz31.checkpointd.follow.dto.UserSummaryResponse;
import com.chmz31.checkpointd.profile.dto.PublicProfileResponse;
import com.chmz31.checkpointd.profile.dto.UpdateProfileRequest;
import com.chmz31.checkpointd.profile.service.ProfileService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profiles")
public class ProfileController {

	private final ProfileService profileService;

	public ProfileController(ProfileService profileService) {
		this.profileService = profileService;
	}

	@GetMapping("/me")
	PublicProfileResponse me(@AuthenticationPrincipal Jwt jwt) {
		return profileService.getMyProfile(currentUserId(jwt));
	}

	@PatchMapping("/me")
	PublicProfileResponse updateMe(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateProfileRequest request) {
		return profileService.updateMyProfile(currentUserId(jwt), request);
	}

	@GetMapping("/search")
	PaginatedResponse<UserSummaryResponse> search(
			@RequestParam(defaultValue = "") String q,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		var users = profileService.searchUsers(q, page, size);
		return PaginatedResponse.from(users, users.getContent());
	}

	@GetMapping("/{username}")
	PublicProfileResponse getPublicProfile(@AuthenticationPrincipal Jwt jwt, @PathVariable String username) {
		return profileService.getPublicProfile(username, currentUserId(jwt));
	}

	private UUID currentUserId(Jwt jwt) {
		return jwt == null ? null : UUID.fromString(jwt.getSubject());
	}
}
