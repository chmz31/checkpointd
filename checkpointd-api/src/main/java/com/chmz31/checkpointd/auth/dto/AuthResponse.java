package com.chmz31.checkpointd.auth.dto;

public record AuthResponse(String accessToken, String tokenType, long expiresInMinutes) {
}
