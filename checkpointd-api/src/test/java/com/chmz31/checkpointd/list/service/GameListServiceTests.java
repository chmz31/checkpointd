package com.chmz31.checkpointd.list.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chmz31.checkpointd.common.exception.DuplicateResourceException;
import com.chmz31.checkpointd.common.exception.ResourceNotFoundException;
import com.chmz31.checkpointd.game.entity.Game;
import com.chmz31.checkpointd.game.repository.GameRepository;
import com.chmz31.checkpointd.like.repository.ListLikeRepository;
import com.chmz31.checkpointd.list.dto.AddGameListItemRequest;
import com.chmz31.checkpointd.list.dto.GameListDetailResponse;
import com.chmz31.checkpointd.list.dto.GameListRequest;
import com.chmz31.checkpointd.list.dto.GameListResponse;
import com.chmz31.checkpointd.list.entity.GameList;
import com.chmz31.checkpointd.list.entity.GameListItem;
import com.chmz31.checkpointd.list.model.ListVisibility;
import com.chmz31.checkpointd.list.repository.GameListItemRepository;
import com.chmz31.checkpointd.list.repository.GameListRepository;
import com.chmz31.checkpointd.user.entity.User;
import com.chmz31.checkpointd.user.model.ProfileVisibility;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class GameListServiceTests {

	private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID GAME_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
	private static final UUID LIST_ID = UUID.fromString("00000000-0000-0000-0000-000000000401");

	@Mock
	private GameListRepository gameListRepository;

	@Mock
	private GameListItemRepository gameListItemRepository;

	@Mock
	private ListLikeRepository listLikeRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private GameRepository gameRepository;

	@InjectMocks
	private GameListService gameListService;

	@Test
	void createListCreatesListForUser() {
		User user = user(ProfileVisibility.PUBLIC);
		GameListRequest request = new GameListRequest("Cozy games", "  Comfort picks.  ", ListVisibility.PRIVATE);

		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
		when(gameListRepository.save(any(GameList.class))).thenAnswer(invocation -> withMetadata(invocation.getArgument(0)));

		GameListResponse response = gameListService.createList(USER_ID, request);

		ArgumentCaptor<GameList> captor = ArgumentCaptor.forClass(GameList.class);
		verify(gameListRepository).save(captor.capture());
		GameList saved = captor.getValue();

		assertThat(saved.getUser()).isSameAs(user);
		assertThat(saved.getName()).isEqualTo("Cozy games");
		assertThat(saved.getDescription()).isEqualTo("Comfort picks.");
		assertThat(saved.getVisibility()).isEqualTo(ListVisibility.PRIVATE);
		assertThat(response.owner()).isTrue();
		assertThat(response.itemCount()).isZero();
	}

	@Test
	void createListDefaultsVisibilityToPublicWhenNull() {
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(ProfileVisibility.PUBLIC)));
		when(gameListRepository.save(any(GameList.class))).thenAnswer(invocation -> withMetadata(invocation.getArgument(0)));

		GameListResponse response = gameListService.createList(
				USER_ID, new GameListRequest("Backlog picks", null, null));

		assertThat(response.visibility()).isEqualTo(ListVisibility.PUBLIC);
	}

	@Test
	void createListRejectsMissingUser() {
		when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> gameListService.createList(USER_ID, new GameListRequest("Name", null, null)))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("User not found");

		verify(gameListRepository, never()).save(any(GameList.class));
	}

	@Test
	void getMyListsReturnsOwnersLists() {
		when(gameListRepository.findByUserIdOrderByUpdatedAtDesc(eq(USER_ID), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(list(ListVisibility.PRIVATE))));
		when(gameListItemRepository.countByListId(LIST_ID)).thenReturn(3L);
		when(listLikeRepository.countByListId(LIST_ID)).thenReturn(5L);
		when(listLikeRepository.existsByUserIdAndListId(USER_ID, LIST_ID)).thenReturn(true);

		Page<GameListResponse> lists = gameListService.getMyLists(USER_ID, 0, 20);

		assertThat(lists.getContent()).extracting(GameListResponse::owner).containsExactly(true);
		assertThat(lists.getContent()).extracting(GameListResponse::itemCount).containsExactly(3L);
		assertThat(lists.getContent()).extracting(GameListResponse::likeCount).containsExactly(5L);
		assertThat(lists.getContent()).extracting(GameListResponse::liked).containsExactly(true);
	}

	@Test
	void getPopularListsReturnsListsInLikeRankedOrder() {
		UUID secondListId = UUID.fromString("00000000-0000-0000-0000-000000000402");
		GameList first = list(ListVisibility.PUBLIC);
		GameList second = list(ListVisibility.PUBLIC);
		ReflectionTestUtils.setField(second, "id", secondListId);

		when(listLikeRepository.findPopularListIds(eq(ListVisibility.PUBLIC), eq(ProfileVisibility.PUBLIC), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(LIST_ID, secondListId)));
		when(gameListRepository.findAllById(List.of(LIST_ID, secondListId))).thenReturn(List.of(second, first));
		when(listLikeRepository.countByListId(LIST_ID)).thenReturn(5L);
		when(listLikeRepository.countByListId(secondListId)).thenReturn(2L);

		Page<GameListResponse> popular = gameListService.getPopularLists(null, 0, 20);

		assertThat(popular.getContent()).extracting(GameListResponse::id).containsExactly(LIST_ID, secondListId);
		assertThat(popular.getContent()).extracting(GameListResponse::likeCount).containsExactly(5L, 2L);
	}

	@Test
	void getPopularListsMarksOwnerAndLikedForViewersOwnList() {
		GameList list = list(ListVisibility.PUBLIC);

		when(listLikeRepository.findPopularListIds(eq(ListVisibility.PUBLIC), eq(ProfileVisibility.PUBLIC), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(LIST_ID)));
		when(gameListRepository.findAllById(List.of(LIST_ID))).thenReturn(List.of(list));
		when(listLikeRepository.countByListId(LIST_ID)).thenReturn(3L);
		when(listLikeRepository.existsByUserIdAndListId(USER_ID, LIST_ID)).thenReturn(true);

		Page<GameListResponse> popular = gameListService.getPopularLists(USER_ID, 0, 20);

		assertThat(popular.getContent()).extracting(GameListResponse::owner).containsExactly(true);
		assertThat(popular.getContent()).extracting(GameListResponse::liked).containsExactly(true);
	}

	@Test
	void getPopularListsReturnsEmptyWhenNoListsAreLiked() {
		when(listLikeRepository.findPopularListIds(eq(ListVisibility.PUBLIC), eq(ProfileVisibility.PUBLIC), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of()));

		Page<GameListResponse> popular = gameListService.getPopularLists(null, 0, 20);

		assertThat(popular.getContent()).isEmpty();
	}

	@Test
	void getPublicListsRejectsPrivateProfile() {
		when(userRepository.findByUsername("playerone")).thenReturn(Optional.of(user(ProfileVisibility.PRIVATE)));

		assertThatThrownBy(() -> gameListService.getPublicLists("playerone", null, 0, 20))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Profile not found");
	}

	@Test
	void getMyListReturnsDetailWithItems() {
		GameList list = list(ListVisibility.PRIVATE);
		GameListItem item = item(list);

		when(gameListRepository.findByIdAndUserId(LIST_ID, USER_ID)).thenReturn(Optional.of(list));
		when(gameListItemRepository.findByListIdOrderByPositionAsc(LIST_ID)).thenReturn(List.of(item));

		GameListDetailResponse response = gameListService.getMyList(USER_ID, LIST_ID);

		assertThat(response.owner()).isTrue();
		assertThat(response.items()).hasSize(1);
		assertThat(response.items().getFirst().gameTitle()).isEqualTo("Chrono Trigger");
	}

	@Test
	void getMyListRejectsMissingList() {
		when(gameListRepository.findByIdAndUserId(LIST_ID, USER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> gameListService.getMyList(USER_ID, LIST_ID))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("List not found");
	}

	@Test
	void getPublicListReturnsDetailForPublicList() {
		GameList list = list(ListVisibility.PUBLIC);

		when(gameListRepository.findByIdAndUserUsernameAndVisibilityAndUserProfileVisibility(
				LIST_ID, "playerone", ListVisibility.PUBLIC, ProfileVisibility.PUBLIC))
				.thenReturn(Optional.of(list));
		when(gameListItemRepository.findByListIdOrderByPositionAsc(LIST_ID)).thenReturn(List.of());

		GameListDetailResponse response = gameListService.getPublicList("playerone", LIST_ID, null);

		assertThat(response.owner()).isFalse();
		assertThat(response.items()).isEmpty();
	}

	@Test
	void getPublicListRejectsPrivateOrMissingList() {
		when(gameListRepository.findByIdAndUserUsernameAndVisibilityAndUserProfileVisibility(
				LIST_ID, "playerone", ListVisibility.PUBLIC, ProfileVisibility.PUBLIC))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> gameListService.getPublicList("playerone", LIST_ID, null))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("List not found");
	}

	@Test
	void updateListUpdatesOwnedList() {
		GameList list = list(ListVisibility.PUBLIC);
		GameListRequest request = new GameListRequest("Renamed", "New description", ListVisibility.PRIVATE);

		when(gameListRepository.findByIdAndUserId(LIST_ID, USER_ID)).thenReturn(Optional.of(list));
		when(gameListRepository.save(list)).thenReturn(list);
		when(gameListItemRepository.countByListId(LIST_ID)).thenReturn(0L);

		GameListResponse response = gameListService.updateList(USER_ID, LIST_ID, request);

		assertThat(list.getName()).isEqualTo("Renamed");
		assertThat(list.getDescription()).isEqualTo("New description");
		assertThat(list.getVisibility()).isEqualTo(ListVisibility.PRIVATE);
		assertThat(response.name()).isEqualTo("Renamed");
	}

	@Test
	void deleteListDeletesOwnedList() {
		GameList list = list(ListVisibility.PUBLIC);
		when(gameListRepository.findByIdAndUserId(LIST_ID, USER_ID)).thenReturn(Optional.of(list));

		gameListService.deleteList(USER_ID, LIST_ID);

		verify(gameListRepository).delete(list);
	}

	@Test
	void deleteListRejectsMissingList() {
		when(gameListRepository.findByIdAndUserId(LIST_ID, USER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> gameListService.deleteList(USER_ID, LIST_ID))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("List not found");
	}

	@Test
	void addItemAddsGameAtNextPosition() {
		GameList list = list(ListVisibility.PUBLIC);
		Game game = game();

		when(gameListRepository.findByIdAndUserId(LIST_ID, USER_ID)).thenReturn(Optional.of(list));
		when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
		when(gameListItemRepository.existsByListIdAndGameId(LIST_ID, GAME_ID)).thenReturn(false);
		when(gameListItemRepository.maxPositionByListId(LIST_ID)).thenReturn(2);
		when(gameListItemRepository.findByListIdOrderByPositionAsc(LIST_ID))
				.thenReturn(List.of(item(list)));

		gameListService.addItem(USER_ID, LIST_ID, new AddGameListItemRequest(GAME_ID));

		ArgumentCaptor<GameListItem> captor = ArgumentCaptor.forClass(GameListItem.class);
		verify(gameListItemRepository).save(captor.capture());
		assertThat(captor.getValue().getPosition()).isEqualTo(3);
		assertThat(captor.getValue().getGame()).isSameAs(game);
	}

	@Test
	void addItemRejectsDuplicateGame() {
		GameList list = list(ListVisibility.PUBLIC);

		when(gameListRepository.findByIdAndUserId(LIST_ID, USER_ID)).thenReturn(Optional.of(list));
		when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game()));
		when(gameListItemRepository.existsByListIdAndGameId(LIST_ID, GAME_ID)).thenReturn(true);

		assertThatThrownBy(() -> gameListService.addItem(USER_ID, LIST_ID, new AddGameListItemRequest(GAME_ID)))
				.isInstanceOf(DuplicateResourceException.class)
				.hasMessage("Game is already on this list");

		verify(gameListItemRepository, never()).save(any(GameListItem.class));
	}

	@Test
	void addItemRejectsMissingGame() {
		GameList list = list(ListVisibility.PUBLIC);

		when(gameListRepository.findByIdAndUserId(LIST_ID, USER_ID)).thenReturn(Optional.of(list));
		when(gameRepository.findById(GAME_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> gameListService.addItem(USER_ID, LIST_ID, new AddGameListItemRequest(GAME_ID)))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Game not found");
	}

	@Test
	void removeItemRemovesExistingItem() {
		GameList list = list(ListVisibility.PUBLIC);
		GameListItem item = item(list);

		when(gameListRepository.findByIdAndUserId(LIST_ID, USER_ID)).thenReturn(Optional.of(list));
		when(gameListItemRepository.findByListIdAndGameId(LIST_ID, GAME_ID)).thenReturn(Optional.of(item));
		when(gameListItemRepository.findByListIdOrderByPositionAsc(LIST_ID)).thenReturn(List.of());

		gameListService.removeItem(USER_ID, LIST_ID, GAME_ID);

		verify(gameListItemRepository).delete(item);
	}

	@Test
	void removeItemRejectsMissingItem() {
		GameList list = list(ListVisibility.PUBLIC);

		when(gameListRepository.findByIdAndUserId(LIST_ID, USER_ID)).thenReturn(Optional.of(list));
		when(gameListItemRepository.findByListIdAndGameId(LIST_ID, GAME_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> gameListService.removeItem(USER_ID, LIST_ID, GAME_ID))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("List item not found");
	}

	private User user(ProfileVisibility profileVisibility) {
		User user = new User("player@example.com", "playerone", "hash", Role.USER);
		user.setDisplayName("Player One");
		user.setProfileVisibility(profileVisibility);
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

	private GameList list(ListVisibility visibility) {
		GameList list = new GameList(user(ProfileVisibility.PUBLIC), "Favorites");
		list.setVisibility(visibility);
		return withMetadata(list);
	}

	private GameList withMetadata(GameList list) {
		ReflectionTestUtils.setField(list, "id", LIST_ID);
		ReflectionTestUtils.setField(list, "createdAt", Instant.parse("2026-01-01T00:00:00Z"));
		ReflectionTestUtils.setField(list, "updatedAt", Instant.parse("2026-01-02T00:00:00Z"));

		return list;
	}

	private GameListItem item(GameList list) {
		GameListItem item = new GameListItem(list, game(), 1);
		ReflectionTestUtils.setField(item, "id", UUID.fromString("00000000-0000-0000-0000-000000000501"));
		ReflectionTestUtils.setField(item, "createdAt", Instant.parse("2026-01-03T00:00:00Z"));

		return item;
	}
}
