package com.chmz31.checkpointd.comment.repository;

import com.chmz31.checkpointd.comment.entity.ReviewCommentReport;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewCommentReportRepository extends JpaRepository<ReviewCommentReport, UUID> {

	boolean existsByCommentIdAndReporterId(UUID commentId, UUID reporterId);

	long countByCommentId(UUID commentId);
}
