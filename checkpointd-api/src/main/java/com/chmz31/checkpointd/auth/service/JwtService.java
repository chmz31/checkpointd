package com.chmz31.checkpointd.auth.service;

import com.chmz31.checkpointd.auth.dto.AuthResponse;
import com.chmz31.checkpointd.user.entity.User;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

	private final JwtEncoder jwtEncoder;
	private final long expirationMinutes;

	public JwtService(JwtEncoder jwtEncoder,
			@Value("${checkpointd.security.jwt.expiration-minutes}") long expirationMinutes) {
		this.jwtEncoder = jwtEncoder;
		this.expirationMinutes = expirationMinutes;
	}

	public AuthResponse createAuthResponse(User user) {
		return new AuthResponse(generateAccessToken(user), "Bearer", expirationMinutes);
	}

	private String generateAccessToken(User user) {
		Instant issuedAt = Instant.now();
		Instant expiresAt = issuedAt.plusSeconds(expirationMinutes * 60);
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject(user.getId().toString())
				.issuedAt(issuedAt)
				.expiresAt(expiresAt)
				.claim("email", user.getEmail())
				.claim("username", user.getUsername())
				.claim("role", user.getRole().name())
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}
}
