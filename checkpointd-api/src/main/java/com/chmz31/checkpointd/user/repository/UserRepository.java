package com.chmz31.checkpointd.user.repository;

import com.chmz31.checkpointd.user.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

	Optional<User> findByEmail(String email);

	Optional<User> findByUsername(String username);

	boolean existsByEmail(String email);

	boolean existsByUsername(String username);

	@Query("""
			select u from User u
			where u.profileVisibility = com.chmz31.checkpointd.user.model.ProfileVisibility.PUBLIC
				and (lower(u.username) like lower(concat('%', :query, '%'))
					or lower(u.displayName) like lower(concat('%', :query, '%')))
			""")
	Page<User> searchPublicUsers(@Param("query") String query, Pageable pageable);
}
