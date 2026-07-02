package com.chmz31.checkpointd.library.repository;

import com.chmz31.checkpointd.library.entity.LibraryEntry;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LibraryEntryRepository extends JpaRepository<LibraryEntry, UUID> {
}
