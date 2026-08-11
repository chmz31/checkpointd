package com.chmz31.checkpointd.common.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(DuplicateResourceException.class)
	ResponseEntity<ApiError> handleDuplicateResource(DuplicateResourceException exception) {
		return error(HttpStatus.CONFLICT, exception.getMessage());
	}

	@ExceptionHandler(InvalidCredentialsException.class)
	ResponseEntity<ApiError> handleInvalidCredentials(InvalidCredentialsException exception) {
		return error(HttpStatus.UNAUTHORIZED, exception.getMessage());
	}

	@ExceptionHandler(ForbiddenException.class)
	ResponseEntity<ApiError> handleForbidden(ForbiddenException exception) {
		return error(HttpStatus.FORBIDDEN, exception.getMessage());
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	ResponseEntity<ApiError> handleResourceNotFound(ResourceNotFoundException exception) {
		return error(HttpStatus.NOT_FOUND, exception.getMessage());
	}

	@ExceptionHandler(BadRequestException.class)
	ResponseEntity<ApiError> handleBadRequest(BadRequestException exception) {
		return error(HttpStatus.BAD_REQUEST, exception.getMessage());
	}

	@ExceptionHandler(ServiceUnavailableException.class)
	ResponseEntity<ApiError> handleServiceUnavailable(ServiceUnavailableException exception) {
		return error(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiError> handleValidation() {
		return error(HttpStatus.BAD_REQUEST, "Validation failed");
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	ResponseEntity<ApiError> handleMissingRequestParameter() {
		return error(HttpStatus.BAD_REQUEST, "Validation failed");
	}

	@ExceptionHandler(ConstraintViolationException.class)
	ResponseEntity<ApiError> handleConstraintViolation() {
		return error(HttpStatus.BAD_REQUEST, "Validation failed");
	}

	private ResponseEntity<ApiError> error(HttpStatus status, String message) {
		return ResponseEntity.status(status).body(new ApiError(status.value(), message));
	}
}
