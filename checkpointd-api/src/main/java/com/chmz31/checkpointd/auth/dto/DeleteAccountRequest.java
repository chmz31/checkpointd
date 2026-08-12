package com.chmz31.checkpointd.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record DeleteAccountRequest(@NotBlank String password) {
}
