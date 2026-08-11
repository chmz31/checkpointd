package com.chmz31.checkpointd.comment.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chmz31.checkpointd.comment.entity.ListComment;
import com.chmz31.checkpointd.comment.entity.ReviewComment;
import com.chmz31.checkpointd.comment.repository.ListCommentLikeRepository;
import com.chmz31.checkpointd.comment.repository.ListCommentReportRepository;
import com.chmz31.checkpointd.comment.repository.ListCommentRepository;
import com.chmz31.checkpointd.comment.repository.ReviewCommentLikeRepository;
import com.chmz31.checkpointd.comment.repository.ReviewCommentReportRepository;
import com.chmz31.checkpointd.comment.repository.ReviewCommentRepository;
import com.chmz31.checkpointd.externalgames.service.ExternalGameImportService;
import com.chmz31.checkpointd.follow.repository.FollowRepository;
import com.chmz31.checkpointd.game.entity.Game;
import com.chmz31.checkpointd.game.repository.GameRepository;
import com.chmz31.checkpointd.library.repository.LibraryEntryRepository;
import com.chmz31.checkpointd.like.repository.ListLikeRepository;
import com.chmz31.checkpointd.like.repository.ReviewLikeRepository;
import com.chmz31.checkpointd.list.entity.GameList;
import com.chmz31.checkpointd.list.model.ListVisibility;
import com.chmz31.checkpointd.list.repository.GameListItemRepository;
import com.chmz31.checkpointd.list.repository.GameListRepository;
import com.chmz31.checkpointd.notification.repository.NotificationRepository;
import com.chmz31.checkpointd.review.entity.Review;
import com.chmz31.checkpointd.review.model.ReviewVisibility;
import com.chmz31.checkpointd.review.repository.ReviewRepository;
import com.chmz31.checkpointd.user.entity.User;
import com.chmz31.checkpointd.user.model.ProfileVisibility;
import com.chmz31.checkpointd.user.model.Role;
import com.chmz31.checkpointd.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommentControllerTests {

	private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID OTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
	private static final UUID LIST_ID = UUID.fromString("00000000-0000-0000-0000-000000000401");
	private static final UUID REVIEW_ID = UUID.fromString("00000000-0000-0000-0000-000000000301");
	private static final UUID COMMENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000601");
	private static final UUID GAME_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ListCommentRepository listCommentRepository;

	@MockitoBean
	private ReviewCommentRepository reviewCommentRepository;

	@MockitoBean
	private ListCommentReportRepository listCommentReportRepository;

	@MockitoBean
	private ReviewCommentReportRepository reviewCommentReportRepository;

	@MockitoBean
	private ListCommentLikeRepository listCommentLikeRepository;

	@MockitoBean
	private ReviewCommentLikeRepository reviewCommentLikeRepository;

	@MockitoBean
	private NotificationRepository notificationRepository;

	@MockitoBean
	private ListLikeRepository listLikeRepository;

	@MockitoBean
	private ReviewLikeRepository reviewLikeRepository;

	@MockitoBean
	private UserRepository userRepository;

	@MockitoBean
	private GameRepository gameRepository;

	@MockitoBean
	private FollowRepository followRepository;

	@MockitoBean
	private LibraryEntryRepository libraryEntryRepository;

	@MockitoBean
	private ReviewRepository reviewRepository;

	@MockitoBean
	private GameListRepository gameListRepository;

	@MockitoBean
	private GameListItemRepository gameListItemRepository;

	@MockitoBean
	private ExternalGameImportService externalGameImportService;

	@Test
	void addListCommentRequiresAuthentication() throws Exception {
		mockMvc.perform(post("/api/v1/comments/lists/{listId}", LIST_ID)
						.with(csrf())
						.contentType("application/json")
						.content("{\"body\":\"Hi\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void authenticatedUserCanAddListComment() throws Exception {
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(USER_ID)));
		when(gameListRepository.findByIdAndUserId(LIST_ID, USER_ID)).thenReturn(Optional.of(list(ListVisibility.PUBLIC)));
		when(listCommentRepository.save(any(ListComment.class))).thenAnswer(invocation -> invocation.getArgument(0));

		mockMvc.perform(post("/api/v1/comments/lists/{listId}", LIST_ID)
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
						.with(csrf())
						.contentType("application/json")
						.content("{\"body\":\"Nice list!\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.body").value("Nice list!"))
				.andExpect(jsonPath("$.owner").value(true));
	}

	@Test
	void authenticatedUserCanReplyToTopLevelComment() throws Exception {
		GameList list = list(ListVisibility.PUBLIC);
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(USER_ID)));
		when(gameListRepository.findByIdAndUserId(LIST_ID, USER_ID)).thenReturn(Optional.of(list));
		when(listCommentRepository.findByIdAndListId(COMMENT_ID, LIST_ID)).thenReturn(Optional.of(listComment(list)));
		when(listCommentRepository.save(any(ListComment.class))).thenAnswer(invocation -> invocation.getArgument(0));

		mockMvc.perform(post("/api/v1/comments/lists/{listId}", LIST_ID)
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
						.with(csrf())
						.contentType("application/json")
						.content("{\"body\":\"A reply\",\"parentId\":\"" + COMMENT_ID + "\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.body").value("A reply"));
	}

	@Test
	void replyingToAReplyReturnsBadRequest() throws Exception {
		UUID replyId = UUID.fromString("00000000-0000-0000-0000-000000000602");
		GameList list = list(ListVisibility.PUBLIC);
		ListComment topLevel = listComment(list);
		ListComment existingReply = new ListComment(user(OTHER_USER_ID), list, topLevel, "First reply");
		ReflectionTestUtils.setField(existingReply, "id", replyId);

		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(USER_ID)));
		when(gameListRepository.findByIdAndUserId(LIST_ID, USER_ID)).thenReturn(Optional.of(list));
		when(listCommentRepository.findByIdAndListId(replyId, LIST_ID)).thenReturn(Optional.of(existingReply));

		mockMvc.perform(post("/api/v1/comments/lists/{listId}", LIST_ID)
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
						.with(csrf())
						.contentType("application/json")
						.content("{\"body\":\"Nested reply\",\"parentId\":\"" + replyId + "\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Replies can only be added to top-level comments"));
	}

	@Test
	void addListCommentRejectsBlankBody() throws Exception {
		mockMvc.perform(post("/api/v1/comments/lists/{listId}", LIST_ID)
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
						.with(csrf())
						.contentType("application/json")
						.content("{\"body\":\"   \"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Validation failed"));
	}

	@Test
	void listCommentsWorksWithoutAuthentication() throws Exception {
		GameList list = list(ListVisibility.PUBLIC);
		when(gameListRepository.findByIdAndVisibilityAndUserProfileVisibility(
				LIST_ID, ListVisibility.PUBLIC, ProfileVisibility.PUBLIC)).thenReturn(Optional.of(list));
		when(listCommentRepository.findByListIdAndParentIsNullOrderByCreatedAtDesc(eq(LIST_ID), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(listComment(list))));

		mockMvc.perform(get("/api/v1/comments/lists/{listId}", LIST_ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(1)))
				.andExpect(jsonPath("$.content[0].owner").value(false));
	}

	@Test
	void deleteListCommentRequiresAuthentication() throws Exception {
		mockMvc.perform(delete("/api/v1/comments/lists/{listId}/{commentId}", LIST_ID, COMMENT_ID).with(csrf()))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void authenticatedUserCanDeleteOwnListComment() throws Exception {
		when(listCommentRepository.findByIdAndListId(COMMENT_ID, LIST_ID))
				.thenReturn(Optional.of(listComment(list(ListVisibility.PUBLIC), USER_ID)));

		mockMvc.perform(delete("/api/v1/comments/lists/{listId}/{commentId}", LIST_ID, COMMENT_ID)
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
						.with(csrf()))
				.andExpect(status().isNoContent());
	}

	@Test
	void deletingOthersListCommentAsNonAdminReturnsForbidden() throws Exception {
		when(listCommentRepository.findByIdAndListId(COMMENT_ID, LIST_ID))
				.thenReturn(Optional.of(listComment(list(ListVisibility.PUBLIC), OTHER_USER_ID)));

		mockMvc.perform(delete("/api/v1/comments/lists/{listId}/{commentId}", LIST_ID, COMMENT_ID)
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
						.with(csrf()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("You can only delete your own comments"));
	}

	@Test
	void adminCanDeleteOthersListComment() throws Exception {
		when(listCommentRepository.findByIdAndListId(COMMENT_ID, LIST_ID))
				.thenReturn(Optional.of(listComment(list(ListVisibility.PUBLIC), OTHER_USER_ID)));

		mockMvc.perform(delete("/api/v1/comments/lists/{listId}/{commentId}", LIST_ID, COMMENT_ID)
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString()).claim("role", "ADMIN")))
						.with(csrf()))
				.andExpect(status().isNoContent());
	}

	@Test
	void reportListCommentRequiresAuthentication() throws Exception {
		mockMvc.perform(post("/api/v1/comments/lists/{listId}/{commentId}/report", LIST_ID, COMMENT_ID)
						.with(csrf())
						.contentType("application/json")
						.content("{}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void authenticatedUserCanReportListComment() throws Exception {
		when(listCommentRepository.findByIdAndListId(COMMENT_ID, LIST_ID))
				.thenReturn(Optional.of(listComment(list(ListVisibility.PUBLIC), OTHER_USER_ID)));
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(USER_ID)));
		when(listCommentReportRepository.existsByCommentIdAndReporterId(COMMENT_ID, USER_ID)).thenReturn(false);

		mockMvc.perform(post("/api/v1/comments/lists/{listId}/{commentId}/report", LIST_ID, COMMENT_ID)
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
						.with(csrf())
						.contentType("application/json")
						.content("{\"reason\":\"Spam\"}"))
				.andExpect(status().isNoContent());
	}

	@Test
	void reportingOwnListCommentReturnsBadRequest() throws Exception {
		when(listCommentRepository.findByIdAndListId(COMMENT_ID, LIST_ID))
				.thenReturn(Optional.of(listComment(list(ListVisibility.PUBLIC), USER_ID)));
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(USER_ID)));

		mockMvc.perform(post("/api/v1/comments/lists/{listId}/{commentId}/report", LIST_ID, COMMENT_ID)
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
						.with(csrf())
						.contentType("application/json")
						.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("You cannot report your own comment"));
	}

	@Test
	void addReviewCommentRequiresAuthentication() throws Exception {
		mockMvc.perform(post("/api/v1/comments/reviews/{reviewId}", REVIEW_ID)
						.with(csrf())
						.contentType("application/json")
						.content("{\"body\":\"Hi\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void authenticatedUserCanAddReviewComment() throws Exception {
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(USER_ID)));
		when(reviewRepository.findByIdAndUserId(REVIEW_ID, USER_ID)).thenReturn(Optional.of(review(ReviewVisibility.PUBLIC)));
		when(reviewCommentRepository.save(any(ReviewComment.class))).thenAnswer(invocation -> invocation.getArgument(0));

		mockMvc.perform(post("/api/v1/comments/reviews/{reviewId}", REVIEW_ID)
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
						.with(csrf())
						.contentType("application/json")
						.content("{\"body\":\"Great review\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.body").value("Great review"));
	}

	@Test
	void reviewCommentsWorksWithoutAuthentication() throws Exception {
		Review review = review(ReviewVisibility.PUBLIC);
		when(reviewRepository.findByIdAndVisibilityAndUserProfileVisibility(
				REVIEW_ID, ReviewVisibility.PUBLIC, ProfileVisibility.PUBLIC)).thenReturn(Optional.of(review));
		when(reviewCommentRepository.findByReviewIdAndParentIsNullOrderByCreatedAtDesc(eq(REVIEW_ID), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(reviewComment(review))));

		mockMvc.perform(get("/api/v1/comments/reviews/{reviewId}", REVIEW_ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(1)));
	}

	@Test
	void deletingOthersReviewCommentAsNonAdminReturnsForbidden() throws Exception {
		when(reviewCommentRepository.findByIdAndReviewId(COMMENT_ID, REVIEW_ID))
				.thenReturn(Optional.of(reviewComment(review(ReviewVisibility.PUBLIC), OTHER_USER_ID)));

		mockMvc.perform(delete("/api/v1/comments/reviews/{reviewId}/{commentId}", REVIEW_ID, COMMENT_ID)
						.with(jwt().jwt(jwt -> jwt.subject(USER_ID.toString())))
						.with(csrf()))
				.andExpect(status().isForbidden());
	}

	private User user(UUID id) {
		User user = new User(id + "@example.com", "player-" + id, "hash", Role.USER);
		user.setDisplayName("Player " + id);
		user.setProfileVisibility(ProfileVisibility.PUBLIC);
		ReflectionTestUtils.setField(user, "id", id);

		return user;
	}

	private GameList list(ListVisibility visibility) {
		GameList list = new GameList(user(OTHER_USER_ID), "Favorites");
		list.setVisibility(visibility);
		ReflectionTestUtils.setField(list, "id", LIST_ID);

		return list;
	}

	private Game game() {
		Game game = new Game("Chrono Trigger");
		ReflectionTestUtils.setField(game, "id", GAME_ID);

		return game;
	}

	private Review review(ReviewVisibility visibility) {
		Review review = new Review(user(OTHER_USER_ID), game(), "A public opinion.");
		review.setVisibility(visibility);
		ReflectionTestUtils.setField(review, "id", REVIEW_ID);

		return review;
	}

	private ListComment listComment(GameList list) {
		return listComment(list, OTHER_USER_ID);
	}

	private ListComment listComment(GameList list, UUID authorId) {
		ListComment comment = new ListComment(user(authorId), list, "Great list!");
		ReflectionTestUtils.setField(comment, "id", COMMENT_ID);

		return comment;
	}

	private ReviewComment reviewComment(Review review) {
		return reviewComment(review, OTHER_USER_ID);
	}

	private ReviewComment reviewComment(Review review, UUID authorId) {
		ReviewComment comment = new ReviewComment(user(authorId), review, "Great review!");
		ReflectionTestUtils.setField(comment, "id", COMMENT_ID);

		return comment;
	}
}
