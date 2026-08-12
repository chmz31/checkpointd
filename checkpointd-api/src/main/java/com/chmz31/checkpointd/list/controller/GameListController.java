package com.chmz31.checkpointd.list.controller;

import com.chmz31.checkpointd.common.dto.PaginatedResponse;
import com.chmz31.checkpointd.list.dto.AddGameListItemRequest;
import com.chmz31.checkpointd.list.dto.GameListDetailResponse;
import com.chmz31.checkpointd.list.dto.GameListRequest;
import com.chmz31.checkpointd.list.dto.GameListResponse;
import com.chmz31.checkpointd.list.service.GameListService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lists")
public class GameListController {

	private final GameListService gameListService;

	public GameListController(GameListService gameListService) {
		this.gameListService = gameListService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	GameListResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody GameListRequest request) {
		return gameListService.createList(currentUserId(jwt), request);
	}

	@GetMapping("/me")
	PaginatedResponse<GameListResponse> myLists(
			@AuthenticationPrincipal Jwt jwt,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		var lists = gameListService.getMyLists(currentUserId(jwt), page, size);
		return PaginatedResponse.from(lists, lists.getContent());
	}

	@GetMapping("/me/{listId}")
	GameListDetailResponse myList(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID listId) {
		return gameListService.getMyList(currentUserId(jwt), listId);
	}

	@PatchMapping("/me/{listId}")
	GameListResponse update(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID listId,
			@Valid @RequestBody GameListRequest request) {
		return gameListService.updateList(currentUserId(jwt), listId, request);
	}

	@DeleteMapping("/me/{listId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID listId) {
		gameListService.deleteList(currentUserId(jwt), listId);
	}

	@PostMapping("/me/{listId}/items")
	GameListDetailResponse addItem(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID listId,
			@Valid @RequestBody AddGameListItemRequest request) {
		return gameListService.addItem(currentUserId(jwt), listId, request);
	}

	@DeleteMapping("/me/{listId}/items/{gameId}")
	GameListDetailResponse removeItem(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID listId,
			@PathVariable UUID gameId) {
		return gameListService.removeItem(currentUserId(jwt), listId, gameId);
	}

	@GetMapping("/search")
	PaginatedResponse<GameListResponse> search(
			@AuthenticationPrincipal Jwt jwt,
			@RequestParam(defaultValue = "") String q,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		var lists = gameListService.searchLists(q, currentUserId(jwt), page, size);
		return PaginatedResponse.from(lists, lists.getContent());
	}

	@GetMapping("/popular")
	PaginatedResponse<GameListResponse> popularLists(
			@AuthenticationPrincipal Jwt jwt,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		var lists = gameListService.getPopularLists(currentUserId(jwt), page, size);
		return PaginatedResponse.from(lists, lists.getContent());
	}

	@GetMapping("/users/{username}")
	PaginatedResponse<GameListResponse> userLists(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable String username,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		var lists = gameListService.getPublicLists(username, currentUserId(jwt), page, size);
		return PaginatedResponse.from(lists, lists.getContent());
	}

	@GetMapping("/users/{username}/{listId}")
	GameListDetailResponse userList(
			@AuthenticationPrincipal Jwt jwt, @PathVariable String username, @PathVariable UUID listId) {
		return gameListService.getPublicList(username, listId, currentUserId(jwt));
	}

	private UUID currentUserId(Jwt jwt) {
		return jwt == null ? null : UUID.fromString(jwt.getSubject());
	}
}
