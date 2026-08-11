package com.chmz31.checkpointd.list.service;

import com.chmz31.checkpointd.common.exception.DuplicateResourceException;
import com.chmz31.checkpointd.common.exception.ResourceNotFoundException;
import com.chmz31.checkpointd.game.entity.Game;
import com.chmz31.checkpointd.game.repository.GameRepository;
import com.chmz31.checkpointd.like.repository.ListLikeRepository;
import com.chmz31.checkpointd.list.dto.AddGameListItemRequest;
import com.chmz31.checkpointd.list.dto.GameListDetailResponse;
import com.chmz31.checkpointd.list.dto.GameListItemResponse;
import com.chmz31.checkpointd.list.dto.GameListRequest;
import com.chmz31.checkpointd.list.dto.GameListResponse;
import com.chmz31.checkpointd.list.entity.GameList;
import com.chmz31.checkpointd.list.entity.GameListItem;
import com.chmz31.checkpointd.list.model.ListVisibility;
import com.chmz31.checkpointd.list.repository.GameListItemRepository;
import com.chmz31.checkpointd.list.repository.GameListRepository;
import com.chmz31.checkpointd.user.entity.User;
import com.chmz31.checkpointd.user.model.ProfileVisibility;
import com.chmz31.checkpointd.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GameListService {

	private static final int DEFAULT_PAGE_SIZE = 20;
	private static final int MAX_PAGE_SIZE = 50;

	private final GameListRepository gameListRepository;
	private final GameListItemRepository gameListItemRepository;
	private final ListLikeRepository listLikeRepository;
	private final UserRepository userRepository;
	private final GameRepository gameRepository;

	public GameListService(
			GameListRepository gameListRepository,
			GameListItemRepository gameListItemRepository,
			ListLikeRepository listLikeRepository,
			UserRepository userRepository,
			GameRepository gameRepository) {
		this.gameListRepository = gameListRepository;
		this.gameListItemRepository = gameListItemRepository;
		this.listLikeRepository = listLikeRepository;
		this.userRepository = userRepository;
		this.gameRepository = gameRepository;
	}

	@Transactional
	public GameListResponse createList(UUID userId, GameListRequest request) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		GameList list = new GameList(user, request.name().trim());
		list.setDescription(cleanDescription(request.description()));
		list.setVisibility(request.visibility() == null ? ListVisibility.PUBLIC : request.visibility());

		GameList saved = gameListRepository.save(list);
		return GameListResponse.from(saved, 0, 0, false, true);
	}

	@Transactional(readOnly = true)
	public Page<GameListResponse> getMyLists(UUID userId, int page, int size) {
		return gameListRepository.findByUserIdOrderByUpdatedAtDesc(userId, pageRequest(page, size))
				.map(list -> toResponse(list, userId, true));
	}

	@Transactional(readOnly = true)
	public Page<GameListResponse> getPublicLists(String username, UUID currentUserId, int page, int size) {
		User user = publicUser(username);
		return gameListRepository.findByUserUsernameAndUserProfileVisibilityAndVisibilityOrderByUpdatedAtDesc(
				user.getUsername(), ProfileVisibility.PUBLIC, ListVisibility.PUBLIC, pageRequest(page, size))
				.map(list -> toResponse(list, currentUserId, false));
	}

	@Transactional(readOnly = true)
	public Page<GameListResponse> getPopularLists(UUID currentUserId, int page, int size) {
		Page<UUID> popularIds = listLikeRepository.findPopularListIds(
				ListVisibility.PUBLIC, ProfileVisibility.PUBLIC, pageRequest(page, size));

		Map<UUID, GameList> listsById = gameListRepository.findAllById(popularIds.getContent()).stream()
				.collect(Collectors.toMap(GameList::getId, Function.identity()));

		List<GameListResponse> content = popularIds.getContent().stream()
				.map(listsById::get)
				.filter(Objects::nonNull)
				.map(list -> toResponse(
						list, currentUserId, currentUserId != null && list.getUser().getId().equals(currentUserId)))
				.toList();

		return new PageImpl<>(content, popularIds.getPageable(), popularIds.getTotalElements());
	}

	@Transactional(readOnly = true)
	public GameListDetailResponse getMyList(UUID userId, UUID listId) {
		GameList list = gameListRepository.findByIdAndUserId(listId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("List not found"));

		return toDetailResponse(list, userId, true);
	}

	@Transactional(readOnly = true)
	public GameListDetailResponse getPublicList(String username, UUID listId, UUID currentUserId) {
		GameList list = gameListRepository.findByIdAndUserUsernameAndVisibilityAndUserProfileVisibility(
				listId, username, ListVisibility.PUBLIC, ProfileVisibility.PUBLIC)
				.orElseThrow(() -> new ResourceNotFoundException("List not found"));

		return toDetailResponse(list, currentUserId, false);
	}

	@Transactional
	public GameListResponse updateList(UUID userId, UUID listId, GameListRequest request) {
		GameList list = getOwnedList(userId, listId);
		list.setName(request.name().trim());
		list.setDescription(cleanDescription(request.description()));
		list.setVisibility(request.visibility() == null ? ListVisibility.PUBLIC : request.visibility());

		GameList saved = gameListRepository.save(list);
		return toResponse(saved, userId, true);
	}

	@Transactional
	public void deleteList(UUID userId, UUID listId) {
		GameList list = getOwnedList(userId, listId);
		gameListRepository.delete(list);
	}

	@Transactional
	public GameListDetailResponse addItem(UUID userId, UUID listId, AddGameListItemRequest request) {
		GameList list = getOwnedList(userId, listId);
		Game game = gameRepository.findById(request.gameId())
				.orElseThrow(() -> new ResourceNotFoundException("Game not found"));

		if (gameListItemRepository.existsByListIdAndGameId(listId, game.getId())) {
			throw new DuplicateResourceException("Game is already on this list");
		}

		int nextPosition = gameListItemRepository.maxPositionByListId(listId) + 1;
		gameListItemRepository.save(new GameListItem(list, game, nextPosition));

		return toDetailResponse(list, userId, true);
	}

	@Transactional
	public GameListDetailResponse removeItem(UUID userId, UUID listId, UUID gameId) {
		GameList list = getOwnedList(userId, listId);
		GameListItem item = gameListItemRepository.findByListIdAndGameId(listId, gameId)
				.orElseThrow(() -> new ResourceNotFoundException("List item not found"));

		gameListItemRepository.delete(item);
		return toDetailResponse(list, userId, true);
	}

	private GameList getOwnedList(UUID userId, UUID listId) {
		return gameListRepository.findByIdAndUserId(listId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("List not found"));
	}

	private List<GameListItemResponse> items(UUID listId) {
		return gameListItemRepository.findByListIdOrderByPositionAsc(listId).stream()
				.map(GameListItemResponse::from)
				.toList();
	}

	private GameListResponse toResponse(GameList list, UUID currentUserId, boolean owner) {
		long likeCount = listLikeRepository.countByListId(list.getId());
		boolean liked = currentUserId != null && listLikeRepository.existsByUserIdAndListId(currentUserId, list.getId());
		return GameListResponse.from(list, gameListItemRepository.countByListId(list.getId()), likeCount, liked, owner);
	}

	private GameListDetailResponse toDetailResponse(GameList list, UUID currentUserId, boolean owner) {
		long likeCount = listLikeRepository.countByListId(list.getId());
		boolean liked = currentUserId != null && listLikeRepository.existsByUserIdAndListId(currentUserId, list.getId());
		return GameListDetailResponse.from(list, items(list.getId()), likeCount, liked, owner);
	}

	private User publicUser(String username) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new ResourceNotFoundException("Profile not found"));
		if (user.getProfileVisibility() != ProfileVisibility.PUBLIC) {
			throw new ResourceNotFoundException("Profile not found");
		}
		return user;
	}

	private String cleanDescription(String description) {
		if (description == null || description.isBlank()) {
			return null;
		}
		return description.trim();
	}

	private PageRequest pageRequest(int page, int size) {
		int safePage = Math.max(page, 0);
		int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
		return PageRequest.of(safePage, safeSize);
	}
}
