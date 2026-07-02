package com.chmz31.checkpointd.auth.controller;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chmz31.checkpointd.auth.dto.LoginRequest;
import com.chmz31.checkpointd.auth.dto.RegisterRequest;
import com.chmz31.checkpointd.game.repository.GameRepository;
import com.chmz31.checkpointd.user.entity.User;
import com.chmz31.checkpointd.user.model.Role;
import com.chmz31.checkpointd.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@MockitoBean
	private UserRepository userRepository;

	@MockitoBean
	private GameRepository gameRepository;

	@Test
	void registerReturnsJwtAccessToken() throws Exception {
		when(userRepository.existsByEmail("player@example.com")).thenReturn(false);
		when(userRepository.existsByUsername("playerone")).thenReturn(false);
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> withId(invocation.getArgument(0)));

		mockMvc.perform(post("/api/v1/auth/register")
						.with(csrf())
						.contentType("application/json")
						.content("""
								{
								  "email": "player@example.com",
								  "username": "playerone",
								  "password": "plain-password"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.accessToken", notNullValue()))
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.expiresInMinutes").value(60));
	}

	@Test
	void duplicateRegistrationReturnsConflict() throws Exception {
		when(userRepository.existsByEmail("player@example.com")).thenReturn(true);

		mockMvc.perform(post("/api/v1/auth/register")
						.with(csrf())
						.contentType("application/json")
						.content("""
								{
								  "email": "player@example.com",
								  "username": "playerone",
								  "password": "plain-password"
								}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.message").value("Email is already registered"));
	}

	@Test
	void validationErrorReturnsBadRequest() throws Exception {
		mockMvc.perform(post("/api/v1/auth/register")
						.with(csrf())
						.contentType("application/json")
						.content("""
								{
								  "email": "not-an-email",
								  "username": "ab",
								  "password": "short"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.message").value("Validation failed"));
	}

	@Test
	void loginReturnsJwtAccessToken() throws Exception {
		User user = withId(new User("player@example.com", "playerone",
				passwordEncoder.encode("plain-password"), Role.USER));

		when(userRepository.findByEmail("player@example.com")).thenReturn(Optional.of(user));

		mockMvc.perform(post("/api/v1/auth/login")
						.with(csrf())
						.contentType("application/json")
						.content("""
								{
								  "email": "player@example.com",
								  "password": "plain-password"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken", notNullValue()))
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.expiresInMinutes").value(60));
	}

	@Test
	void invalidLoginReturnsUnauthorized() throws Exception {
		when(userRepository.findByEmail("player@example.com")).thenReturn(Optional.empty());

		mockMvc.perform(post("/api/v1/auth/login")
						.with(csrf())
						.contentType("application/json")
						.content("""
								{
								  "email": "player@example.com",
								  "password": "plain-password"
								}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.message").value("Invalid email or password"));
	}

	@Test
	void meReturnsCurrentUserWithoutPasswordHash() throws Exception {
		String accessToken = registerAndReturnToken();

		mockMvc.perform(get("/api/v1/users/me")
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value("00000000-0000-0000-0000-000000000001"))
				.andExpect(jsonPath("$.email").value("player@example.com"))
				.andExpect(jsonPath("$.username").value("playerone"))
				.andExpect(jsonPath("$.role").value("USER"))
				.andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	private String registerAndReturnToken() throws Exception {
		when(userRepository.existsByEmail("player@example.com")).thenReturn(false);
		when(userRepository.existsByUsername("playerone")).thenReturn(false);
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> withId(invocation.getArgument(0)));

		MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
						.with(csrf())
						.contentType("application/json")
						.content("""
								{
								  "email": "player@example.com",
								  "username": "playerone",
								  "password": "plain-password"
								}
								"""))
				.andExpect(status().isCreated())
				.andReturn();

		return result.getResponse().getContentAsString().replaceFirst(".*\"accessToken\":\"([^\"]+)\".*", "$1");
	}

	private User withId(User user) {
		ReflectionTestUtils.setField(user, "id", UUID.fromString("00000000-0000-0000-0000-000000000001"));

		return user;
	}
}
