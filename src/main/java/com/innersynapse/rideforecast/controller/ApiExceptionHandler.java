package com.innersynapse.rideforecast.controller;

import com.innersynapse.rideforecast.dto.ApiErrorResponse;
import com.innersynapse.rideforecast.exception.InvalidQuoteSubmissionException;
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
        return ResponseEntity.badRequest().body(new ApiErrorResponse(
                "INVALID_QUOTE",
                "Check the highlighted quote details and try again.",
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
}
