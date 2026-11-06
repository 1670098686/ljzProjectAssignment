package com.finance.exception;

/**
 * Thrown when business validation fails. Controllers can convert it into a 4xx response.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
