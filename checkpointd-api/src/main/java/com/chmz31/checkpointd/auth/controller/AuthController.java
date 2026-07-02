package com.chmz31.checkpointd.auth.controller;

import com.chmz31.checkpointd.auth.dto.AuthResponse;
import com.chmz31.checkpointd.auth.dto.CurrentUserResponse;
import com.chmz31.checkpointd.auth.dto.LoginRequest;
import com.chmz31.checkpointd.auth.dto.RegisterRequest;
import com.chmz31.checkpointd.auth.service.AuthService;
import com.chmz31.checkpointd.auth.service.JwtService;
import com.chmz31.checkpointd.user.entity.User;
import com.chmz31.checkpointd.user.model.Role;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class AuthController {

	private final AuthService authService;
	private final JwtService jwtService;

	public AuthController(AuthService authService, JwtService jwtService) {
		this.authService = authService;
		this.jwtService = jwtService;
	}

	@PostMapping("/auth/register")
	@ResponseStatus(HttpStatus.CREATED)
	AuthResponse register(@Valid @RequestBody RegisterRequest request) {
		User user = authService.register(request);

		return jwtService.createAuthResponse(user);
	}

	@PostMapping("/auth/login")
	AuthResponse login(@Valid @RequestBody LoginRequest request) {
		User user = authService.login(request);

		return jwtService.createAuthResponse(user);
	}

	@GetMapping("/users/me")
	CurrentUserResponse me(@AuthenticationPrincipal Jwt jwt) {
		return new CurrentUserResponse(
				UUID.fromString(jwt.getSubject()),
				jwt.getClaimAsString("email"),
				jwt.getClaimAsString("username"),
				Role.valueOf(jwt.getClaimAsString("role")));
	}
}
