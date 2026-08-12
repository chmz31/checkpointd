package com.chmz31.checkpointd.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chmz31.checkpointd.auth.dto.LoginRequest;
import com.chmz31.checkpointd.auth.dto.RegisterRequest;
import com.chmz31.checkpointd.common.exception.DuplicateResourceException;
import com.chmz31.checkpointd.common.exception.InvalidCredentialsException;
import com.chmz31.checkpointd.user.entity.User;
import com.chmz31.checkpointd.user.model.Role;
import com.chmz31.checkpointd.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTests {

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@InjectMocks
	private AuthService authService;

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
}
