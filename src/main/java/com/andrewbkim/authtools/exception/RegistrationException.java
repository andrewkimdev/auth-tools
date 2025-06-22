package com.andrewbkim.authtools.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;


/**
 * Custom exception for handling registration-specific business logic errors.
 * This allows us to map them to specific HTTP status codes.
 */
@Getter
public class RegistrationException extends RuntimeException {

    private final HttpStatus status;

    public RegistrationException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}
