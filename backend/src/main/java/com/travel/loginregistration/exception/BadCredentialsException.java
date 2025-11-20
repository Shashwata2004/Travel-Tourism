package com.travel.loginregistration.exception;

// Exception thrown when user provides invalid login credentials
public class BadCredentialsException extends RuntimeException {
    public BadCredentialsException(String message) {
        super(message);
    }
}
