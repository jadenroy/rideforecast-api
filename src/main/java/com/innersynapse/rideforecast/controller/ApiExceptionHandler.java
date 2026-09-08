package com.innersynapse.rideforecast.controller;

import com.innersynapse.rideforecast.dto.ApiErrorResponse;
import com.innersynapse.rideforecast.exception.InvalidQuoteSubmissionException;
import com.innersynapse.rideforecast.exception.ProfileNotFoundException;
import com.innersynapse.rideforecast.auth.InvalidAuthenticationException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> invalidBody(MethodArgumentNotValidException exception) {
        Map<String, String> fields = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                fields.putIfAbsent(error.getField(), error.getDefaultMessage())
        );
        boolean profileRequest = exception.getBindingResult().getObjectName().equals("userProfileRequest");
        return ResponseEntity.badRequest().body(new ApiErrorResponse(
                profileRequest ? "INVALID_PROFILE" : "INVALID_QUOTE",
                profileRequest
                        ? "Check the highlighted profile details and try again."
                        : "Check the highlighted quote details and try again.",
                fields,
                null
        ));
    }

    @ExceptionHandler({ConstraintViolationException.class, HttpMessageNotReadableException.class})
    ResponseEntity<ApiErrorResponse> invalidRequest(Exception exception) {
        return ResponseEntity.badRequest().body(ApiErrorResponse.of(
                "INVALID_REQUEST",
                "The quote request contains an invalid value."
        ));
    }

    @ExceptionHandler(InvalidQuoteSubmissionException.class)
    ResponseEntity<ApiErrorResponse> invalidSubmission(InvalidQuoteSubmissionException exception) {
        HttpStatus status = exception.getCode().equals("IDEMPOTENCY_KEY_REUSED")
                ? HttpStatus.CONFLICT
                : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(ApiErrorResponse.of(
                exception.getCode(),
                exception.getMessage()
        ));
    }

    @ExceptionHandler(InvalidAuthenticationException.class)
    ResponseEntity<ApiErrorResponse> invalidAuthentication(InvalidAuthenticationException exception) {
        HttpStatus status = exception.getCode().equals("AUTH_REQUIRED")
                || exception.getCode().equals("INVALID_AUTH_TOKEN")
                || exception.getCode().equals("REAUTHENTICATION_REQUIRED")
                ? HttpStatus.UNAUTHORIZED
                : HttpStatus.CONFLICT;
        return ResponseEntity.status(status).body(ApiErrorResponse.of(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(ProfileNotFoundException.class)
    ResponseEntity<ApiErrorResponse> profileNotFound(ProfileNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of("PROFILE_NOT_FOUND", exception.getMessage()));
    }
}
