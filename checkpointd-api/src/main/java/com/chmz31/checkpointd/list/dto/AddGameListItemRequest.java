package com.chmz31.checkpointd.list.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AddGameListItemRequest(@NotNull UUID gameId) {
}
