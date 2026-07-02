package com.chmz31.checkpointd.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chmz31.checkpointd.auth.dto.RegisterRequest;
import com.chmz31.checkpointd.common.exception.DuplicateResourceException;
import com.chmz31.checkpointd.user.entity.User;
import com.chmz31.checkpointd.user.model.Role;
import com.chmz31.checkpointd.user.repository.UserRepository;
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
}
