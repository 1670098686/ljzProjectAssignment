package com.finance.exception;

/**
 * Indicates that a database entity required by a business operation does not exist.
 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
