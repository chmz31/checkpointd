package com.chmz31.checkpointd.like.controller;

import com.chmz31.checkpointd.like.dto.LikeStatusResponse;
import com.chmz31.checkpointd.like.service.LikeService;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/likes")
public class LikeController {

	private final LikeService likeService;

	public LikeController(LikeService likeService) {
		this.likeService = likeService;
	}

	@PostMapping("/lists/{listId}")
	LikeStatusResponse likeList(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID listId) {
		return likeService.likeList(currentUserId(jwt), listId);
	}

	@DeleteMapping("/lists/{listId}")
	LikeStatusResponse unlikeList(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID listId) {
		return likeService.unlikeList(currentUserId(jwt), listId);
	}

	@GetMapping("/lists/{listId}/status")
	LikeStatusResponse listStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID listId) {
		return likeService.getListLikeStatus(currentUserId(jwt), listId);
	}

	@PostMapping("/reviews/{reviewId}")
	LikeStatusResponse likeReview(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID reviewId) {
		return likeService.likeReview(currentUserId(jwt), reviewId);
	}

	@DeleteMapping("/reviews/{reviewId}")
	LikeStatusResponse unlikeReview(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID reviewId) {
		return likeService.unlikeReview(currentUserId(jwt), reviewId);
	}

	@GetMapping("/reviews/{reviewId}/status")
	LikeStatusResponse reviewStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID reviewId) {
		return likeService.getReviewLikeStatus(currentUserId(jwt), reviewId);
	}

	private UUID currentUserId(Jwt jwt) {
		return jwt == null ? null : UUID.fromString(jwt.getSubject());
	}
}
