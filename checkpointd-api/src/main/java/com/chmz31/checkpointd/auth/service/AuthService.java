package com.chmz31.checkpointd.auth.service;

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
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private static final Logger log = LoggerFactory.getLogger(AuthService.class);
	private static final long TOKEN_EXPIRY_HOURS = 24;
	private static final long RESEND_COOLDOWN_SECONDS = 60;

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final EmailVerificationTokenRepository emailVerificationTokenRepository;
	private final ResendClient resendClient;
	private final String frontendUrl;

	public AuthService(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			EmailVerificationTokenRepository emailVerificationTokenRepository,
			ResendClient resendClient,
			@Value("${checkpointd.app.frontend-url}") String frontendUrl) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.emailVerificationTokenRepository = emailVerificationTokenRepository;
		this.resendClient = resendClient;
		this.frontendUrl = frontendUrl;
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
		User user = userRepository.save(new User(email, username, passwordHash, Role.USER));

		issueAndSendVerificationToken(user);

		return user;
	}

	@Transactional
	public void verifyEmail(String token) {
		EmailVerificationToken verificationToken = emailVerificationTokenRepository.findByToken(token)
				.orElseThrow(() -> new BadRequestException("Verification link is invalid or expired"));

		if (verificationToken.getExpiresAt().isBefore(Instant.now())) {
			throw new BadRequestException("Verification link is invalid or expired");
		}

		User user = verificationToken.getUser();
		user.setEmailVerified(true);
		userRepository.save(user);
		emailVerificationTokenRepository.deleteByUserId(user.getId());
	}

	@Transactional
	public void resendVerificationEmail(UUID userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

		if (user.isEmailVerified()) {
			throw new BadRequestException("Email is already verified");
		}

		emailVerificationTokenRepository.findTopByUserIdOrderByCreatedAtDesc(userId).ifPresent(lastToken -> {
			if (lastToken.getCreatedAt().isAfter(Instant.now().minusSeconds(RESEND_COOLDOWN_SECONDS))) {
				throw new BadRequestException("Please wait a moment before requesting another verification email");
			}
		});

		emailVerificationTokenRepository.deleteByUserId(userId);
		issueAndSendVerificationToken(user);
	}

	private void issueAndSendVerificationToken(User user) {
		String token = UUID.randomUUID().toString();
		Instant expiresAt = Instant.now().plus(TOKEN_EXPIRY_HOURS, ChronoUnit.HOURS);
		emailVerificationTokenRepository.save(new EmailVerificationToken(user, token, expiresAt));

		String verifyUrl = frontendUrl + "/verify-email?token=" + token;
		try {
			resendClient.sendVerificationEmail(user.getEmail(), verifyUrl);
		}
		catch (RuntimeException exception) {
			log.warn("Could not send verification email to user {}", user.getId(), exception);
		}
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
