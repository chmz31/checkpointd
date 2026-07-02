package com.chmz31.checkpointd.library.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chmz31.checkpointd.common.exception.DuplicateResourceException;
import com.chmz31.checkpointd.common.exception.ResourceNotFoundException;
import com.chmz31.checkpointd.game.entity.Game;
import com.chmz31.checkpointd.game.repository.GameRepository;
import com.chmz31.checkpointd.library.dto.AddLibraryEntryRequest;
import com.chmz31.checkpointd.library.dto.UpdateLibraryEntryRequest;
import com.chmz31.checkpointd.library.entity.LibraryEntry;
import com.chmz31.checkpointd.library.model.LibraryStatus;
import com.chmz31.checkpointd.library.repository.LibraryEntryRepository;
import com.chmz31.checkpointd.user.entity.User;
import com.chmz31.checkpointd.user.model.Role;
import com.chmz31.checkpointd.user.repository.UserRepository;
import java.time.Instant;
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

	@InjectMocks
	private LibraryService libraryService;

	@Test
	void addCreatesEntry() {
		User user = user();
		Game game = game();
		AddLibraryEntryRequest request = new AddLibraryEntryRequest(
				GAME_ID, LibraryStatus.PLAYING, 9, "Great", Instant.parse("2026-01-01T00:00:00Z"), null);

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
		assertThat(saved.getStartedAt()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
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
	}

	@Test
	void updateCurrentUsersEntry() {
		LibraryEntry entry = entry();
		UpdateLibraryEntryRequest request = new UpdateLibraryEntryRequest(
				LibraryStatus.COMPLETED, 10, "Done", null, Instant.parse("2026-02-01T00:00:00Z"));

		when(libraryEntryRepository.findByIdAndUserId(ENTRY_ID, USER_ID)).thenReturn(Optional.of(entry));
		when(libraryEntryRepository.save(entry)).thenReturn(entry);

		LibraryEntry updated = libraryService.update(USER_ID, ENTRY_ID, request);

		assertThat(updated).isSameAs(entry);
		assertThat(entry.getStatus()).isEqualTo(LibraryStatus.COMPLETED);
		assertThat(entry.getRating()).isEqualTo(10);
		assertThat(entry.getNotes()).isEqualTo("Done");
		assertThat(entry.getCompletedAt()).isEqualTo(Instant.parse("2026-02-01T00:00:00Z"));
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
