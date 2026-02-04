package com.ecommerce.user.exception;

/**
 * Exception thrown when login credentials are invalid.
 */
public class InvalidCredentialsException extends RuntimeException {
    
    public InvalidCredentialsException(String message) {
        super(message);
    }
    
    public InvalidCredentialsException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public static InvalidCredentialsException generic() {
        return new InvalidCredentialsException("Invalid email or password");
    }
}
