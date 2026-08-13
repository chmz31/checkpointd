package com.chmz31.checkpointd.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chmz31.checkpointd.auth.dto.LoginRequest;
import com.chmz31.checkpointd.auth.dto.RegisterRequest;
import com.chmz31.checkpointd.auth.entity.EmailVerificationToken;
import com.chmz31.checkpointd.auth.repository.EmailVerificationTokenRepository;
import com.chmz31.checkpointd.common.exception.BadRequestException;
import com.chmz31.checkpointd.common.exception.DuplicateResourceException;
import com.chmz31.checkpointd.common.exception.InvalidCredentialsException;
import com.chmz31.checkpointd.user.entity.User;
import com.chmz31.checkpointd.user.model.Role;
import com.chmz31.checkpointd.user.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTests {

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private EmailVerificationTokenRepository emailVerificationTokenRepository;

	@Mock
	private ResendClient resendClient;

	@InjectMocks
	private AuthService authService;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(authService, "frontendUrl", "http://localhost:5173");
	}

	@Test
	void registerCreatesUserWithHashedPassword() {
		RegisterRequest request = new RegisterRequest(" Player@Example.COM ", " playerone ", "plain-password");

		when(userRepository.existsByEmail("player@example.com")).thenReturn(false);
		when(userRepository.existsByUsername("playerone")).thenReturn(false);
		when(passwordEncoder.encode(request.password())).thenReturn("hashed-password");
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		User registeredUser = authService.register(request);

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).existsByEmail("player@example.com");
		verify(userRepository).existsByUsername("playerone");
		verify(userRepository).save(userCaptor.capture());

		User savedUser = userCaptor.getValue();
		assertThat(savedUser.getEmail()).isEqualTo("player@example.com");
		assertThat(savedUser.getUsername()).isEqualTo("playerone");
		assertThat(savedUser.getPasswordHash()).isEqualTo("hashed-password");
		assertThat(savedUser.getPasswordHash()).isNotEqualTo(request.password());
		assertThat(savedUser.getRole()).isEqualTo(Role.USER);
		assertThat(registeredUser).isSameAs(savedUser);
	}

	@Test
	void registerIssuesVerificationTokenAndSendsEmail() {
		RegisterRequest request = new RegisterRequest(" Player@Example.COM ", " playerone ", "plain-password");

		when(userRepository.existsByEmail("player@example.com")).thenReturn(false);
		when(userRepository.existsByUsername("playerone")).thenReturn(false);
		when(passwordEncoder.encode(request.password())).thenReturn("hashed-password");
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		authService.register(request);

		ArgumentCaptor<EmailVerificationToken> tokenCaptor = ArgumentCaptor.forClass(EmailVerificationToken.class);
		verify(emailVerificationTokenRepository).save(tokenCaptor.capture());
		assertThat(tokenCaptor.getValue().getToken()).isNotBlank();
		assertThat(tokenCaptor.getValue().getExpiresAt()).isAfter(Instant.now());
		verify(resendClient).sendVerificationEmail(eq("player@example.com"), anyString());
	}

	@Test
	void registerSucceedsEvenWhenEmailSendFails() {
		RegisterRequest request = new RegisterRequest(" Player@Example.COM ", " playerone ", "plain-password");

		when(userRepository.existsByEmail("player@example.com")).thenReturn(false);
		when(userRepository.existsByUsername("playerone")).thenReturn(false);
		when(passwordEncoder.encode(request.password())).thenReturn("hashed-password");
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
		doThrow(new RuntimeException("email provider down")).when(resendClient).sendVerificationEmail(anyString(), anyString());

		User registeredUser = authService.register(request);

		assertThat(registeredUser.getEmail()).isEqualTo("player@example.com");
	}

	@Test
	void registerRejectsDuplicateEmail() {
		RegisterRequest request = new RegisterRequest(" Player@Example.COM ", " playerone ", "plain-password");

		when(userRepository.existsByEmail("player@example.com")).thenReturn(true);

		assertThatThrownBy(() -> authService.register(request))
				.isInstanceOf(DuplicateResourceException.class)
				.hasMessage("Email is already registered");

		verify(userRepository).existsByEmail("player@example.com");
		verify(userRepository, never()).existsByUsername("playerone");
		verify(passwordEncoder, never()).encode(request.password());
		verify(userRepository, never()).save(any(User.class));
	}

	@Test
	void registerRejectsDuplicateUsername() {
		RegisterRequest request = new RegisterRequest(" Player@Example.COM ", " playerone ", "plain-password");

		when(userRepository.existsByEmail("player@example.com")).thenReturn(false);
		when(userRepository.existsByUsername("playerone")).thenReturn(true);

		assertThatThrownBy(() -> authService.register(request))
				.isInstanceOf(DuplicateResourceException.class)
				.hasMessage("Username is already taken");

		verify(userRepository).existsByEmail("player@example.com");
		verify(userRepository).existsByUsername("playerone");
		verify(passwordEncoder, never()).encode(request.password());
		verify(userRepository, never()).save(any(User.class));
	}

	@Test
	void loginReturnsUserForValidCredentials() {
		LoginRequest request = new LoginRequest(" Player@Example.COM ", "plain-password");
		User user = new User("player@example.com", "playerone", "hashed-password", Role.USER);

		when(userRepository.findByEmail("player@example.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches(request.password(), user.getPasswordHash())).thenReturn(true);

		User loggedInUser = authService.login(request);

		assertThat(loggedInUser).isSameAs(user);
		verify(userRepository).findByEmail("player@example.com");
		verify(passwordEncoder).matches(request.password(), user.getPasswordHash());
	}

	@Test
	void loginRejectsUnknownEmail() {
		LoginRequest request = new LoginRequest(" Player@Example.COM ", "plain-password");

		when(userRepository.findByEmail("player@example.com")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.login(request))
				.isInstanceOf(InvalidCredentialsException.class)
				.hasMessage("Invalid email or password");

		verify(userRepository).findByEmail("player@example.com");
		verify(passwordEncoder, never()).matches(any(), any());
	}

	@Test
	void loginRejectsWrongPassword() {
		LoginRequest request = new LoginRequest(" Player@Example.COM ", "plain-password");
		User user = new User("player@example.com", "playerone", "hashed-password", Role.USER);

		when(userRepository.findByEmail("player@example.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches(request.password(), user.getPasswordHash())).thenReturn(false);

		assertThatThrownBy(() -> authService.login(request))
				.isInstanceOf(InvalidCredentialsException.class)
				.hasMessage("Invalid email or password");

		verify(userRepository).findByEmail("player@example.com");
		verify(passwordEncoder).matches(request.password(), user.getPasswordHash());
	}

	@Test
	void deleteAccountRemovesUserForValidPassword() {
		UUID userId = UUID.randomUUID();
		User user = new User("player@example.com", "playerone", "hashed-password", Role.USER);

		when(userRepository.findById(userId)).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("plain-password", user.getPasswordHash())).thenReturn(true);

		authService.deleteAccount(userId, "plain-password");

		verify(userRepository).findById(userId);
		verify(passwordEncoder).matches("plain-password", user.getPasswordHash());
		verify(userRepository).delete(user);
	}

	@Test
	void deleteAccountRejectsUnknownUser() {
		UUID userId = UUID.randomUUID();

		when(userRepository.findById(userId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.deleteAccount(userId, "plain-password"))
				.isInstanceOf(InvalidCredentialsException.class)
				.hasMessage("Invalid email or password");

		verify(userRepository).findById(userId);
		verify(passwordEncoder, never()).matches(any(), any());
		verify(userRepository, never()).delete(any(User.class));
	}

	@Test
	void deleteAccountRejectsWrongPassword() {
		UUID userId = UUID.randomUUID();
		User user = new User("player@example.com", "playerone", "hashed-password", Role.USER);

		when(userRepository.findById(userId)).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("wrong-password", user.getPasswordHash())).thenReturn(false);

		assertThatThrownBy(() -> authService.deleteAccount(userId, "wrong-password"))
				.isInstanceOf(InvalidCredentialsException.class)
				.hasMessage("Invalid email or password");

		verify(userRepository).findById(userId);
		verify(passwordEncoder).matches("wrong-password", user.getPasswordHash());
		verify(userRepository, never()).delete(any(User.class));
	}

	@Test
	void verifyEmailMarksUserVerifiedAndDeletesTokens() {
		User user = new User("player@example.com", "playerone", "hashed-password", Role.USER);
		EmailVerificationToken token = new EmailVerificationToken(user, "good-token", Instant.now().plusSeconds(3600));

		when(emailVerificationTokenRepository.findByToken("good-token")).thenReturn(Optional.of(token));

		authService.verifyEmail("good-token");

		assertThat(user.isEmailVerified()).isTrue();
		verify(userRepository).save(user);
		verify(emailVerificationTokenRepository).deleteByUserId(user.getId());
	}

	@Test
	void verifyEmailRejectsUnknownToken() {
		when(emailVerificationTokenRepository.findByToken("missing")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.verifyEmail("missing"))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("Verification link is invalid or expired");
	}

	@Test
	void verifyEmailRejectsExpiredToken() {
		User user = new User("player@example.com", "playerone", "hashed-password", Role.USER);
		EmailVerificationToken token = new EmailVerificationToken(user, "old-token", Instant.now().minusSeconds(60));

		when(emailVerificationTokenRepository.findByToken("old-token")).thenReturn(Optional.of(token));

		assertThatThrownBy(() -> authService.verifyEmail("old-token"))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("Verification link is invalid or expired");

		verify(userRepository, never()).save(any(User.class));
	}

	@Test
	void resendVerificationEmailRejectsAlreadyVerifiedUser() {
		UUID userId = UUID.randomUUID();
		User user = new User("player@example.com", "playerone", "hashed-password", Role.USER);
		user.setEmailVerified(true);

		when(userRepository.findById(userId)).thenReturn(Optional.of(user));

		assertThatThrownBy(() -> authService.resendVerificationEmail(userId))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("Email is already verified");

		verify(emailVerificationTokenRepository, never()).save(any(EmailVerificationToken.class));
	}

	@Test
	void resendVerificationEmailRejectsWhenWithinCooldown() {
		UUID userId = UUID.randomUUID();
		User user = new User("player@example.com", "playerone", "hashed-password", Role.USER);
		EmailVerificationToken lastToken = new EmailVerificationToken(user, "recent-token", Instant.now().plusSeconds(3600));
		ReflectionTestUtils.setField(lastToken, "createdAt", Instant.now());

		when(userRepository.findById(userId)).thenReturn(Optional.of(user));
		when(emailVerificationTokenRepository.findTopByUserIdOrderByCreatedAtDesc(userId)).thenReturn(Optional.of(lastToken));

		assertThatThrownBy(() -> authService.resendVerificationEmail(userId))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("wait");

		verify(emailVerificationTokenRepository, never()).deleteByUserId(userId);
	}

	@Test
	void resendVerificationEmailIssuesNewTokenAfterCooldown() {
		UUID userId = UUID.randomUUID();
		User user = new User("player@example.com", "playerone", "hashed-password", Role.USER);
		EmailVerificationToken lastToken = new EmailVerificationToken(user, "old-token", Instant.now().plusSeconds(3600));
		ReflectionTestUtils.setField(lastToken, "createdAt", Instant.now().minusSeconds(120));

		when(userRepository.findById(userId)).thenReturn(Optional.of(user));
		when(emailVerificationTokenRepository.findTopByUserIdOrderByCreatedAtDesc(userId)).thenReturn(Optional.of(lastToken));

		authService.resendVerificationEmail(userId);

		verify(emailVerificationTokenRepository).deleteByUserId(userId);
		verify(emailVerificationTokenRepository).save(any(EmailVerificationToken.class));
		verify(resendClient).sendVerificationEmail(eq("player@example.com"), anyString());
	}
}
