package com.chmz31.checkpointd.common.exception;

public record ApiError(int status, String message) {
}
