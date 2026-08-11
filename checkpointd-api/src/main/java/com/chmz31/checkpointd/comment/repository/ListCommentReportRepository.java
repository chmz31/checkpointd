package com.chmz31.checkpointd.comment.repository;

import com.chmz31.checkpointd.comment.entity.ListCommentReport;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListCommentReportRepository extends JpaRepository<ListCommentReport, UUID> {

	boolean existsByCommentIdAndReporterId(UUID commentId, UUID reporterId);

	long countByCommentId(UUID commentId);
}
