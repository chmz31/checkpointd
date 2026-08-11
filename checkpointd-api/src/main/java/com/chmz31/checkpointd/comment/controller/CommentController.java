package com.chmz31.checkpointd.comment.controller;

import com.chmz31.checkpointd.comment.dto.CommentRequest;
import com.chmz31.checkpointd.comment.dto.CommentResponse;
import com.chmz31.checkpointd.comment.dto.ReportCommentRequest;
import com.chmz31.checkpointd.comment.service.CommentService;
import com.chmz31.checkpointd.common.dto.PaginatedResponse;
import com.chmz31.checkpointd.user.model.Role;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/comments")
public class CommentController {

	private final CommentService commentService;

	public CommentController(CommentService commentService) {
		this.commentService = commentService;
	}

	@PostMapping("/lists/{listId}")
	@ResponseStatus(HttpStatus.CREATED)
	CommentResponse addListComment(
			@AuthenticationPrincipal Jwt jwt, @PathVariable UUID listId, @Valid @RequestBody CommentRequest request) {
		return commentService.addListComment(currentUserId(jwt), listId, request);
	}

	@GetMapping("/lists/{listId}")
	PaginatedResponse<CommentResponse> listComments(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID listId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		var comments = commentService.getListComments(currentUserId(jwt), listId, page, size);
		return PaginatedResponse.from(comments, comments.getContent());
	}

	@DeleteMapping("/lists/{listId}/{commentId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void deleteListComment(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID listId, @PathVariable UUID commentId) {
		commentService.deleteListComment(currentUserId(jwt), isAdmin(jwt), listId, commentId);
	}

	@PostMapping("/lists/{listId}/{commentId}/report")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void reportListComment(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID listId,
			@PathVariable UUID commentId,
			@Valid @RequestBody ReportCommentRequest request) {
		commentService.reportListComment(currentUserId(jwt), listId, commentId, request);
	}

	@PostMapping("/reviews/{reviewId}")
	@ResponseStatus(HttpStatus.CREATED)
	CommentResponse addReviewComment(
			@AuthenticationPrincipal Jwt jwt, @PathVariable UUID reviewId, @Valid @RequestBody CommentRequest request) {
		return commentService.addReviewComment(currentUserId(jwt), reviewId, request);
	}

	@GetMapping("/reviews/{reviewId}")
	PaginatedResponse<CommentResponse> reviewComments(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID reviewId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		var comments = commentService.getReviewComments(currentUserId(jwt), reviewId, page, size);
		return PaginatedResponse.from(comments, comments.getContent());
	}

	@DeleteMapping("/reviews/{reviewId}/{commentId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void deleteReviewComment(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID reviewId, @PathVariable UUID commentId) {
		commentService.deleteReviewComment(currentUserId(jwt), isAdmin(jwt), reviewId, commentId);
	}

	@PostMapping("/reviews/{reviewId}/{commentId}/report")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void reportReviewComment(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID reviewId,
			@PathVariable UUID commentId,
			@Valid @RequestBody ReportCommentRequest request) {
		commentService.reportReviewComment(currentUserId(jwt), reviewId, commentId, request);
	}

	private UUID currentUserId(Jwt jwt) {
		return jwt == null ? null : UUID.fromString(jwt.getSubject());
	}

	private boolean isAdmin(Jwt jwt) {
		if (jwt == null) {
			return false;
		}
		String role = jwt.getClaimAsString("role");
		return role != null && Role.ADMIN == Role.valueOf(role);
	}
}
