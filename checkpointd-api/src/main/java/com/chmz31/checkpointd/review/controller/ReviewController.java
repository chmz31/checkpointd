package com.chmz31.checkpointd.review.controller;

import com.chmz31.checkpointd.common.dto.PaginatedResponse;
import com.chmz31.checkpointd.review.dto.ReviewRequest;
import com.chmz31.checkpointd.review.dto.ReviewResponse;
import com.chmz31.checkpointd.review.service.ReviewService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController {

	private final ReviewService reviewService;

	public ReviewController(ReviewService reviewService) {
		this.reviewService = reviewService;
	}

	@GetMapping("/games/{gameId}")
	PaginatedResponse<ReviewResponse> gameReviews(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID gameId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {
		var reviews = reviewService.getPublicGameReviews(gameId, currentUserId(jwt), page, size);
		return PaginatedResponse.from(reviews, reviews.getContent());
	}

	@GetMapping("/users/{username}")
	PaginatedResponse<ReviewResponse> userReviews(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable String username,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {
		var reviews = reviewService.getPublicUserReviews(username, currentUserId(jwt), page, size);
		return PaginatedResponse.from(reviews, reviews.getContent());
	}

	@GetMapping("/users/{username}/games/{gameId}")
	ReviewResponse userGameReview(
			@AuthenticationPrincipal Jwt jwt, @PathVariable String username, @PathVariable UUID gameId) {
		return reviewService.getPublicUserGameReview(username, gameId, currentUserId(jwt));
	}

	@GetMapping("/me")
	PaginatedResponse<ReviewResponse> myReviews(
			@AuthenticationPrincipal Jwt jwt,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {
		var reviews = reviewService.getMyReviews(currentUserId(jwt), page, size);
		return PaginatedResponse.from(reviews, reviews.getContent());
	}

	@GetMapping("/me/games/{gameId}")
	ReviewResponse myGameReview(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID gameId) {
		return reviewService.getMyGameReview(currentUserId(jwt), gameId);
	}

	@PutMapping("/me/games/{gameId}")
	ReviewResponse saveMyGameReview(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID gameId,
			@Valid @RequestBody ReviewRequest request) {
		return reviewService.saveMyGameReview(currentUserId(jwt), gameId, request);
	}

	@DeleteMapping("/me/games/{gameId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void deleteMyGameReview(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID gameId) {
		reviewService.deleteMyGameReview(currentUserId(jwt), gameId);
	}

	private UUID currentUserId(Jwt jwt) {
		return jwt == null ? null : UUID.fromString(jwt.getSubject());
	}
}
