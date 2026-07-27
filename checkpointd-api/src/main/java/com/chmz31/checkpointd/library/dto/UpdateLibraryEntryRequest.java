package com.chmz31.checkpointd.library.dto;

import com.chmz31.checkpointd.library.model.LibraryStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateLibraryEntryRequest(
		LibraryStatus status,
		@Min(1) @Max(10) Integer rating,
		@Size(max = 5000) String notes,
		LocalDate startedAt,
		LocalDate completedAt) {
}
