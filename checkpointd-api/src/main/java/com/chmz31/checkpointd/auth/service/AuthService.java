package com.chmz31.checkpointd.auth.service;

import com.chmz31.checkpointd.auth.dto.LoginRequest;
import com.chmz31.checkpointd.auth.dto.RegisterRequest;
import com.chmz31.checkpointd.common.exception.DuplicateResourceException;
import com.chmz31.checkpointd.common.exception.InvalidCredentialsException;
import com.chmz31.checkpointd.user.entity.User;
import com.chmz31.checkpointd.user.model.Role;
import com.chmz31.checkpointd.user.repository.UserRepository;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public User register(RegisterRequest request) {
		String email = request.email().trim().toLowerCase();
		String username = request.username().trim();

		if (userRepository.existsByEmail(email)) {
			throw new DuplicateResourceException("Email is already registered");
		}

		if (userRepository.existsByUsername(username)) {
			throw new DuplicateResourceException("Username is already taken");
		}

		String passwordHash = passwordEncoder.encode(request.password());
		User user = new User(email, username, passwordHash, Role.USER);

		return userRepository.save(user);
	}

	@Transactional(readOnly = true)
	public User login(LoginRequest request) {
		String email = request.email().trim().toLowerCase();
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new InvalidCredentialsException("Invalid email or password");
		}

		return user;
	}

	@Transactional
	public void deleteAccount(UUID userId, String password) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

		if (!passwordEncoder.matches(password, user.getPasswordHash())) {
			throw new InvalidCredentialsException("Invalid email or password");
		}

		userRepository.delete(user);
	}
}
