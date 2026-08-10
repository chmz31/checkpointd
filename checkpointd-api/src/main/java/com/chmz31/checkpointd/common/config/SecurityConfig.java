package com.chmz31.checkpointd.common.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.cors(Customizer.withDefaults())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login").permitAll()
						.requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/info").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/profiles/me").authenticated()
						.requestMatchers(HttpMethod.PATCH, "/api/v1/profiles/me").authenticated()
						.requestMatchers(HttpMethod.GET, "/api/v1/profiles/*").permitAll()
						.requestMatchers("/api/v1/reviews/me", "/api/v1/reviews/me/**").authenticated()
						.requestMatchers(HttpMethod.GET, "/api/v1/reviews/games/*").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/reviews/users/*").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/reviews/users/*/games/*").permitAll()
						.requestMatchers("/api/v1/*").authenticated()
						.requestMatchers("/api/v1/**").authenticated()
						.anyRequest().denyAll())
				.oauth2ResourceServer(oauth2 -> oauth2
						.jwt(Customizer.withDefaults())
						.authenticationEntryPoint((request, response, exception) ->
								writeError(response, HttpStatus.UNAUTHORIZED, "Unauthorized")));

		return http.build();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(
			@Value("${checkpointd.cors.allowed-origins:}") String allowedOrigins) {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(parseAllowedOrigins(allowedOrigins));
		configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
		configuration.setAllowCredentials(false);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);

		return source;
	}

	@Bean
	JwtEncoder jwtEncoder(@Value("${checkpointd.security.jwt.secret}") String secret) {
		return new NimbusJwtEncoder(new com.nimbusds.jose.jwk.source.ImmutableSecret<>(jwtSecretKey(secret)));
	}

	@Bean
	JwtDecoder jwtDecoder(@Value("${checkpointd.security.jwt.secret}") String secret) {
		return NimbusJwtDecoder.withSecretKey(jwtSecretKey(secret))
				.macAlgorithm(MacAlgorithm.HS256)
				.build();
	}

	private SecretKey jwtSecretKey(String secret) {
		if (secret == null || secret.isBlank()) {
			throw new IllegalStateException("checkpointd.security.jwt.secret must be configured");
		}
		try {
			byte[] keyBytes = MessageDigest.getInstance("SHA-256")
					.digest(secret.getBytes(StandardCharsets.UTF_8));
			return new SecretKeySpec(keyBytes, "HmacSHA256");
		}
		catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is not available", exception);
		}
	}

	private List<String> parseAllowedOrigins(String allowedOrigins) {
		if (allowedOrigins == null || allowedOrigins.isBlank()) {
			return List.of();
		}

		return Arrays.stream(allowedOrigins.split(","))
				.map(String::trim)
				.filter(origin -> !origin.isBlank())
				.toList();
	}

	private void writeError(jakarta.servlet.http.HttpServletResponse response, HttpStatus status, String message)
			throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.getWriter().write("{\"status\":" + status.value() + ",\"message\":\"" + message + "\"}");
	}
}
