package com.chmz31.checkpointd.library.dto;

import com.chmz31.checkpointd.library.model.LibraryStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record AddLibraryEntryRequest(
		@NotNull UUID gameId,
		@NotNull LibraryStatus status,
		@Size(max = 5000) String notes,
		LocalDate startedAt,
		LocalDate completedAt) {
}
