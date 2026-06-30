package com.chmz31.checkpointd.user.repository;

import com.chmz31.checkpointd.user.entity.User;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
}
