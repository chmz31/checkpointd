package com.chmz31.checkpointd.library.service;

import com.chmz31.checkpointd.common.exception.DuplicateResourceException;
import com.chmz31.checkpointd.common.exception.ResourceNotFoundException;
import com.chmz31.checkpointd.externalgames.service.ExternalGameImportService;
import com.chmz31.checkpointd.game.entity.Game;
import com.chmz31.checkpointd.game.repository.GameRepository;
import com.chmz31.checkpointd.game.service.MetadataRefreshService;
import com.chmz31.checkpointd.library.dto.AddLibraryEntryRequest;
import com.chmz31.checkpointd.library.dto.LibraryEntryResponse;
import com.chmz31.checkpointd.library.dto.LibraryStatsResponse;
import com.chmz31.checkpointd.library.dto.UpdateLibraryEntryRequest;
import com.chmz31.checkpointd.library.entity.LibraryEntry;
import com.chmz31.checkpointd.library.model.LibrarySortOption;
import com.chmz31.checkpointd.library.model.LibraryStatus;
import com.chmz31.checkpointd.library.repository.LibraryEntryRepository;
import com.chmz31.checkpointd.user.entity.User;
import com.chmz31.checkpointd.user.repository.UserRepository;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LibraryService {

	private static final int DEFAULT_PAGE_SIZE = 24;
	private static final int MAX_PAGE_SIZE = 100;

	private final LibraryEntryRepository libraryEntryRepository;
	private final GameRepository gameRepository;
	private final UserRepository userRepository;
	private final ExternalGameImportService externalGameImportService;
	private final MetadataRefreshService metadataRefreshService;

	public LibraryService(
			LibraryEntryRepository libraryEntryRepository,
			GameRepository gameRepository,
			UserRepository userRepository,
			ExternalGameImportService externalGameImportService,
			MetadataRefreshService metadataRefreshService) {
		this.libraryEntryRepository = libraryEntryRepository;
		this.gameRepository = gameRepository;
		this.userRepository = userRepository;
		this.externalGameImportService = externalGameImportService;
		this.metadataRefreshService = metadataRefreshService;
	}

	@Transactional
	public LibraryEntry add(UUID userId, AddLibraryEntryRequest request) {
		User user = getUser(userId);
		Game game = gameRepository.findById(request.gameId())
				.orElseThrow(() -> new ResourceNotFoundException("Game not found"));

		if (libraryEntryRepository.existsByUserIdAndGameId(userId, request.gameId())) {
			throw new DuplicateResourceException("Game is already in library");
		}

		LibraryEntry entry = new LibraryEntry(user, game, request.status());
		entry.setRating(request.rating());
		entry.setNotes(request.notes());
		entry.setStartedAt(request.startedAt());
		entry.setCompletedAt(request.completedAt());

		return libraryEntryRepository.save(entry);
	}

	@Transactional(readOnly = true)
	public Page<LibraryEntryResponse> list(UUID userId, LibraryStatus status, String q, int page, int size, LibrarySortOption sort) {
		int safePage = Math.max(page, 0);
		int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
		LibrarySortOption safeSort = sort == null ? LibrarySortOption.UPDATED_DESC : sort;
		String cleanQuery = q == null || q.isBlank() ? null : q.trim().toLowerCase(Locale.ROOT);
		PageRequest pageRequest = PageRequest.of(safePage, safeSize, safeSort.toSort());

		if (cleanQuery == null) {
			return libraryEntryRepository.findUserLibrary(userId, status, pageRequest)
					.map(LibraryEntryResponse::from);
		}

		return libraryEntryRepository.searchUserLibrary(
				userId,
				status,
				"%" + cleanQuery + "%",
				pageRequest)
				.map(LibraryEntryResponse::from);
	}

	@Transactional(readOnly = true)
	public LibraryEntry get(UUID userId, UUID entryId) {
		LibraryEntry entry = getUserEntry(userId, entryId);
		metadataRefreshService.triggerRefreshIfStale(entry.getGame());

		return entry;
	}

	@Transactional(readOnly = true)
	public LibraryEntry getByGame(UUID userId, UUID gameId) {
		return libraryEntryRepository.findByUserIdAndGameId(userId, gameId)
				.orElseThrow(() -> new ResourceNotFoundException("Library entry not found"));
	}

	@Transactional(readOnly = true)
	public LibraryEntry getByExternalGame(UUID userId, String provider, String externalId) {
		String cleanProvider = provider == null ? null : provider.trim().toLowerCase(Locale.ROOT);
		String cleanExternalId = externalId == null ? null : externalId.trim();
		if (cleanProvider == null || cleanProvider.isBlank() || cleanExternalId == null || cleanExternalId.isBlank()) {
			throw new ResourceNotFoundException("Library entry not found");
		}

		Game game = gameRepository.findByExternalProviderAndExternalId(cleanProvider, cleanExternalId)
				.orElseThrow(() -> new ResourceNotFoundException("Library entry not found"));

		return getByGame(userId, game.getId());
	}

	@Transactional(readOnly = true)
	public LibraryStatsResponse stats(UUID userId) {
		return new LibraryStatsResponse(
				libraryEntryRepository.countByUserId(userId),
				libraryEntryRepository.countByUserIdAndStatus(userId, LibraryStatus.WISHLIST),
				libraryEntryRepository.countByUserIdAndStatus(userId, LibraryStatus.BACKLOG),
				libraryEntryRepository.countByUserIdAndStatus(userId, LibraryStatus.PLAYING),
				libraryEntryRepository.countByUserIdAndStatus(userId, LibraryStatus.COMPLETED),
				libraryEntryRepository.countByUserIdAndStatus(userId, LibraryStatus.DROPPED),
				libraryEntryRepository.countByUserIdAndStatus(userId, LibraryStatus.PAUSED),
				libraryEntryRepository.countByUserIdAndRatingIsNotNull(userId),
				libraryEntryRepository.averageRatingByUserId(userId));
	}

	@Transactional
	public LibraryEntry update(UUID userId, UUID entryId, UpdateLibraryEntryRequest request) {
		LibraryEntry entry = getUserEntry(userId, entryId);

		if (request.status() != null) {
			entry.setStatus(request.status());
		}
		entry.setRating(request.rating());
		entry.setNotes(request.notes());
		entry.setStartedAt(request.startedAt());
		entry.setCompletedAt(request.completedAt());

		return libraryEntryRepository.save(entry);
	}

	@Transactional
	public LibraryEntry syncMetadata(UUID userId, UUID entryId) {
		LibraryEntry entry = getUserEntry(userId, entryId);
		externalGameImportService.syncMetadata(entry.getGame());

		return entry;
	}

	public boolean isGameMetadataStale(LibraryEntry entry) {
		return metadataRefreshService.isMetadataStale(entry.getGame());
	}

	@Transactional
	public void delete(UUID userId, UUID entryId) {
		LibraryEntry entry = getUserEntry(userId, entryId);

		libraryEntryRepository.delete(entry);
	}

	private User getUser(UUID userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));
	}

	private LibraryEntry getUserEntry(UUID userId, UUID entryId) {
		return libraryEntryRepository.findByIdAndUserId(entryId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Library entry not found"));
	}
}
