package com.innersynapse.rideforecast.exception;

public class InvalidQuoteSubmissionException extends RuntimeException {
    private final String code;

    public InvalidQuoteSubmissionException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
