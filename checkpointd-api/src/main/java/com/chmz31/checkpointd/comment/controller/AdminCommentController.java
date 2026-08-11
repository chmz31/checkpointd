package com.chmz31.checkpointd.comment.controller;

import com.chmz31.checkpointd.comment.dto.ReportedListCommentResponse;
import com.chmz31.checkpointd.comment.dto.ReportedReviewCommentResponse;
import com.chmz31.checkpointd.comment.service.CommentService;
import com.chmz31.checkpointd.common.dto.PaginatedResponse;
import com.chmz31.checkpointd.user.model.Role;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/comments")
public class AdminCommentController {

	private final CommentService commentService;

	public AdminCommentController(CommentService commentService) {
		this.commentService = commentService;
	}

	@GetMapping("/lists/reported")
	PaginatedResponse<ReportedListCommentResponse> reportedListComments(
			@AuthenticationPrincipal Jwt jwt,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		var comments = commentService.getReportedListComments(isAdmin(jwt), page, size);
		return PaginatedResponse.from(comments, comments.getContent());
	}

	@GetMapping("/reviews/reported")
	PaginatedResponse<ReportedReviewCommentResponse> reportedReviewComments(
			@AuthenticationPrincipal Jwt jwt,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		var comments = commentService.getReportedReviewComments(isAdmin(jwt), page, size);
		return PaginatedResponse.from(comments, comments.getContent());
	}

	private boolean isAdmin(Jwt jwt) {
		if (jwt == null) {
			return false;
		}
		String role = jwt.getClaimAsString("role");
		return role != null && Role.ADMIN == Role.valueOf(role);
	}
}
