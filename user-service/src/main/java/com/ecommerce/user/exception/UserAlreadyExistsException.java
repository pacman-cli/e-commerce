package com.ecommerce.user.exception;

/**
 * Exception thrown when attempting to create a user that already exists.
 */
public class UserAlreadyExistsException extends RuntimeException {
    
    public UserAlreadyExistsException(String message) {
        super(message);
    }
    
    public UserAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public static UserAlreadyExistsException withEmail(String email) {
        return new UserAlreadyExistsException("User already exists with email: " + email);
    }
}
