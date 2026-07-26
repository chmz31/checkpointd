package com.chmz31.checkpointd.library.controller;

import com.chmz31.checkpointd.library.dto.AddLibraryEntryRequest;
import com.chmz31.checkpointd.library.dto.LibraryEntryResponse;
import com.chmz31.checkpointd.library.dto.LibraryStatsResponse;
import com.chmz31.checkpointd.library.dto.UpdateLibraryEntryRequest;
import com.chmz31.checkpointd.library.model.LibraryStatus;
import com.chmz31.checkpointd.library.service.LibraryService;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/v1/library")
public class LibraryController {

	private final LibraryService libraryService;

	public LibraryController(LibraryService libraryService) {
		this.libraryService = libraryService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	LibraryEntryResponse add(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AddLibraryEntryRequest request) {
		return LibraryEntryResponse.from(libraryService.add(currentUserId(jwt), request));
	}

	@GetMapping
	List<LibraryEntryResponse> list(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) LibraryStatus status) {
		return libraryService.list(currentUserId(jwt), status).stream()
				.map(LibraryEntryResponse::from)
				.toList();
	}

	@GetMapping("/stats")
	LibraryStatsResponse stats(@AuthenticationPrincipal Jwt jwt) {
		return libraryService.stats(currentUserId(jwt));
	}

	@GetMapping("/by-game/{gameId}")
	LibraryEntryResponse getByGame(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID gameId) {
		return LibraryEntryResponse.from(libraryService.getByGame(currentUserId(jwt), gameId));
	}

	@GetMapping("/by-external-game")
	LibraryEntryResponse getByExternalGame(
			@AuthenticationPrincipal Jwt jwt,
			@RequestParam String provider,
			@RequestParam String externalId) {
		return LibraryEntryResponse.from(libraryService.getByExternalGame(currentUserId(jwt), provider, externalId));
	}

	@GetMapping("/{entryId}")
	LibraryEntryResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID entryId) {
		return LibraryEntryResponse.from(libraryService.get(currentUserId(jwt), entryId));
	}

	@PatchMapping("/{entryId}")
	LibraryEntryResponse update(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID entryId,
			@Valid @RequestBody UpdateLibraryEntryRequest request) {
		return LibraryEntryResponse.from(libraryService.update(currentUserId(jwt), entryId, request));
	}

	@PostMapping("/{entryId}/sync-metadata")
	LibraryEntryResponse syncMetadata(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID entryId) {
		return LibraryEntryResponse.from(libraryService.syncMetadata(currentUserId(jwt), entryId));
	}

	@DeleteMapping("/{entryId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID entryId) {
		libraryService.delete(currentUserId(jwt), entryId);
	}

	private UUID currentUserId(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}
}
