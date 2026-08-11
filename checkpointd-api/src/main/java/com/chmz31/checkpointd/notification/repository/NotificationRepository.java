package com.chmz31.checkpointd.notification.repository;

import com.chmz31.checkpointd.notification.entity.Notification;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

	@EntityGraph(attributePaths = {"actor", "list", "review", "review.game"})
	Page<Notification> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId, Pageable pageable);

	long countByRecipientIdAndReadFalse(UUID recipientId);

	@Modifying
	@Query("update Notification n set n.read = true where n.recipient.id = :recipientId and n.read = false")
	int markAllAsRead(@Param("recipientId") UUID recipientId);
}
