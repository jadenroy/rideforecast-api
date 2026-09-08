package com.innersynapse.rideforecast.auth;

public class InvalidAuthenticationException extends RuntimeException {
    private final String code;

    public InvalidAuthenticationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
