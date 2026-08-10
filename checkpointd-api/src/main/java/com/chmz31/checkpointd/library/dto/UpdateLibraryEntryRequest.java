package com.chmz31.checkpointd.library.dto;

import com.chmz31.checkpointd.library.model.LibraryStatus;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateLibraryEntryRequest(
		LibraryStatus status,
		@Size(max = 5000) String notes,
		LocalDate startedAt,
		LocalDate completedAt) {
}
