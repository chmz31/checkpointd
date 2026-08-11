package com.chmz31.checkpointd.comment.repository;

import com.chmz31.checkpointd.comment.entity.ListComment;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ListCommentRepository extends JpaRepository<ListComment, UUID> {

	@EntityGraph(attributePaths = {"user"})
	Page<ListComment> findByListIdOrderByCreatedAtDesc(UUID listId, Pageable pageable);

	@EntityGraph(attributePaths = {"user", "list"})
	Optional<ListComment> findByIdAndListId(UUID id, UUID listId);

	long countByListId(UUID listId);

	@Query("""
			select comment from ListComment comment
			join fetch comment.user
			join fetch comment.list list
			join fetch list.user
			where exists (select 1 from ListCommentReport r where r.comment = comment)
			order by comment.createdAt desc
			""")
	Page<ListComment> findReportedOrderByCreatedAtDesc(Pageable pageable);
}
