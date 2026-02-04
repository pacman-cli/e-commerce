package com.ecommerce.user.exception;

/**
 * Exception thrown when a user cannot be found.
 */
public class UserNotFoundException extends RuntimeException {
    
    public UserNotFoundException(String message) {
        super(message);
    }
    
    public UserNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public static UserNotFoundException withId(String userId) {
        return new UserNotFoundException("User not found with ID: " + userId);
    }
    
    public static UserNotFoundException withEmail(String email) {
        return new UserNotFoundException("User not found with email: " + email);
    }
}
