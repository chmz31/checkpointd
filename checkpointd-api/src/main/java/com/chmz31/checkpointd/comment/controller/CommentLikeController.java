package com.chmz31.checkpointd.comment.controller;

import com.chmz31.checkpointd.comment.service.CommentLikeService;
import com.chmz31.checkpointd.like.dto.LikeStatusResponse;
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
public class CommentLikeController {

	private final CommentLikeService commentLikeService;

	public CommentLikeController(CommentLikeService commentLikeService) {
		this.commentLikeService = commentLikeService;
	}

	@PostMapping("/list-comments/{commentId}")
	LikeStatusResponse likeListComment(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID commentId) {
		return commentLikeService.likeListComment(currentUserId(jwt), commentId);
	}

	@DeleteMapping("/list-comments/{commentId}")
	LikeStatusResponse unlikeListComment(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID commentId) {
		return commentLikeService.unlikeListComment(currentUserId(jwt), commentId);
	}

	@GetMapping("/list-comments/{commentId}/status")
	LikeStatusResponse listCommentStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID commentId) {
		return commentLikeService.getListCommentLikeStatus(currentUserId(jwt), commentId);
	}

	@PostMapping("/review-comments/{commentId}")
	LikeStatusResponse likeReviewComment(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID commentId) {
		return commentLikeService.likeReviewComment(currentUserId(jwt), commentId);
	}

	@DeleteMapping("/review-comments/{commentId}")
	LikeStatusResponse unlikeReviewComment(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID commentId) {
		return commentLikeService.unlikeReviewComment(currentUserId(jwt), commentId);
	}

	@GetMapping("/review-comments/{commentId}/status")
	LikeStatusResponse reviewCommentStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID commentId) {
		return commentLikeService.getReviewCommentLikeStatus(currentUserId(jwt), commentId);
	}

	private UUID currentUserId(Jwt jwt) {
		return jwt == null ? null : UUID.fromString(jwt.getSubject());
	}
}
