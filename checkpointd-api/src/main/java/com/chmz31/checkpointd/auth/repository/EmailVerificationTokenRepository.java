package com.chmz31.checkpointd.auth.repository;

import com.chmz31.checkpointd.auth.entity.EmailVerificationToken;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {

	Optional<EmailVerificationToken> findByToken(String token);

	Optional<EmailVerificationToken> findTopByUserIdOrderByCreatedAtDesc(UUID userId);

	void deleteByUserId(UUID userId);
}
