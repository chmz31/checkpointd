package com.chmz31.checkpointd.like.repository;

import com.chmz31.checkpointd.like.entity.ListLike;
import com.chmz31.checkpointd.list.model.ListVisibility;
import com.chmz31.checkpointd.user.model.ProfileVisibility;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ListLikeRepository extends JpaRepository<ListLike, UUID> {

	boolean existsByUserIdAndListId(UUID userId, UUID listId);

	Optional<ListLike> findByUserIdAndListId(UUID userId, UUID listId);

	long countByListId(UUID listId);

	@Query("""
			select l.list.id from ListLike l
			where l.list.visibility = :visibility and l.list.user.profileVisibility = :profileVisibility
			group by l.list.id
			order by count(l) desc
			""")
	Page<UUID> findPopularListIds(
			@Param("visibility") ListVisibility visibility,
			@Param("profileVisibility") ProfileVisibility profileVisibility,
			Pageable pageable);
}
