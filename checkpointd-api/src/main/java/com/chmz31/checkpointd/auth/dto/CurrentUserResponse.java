package com.chmz31.checkpointd.auth.dto;

import com.chmz31.checkpointd.user.model.Role;
import java.util.UUID;

public record CurrentUserResponse(UUID id, String email, String username, Role role) {
}
