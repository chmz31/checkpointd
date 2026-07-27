package com.chmz31.checkpointd.library.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chmz31.checkpointd.common.exception.DuplicateResourceException;
import com.chmz31.checkpointd.common.exception.ResourceNotFoundException;
import com.chmz31.checkpointd.externalgames.service.ExternalGameImportService;
import com.chmz31.checkpointd.game.entity.Game;
import com.chmz31.checkpointd.game.repository.GameRepository;
import com.chmz31.checkpointd.game.service.MetadataRefreshService;
import com.chmz31.checkpointd.library.dto.AddLibraryEntryRequest;
import com.chmz31.checkpointd.library.dto.LibraryStatsResponse;
import com.chmz31.checkpointd.library.dto.UpdateLibraryEntryRequest;
import com.chmz31.checkpointd.library.entity.LibraryEntry;
import com.chmz31.checkpointd.library.model.LibraryStatus;
import com.chmz31.checkpointd.library.repository.LibraryEntryRepository;
import com.chmz31.checkpointd.user.entity.User;
import com.chmz31.checkpointd.user.model.Role;
import com.chmz31.checkpointd.user.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LibraryServiceTests {

	private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID GAME_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
	private static final UUID ENTRY_ID = UUID.fromString("00000000-0000-0000-0000-000000000201");

	@Mock
	private LibraryEntryRepository libraryEntryRepository;

	@Mock
	private GameRepository gameRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private ExternalGameImportService externalGameImportService;

	@Mock
	private MetadataRefreshService metadataRefreshService;

	@InjectMocks
	private LibraryService libraryService;

	@Test
	void addCreatesEntry() {
		User user = user();
		Game game = game();
		AddLibraryEntryRequest request = new AddLibraryEntryRequest(
				GAME_ID, LibraryStatus.PLAYING, 9, "Great", LocalDate.parse("2026-01-01"), LocalDate.parse("2026-02-01"));

		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
		when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
		when(libraryEntryRepository.existsByUserIdAndGameId(USER_ID, GAME_ID)).thenReturn(false);
		when(libraryEntryRepository.save(any(LibraryEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

		LibraryEntry created = libraryService.add(USER_ID, request);

		ArgumentCaptor<LibraryEntry> entryCaptor = ArgumentCaptor.forClass(LibraryEntry.class);
		verify(libraryEntryRepository).save(entryCaptor.capture());
		LibraryEntry saved = entryCaptor.getValue();

		assertThat(created).isSameAs(saved);
		assertThat(saved.getUser()).isSameAs(user);
		assertThat(saved.getGame()).isSameAs(game);
		assertThat(saved.getStatus()).isEqualTo(LibraryStatus.PLAYING);
		assertThat(saved.getRating()).isEqualTo(9);
		assertThat(saved.getNotes()).isEqualTo("Great");
		assertThat(saved.getStartedAt()).isEqualTo(LocalDate.parse("2026-01-01"));
		assertThat(saved.getCompletedAt()).isEqualTo(LocalDate.parse("2026-02-01"));
	}

	@Test
	void addRejectsDuplicateUserGameEntry() {
		AddLibraryEntryRequest request = new AddLibraryEntryRequest(GAME_ID, LibraryStatus.PLAYING, null, null, null, null);

		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
		when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game()));
		when(libraryEntryRepository.existsByUserIdAndGameId(USER_ID, GAME_ID)).thenReturn(true);

		assertThatThrownBy(() -> libraryService.add(USER_ID, request))
				.isInstanceOf(DuplicateResourceException.class)
				.hasMessage("Game is already in library");

		verify(libraryEntryRepository, never()).save(any(LibraryEntry.class));
	}

	@Test
	void addRejectsMissingGame() {
		AddLibraryEntryRequest request = new AddLibraryEntryRequest(GAME_ID, LibraryStatus.PLAYING, null, null, null, null);

		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
		when(gameRepository.findById(GAME_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> libraryService.add(USER_ID, request))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Game not found");

		verify(libraryEntryRepository, never()).save(any(LibraryEntry.class));
	}

	@Test
	void listReturnsCurrentUsersEntries() {
		List<LibraryEntry> entries = List.of(entry());

		when(libraryEntryRepository.findTop50ByUserIdOrderByUpdatedAtDesc(USER_ID)).thenReturn(entries);

		assertThat(libraryService.list(USER_ID, null)).isSameAs(entries);
	}

	@Test
	void listFiltersCurrentUsersEntriesByStatus() {
		List<LibraryEntry> entries = List.of(entry());

		when(libraryEntryRepository.findTop50ByUserIdAndStatusOrderByUpdatedAtDesc(USER_ID, LibraryStatus.PLAYING))
				.thenReturn(entries);

		assertThat(libraryService.list(USER_ID, LibraryStatus.PLAYING)).isSameAs(entries);
	}

	@Test
	void getReturnsCurrentUsersEntry() {
		LibraryEntry entry = entry();

		when(libraryEntryRepository.findByIdAndUserId(ENTRY_ID, USER_ID)).thenReturn(Optional.of(entry));

		assertThat(libraryService.get(USER_ID, ENTRY_ID)).isSameAs(entry);
		verify(metadataRefreshService).triggerRefreshIfStale(entry.getGame());
	}

	@Test
	void getByGameReturnsCurrentUsersEntry() {
		LibraryEntry entry = entry();

		when(libraryEntryRepository.findByUserIdAndGameId(USER_ID, GAME_ID)).thenReturn(Optional.of(entry));

		assertThat(libraryService.getByGame(USER_ID, GAME_ID)).isSameAs(entry);
	}

	@Test
	void getByGameRejectsMissingOrOtherUsersEntry() {
		when(libraryEntryRepository.findByUserIdAndGameId(USER_ID, GAME_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> libraryService.getByGame(USER_ID, GAME_ID))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Library entry not found");
	}

	@Test
	void getByExternalGameReturnsCurrentUsersEntry() {
		Game game = game();
		LibraryEntry entry = entry();

		when(gameRepository.findByExternalProviderAndExternalId("igdb", "123")).thenReturn(Optional.of(game));
		when(libraryEntryRepository.findByUserIdAndGameId(USER_ID, GAME_ID)).thenReturn(Optional.of(entry));

		assertThat(libraryService.getByExternalGame(USER_ID, " IGDB ", " 123 ")).isSameAs(entry);
	}

	@Test
	void getByExternalGameRejectsUncachedGame() {
		when(gameRepository.findByExternalProviderAndExternalId("igdb", "123")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> libraryService.getByExternalGame(USER_ID, "igdb", "123"))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Library entry not found");

		verify(libraryEntryRepository, never()).findByUserIdAndGameId(any(UUID.class), any(UUID.class));
	}

	@Test
	void getByExternalGameRejectsGameNotInCurrentUsersLibrary() {
		Game game = game();

		when(gameRepository.findByExternalProviderAndExternalId("igdb", "123")).thenReturn(Optional.of(game));
		when(libraryEntryRepository.findByUserIdAndGameId(USER_ID, GAME_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> libraryService.getByExternalGame(USER_ID, "igdb", "123"))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Library entry not found");
	}

	@Test
	void statsReturnsCurrentUsersLibraryCounts() {
		when(libraryEntryRepository.countByUserId(USER_ID)).thenReturn(6L);
		when(libraryEntryRepository.countByUserIdAndStatus(USER_ID, LibraryStatus.WISHLIST)).thenReturn(1L);
		when(libraryEntryRepository.countByUserIdAndStatus(USER_ID, LibraryStatus.BACKLOG)).thenReturn(1L);
		when(libraryEntryRepository.countByUserIdAndStatus(USER_ID, LibraryStatus.PLAYING)).thenReturn(1L);
		when(libraryEntryRepository.countByUserIdAndStatus(USER_ID, LibraryStatus.COMPLETED)).thenReturn(1L);
		when(libraryEntryRepository.countByUserIdAndStatus(USER_ID, LibraryStatus.DROPPED)).thenReturn(1L);
		when(libraryEntryRepository.countByUserIdAndStatus(USER_ID, LibraryStatus.PAUSED)).thenReturn(1L);
		when(libraryEntryRepository.countByUserIdAndRatingIsNotNull(USER_ID)).thenReturn(3L);
		when(libraryEntryRepository.averageRatingByUserId(USER_ID)).thenReturn(8.5);

		LibraryStatsResponse stats = libraryService.stats(USER_ID);

		assertThat(stats.totalEntries()).isEqualTo(6);
		assertThat(stats.wishlistCount()).isEqualTo(1);
		assertThat(stats.backlogCount()).isEqualTo(1);
		assertThat(stats.playingCount()).isEqualTo(1);
		assertThat(stats.completedCount()).isEqualTo(1);
		assertThat(stats.droppedCount()).isEqualTo(1);
		assertThat(stats.pausedCount()).isEqualTo(1);
		assertThat(stats.ratedCount()).isEqualTo(3);
		assertThat(stats.averageRating()).isEqualTo(8.5);
	}

	@Test
	void updateCurrentUsersEntry() {
		LibraryEntry entry = entry();
		UpdateLibraryEntryRequest request = new UpdateLibraryEntryRequest(
				LibraryStatus.COMPLETED, 10, "Done", LocalDate.parse("2026-01-01"), LocalDate.parse("2026-02-01"));

		when(libraryEntryRepository.findByIdAndUserId(ENTRY_ID, USER_ID)).thenReturn(Optional.of(entry));
		when(libraryEntryRepository.save(entry)).thenReturn(entry);

		LibraryEntry updated = libraryService.update(USER_ID, ENTRY_ID, request);

		assertThat(updated).isSameAs(entry);
		assertThat(entry.getStatus()).isEqualTo(LibraryStatus.COMPLETED);
		assertThat(entry.getRating()).isEqualTo(10);
		assertThat(entry.getNotes()).isEqualTo("Done");
		assertThat(entry.getStartedAt()).isEqualTo(LocalDate.parse("2026-01-01"));
		assertThat(entry.getCompletedAt()).isEqualTo(LocalDate.parse("2026-02-01"));
	}

	@Test
	void updateClearsNullableTrackingFields() {
		LibraryEntry entry = entry();
		entry.setRating(8);
		entry.setNotes("Old notes");
		entry.setStartedAt(LocalDate.parse("2026-01-01"));
		entry.setCompletedAt(LocalDate.parse("2026-02-01"));
		UpdateLibraryEntryRequest request = new UpdateLibraryEntryRequest(LibraryStatus.PLAYING, null, null, null, null);

		when(libraryEntryRepository.findByIdAndUserId(ENTRY_ID, USER_ID)).thenReturn(Optional.of(entry));
		when(libraryEntryRepository.save(entry)).thenReturn(entry);

		LibraryEntry updated = libraryService.update(USER_ID, ENTRY_ID, request);

		assertThat(updated).isSameAs(entry);
		assertThat(entry.getStatus()).isEqualTo(LibraryStatus.PLAYING);
		assertThat(entry.getRating()).isNull();
		assertThat(entry.getNotes()).isNull();
		assertThat(entry.getStartedAt()).isNull();
		assertThat(entry.getCompletedAt()).isNull();
	}

	@Test
	void syncMetadataRefreshesCurrentUsersEntryGame() {
		LibraryEntry entry = entry();
		Game game = entry.getGame();

		when(libraryEntryRepository.findByIdAndUserId(ENTRY_ID, USER_ID)).thenReturn(Optional.of(entry));
		when(externalGameImportService.syncMetadata(game)).thenAnswer(invocation -> {
			game.setSummary("Updated summary");
			game.setGenres(List.of("RPG"));
			game.setPlatforms(List.of("PC"));
			return game;
		});

		LibraryEntry synced = libraryService.syncMetadata(USER_ID, ENTRY_ID);

		assertThat(synced).isSameAs(entry);
		assertThat(synced.getGame().getSummary()).isEqualTo("Updated summary");
		verify(externalGameImportService).syncMetadata(game);
	}

	@Test
	void syncMetadataRejectsMissingOrOtherUsersEntry() {
		when(libraryEntryRepository.findByIdAndUserId(ENTRY_ID, USER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> libraryService.syncMetadata(USER_ID, ENTRY_ID))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Library entry not found");

		verify(externalGameImportService, never()).syncMetadata(any(Game.class));
	}

	@Test
	void deleteCurrentUsersEntry() {
		LibraryEntry entry = entry();

		when(libraryEntryRepository.findByIdAndUserId(ENTRY_ID, USER_ID)).thenReturn(Optional.of(entry));

		libraryService.delete(USER_ID, ENTRY_ID);

		verify(libraryEntryRepository).delete(entry);
	}

	@Test
	void getRejectsMissingOrOtherUsersEntry() {
		when(libraryEntryRepository.findByIdAndUserId(ENTRY_ID, USER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> libraryService.get(USER_ID, ENTRY_ID))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Library entry not found");
	}

	private User user() {
		User user = new User("player@example.com", "playerone", "hash", Role.USER);
		ReflectionTestUtils.setField(user, "id", USER_ID);

		return user;
	}

	private Game game() {
		Game game = new Game("Chrono Trigger");
		game.setExternalProvider("igdb");
		game.setExternalId("123");
		game.setSlug("chrono-trigger");
		game.setCoverUrl("https://img.example/cover.jpg");
		ReflectionTestUtils.setField(game, "id", GAME_ID);

		return game;
	}

	private LibraryEntry entry() {
		LibraryEntry entry = new LibraryEntry(user(), game(), LibraryStatus.PLAYING);
		ReflectionTestUtils.setField(entry, "id", ENTRY_ID);
		ReflectionTestUtils.setField(entry, "createdAt", Instant.parse("2026-01-01T00:00:00Z"));
		ReflectionTestUtils.setField(entry, "updatedAt", Instant.parse("2026-01-02T00:00:00Z"));

		return entry;
	}
}
