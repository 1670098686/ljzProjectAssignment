package com.campus.trade.exception;

import com.campus.trade.common.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public GlobalExceptionHandler() {
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        log.warn("business error: {}", ex.getMessage());
        return buildResponse(HttpStatus.valueOf(errorCode.getHttpStatus()), ex.getMessage(), errorCode.getCode());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("参数校验失败");
        log.warn("validation error: {}", message);
        return buildResponse(HttpStatus.BAD_REQUEST, message, ErrorCode.BUSINESS_ERROR.getCode());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("constraint violation: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), ErrorCode.BUSINESS_ERROR.getCode());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("access denied");
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage(), ErrorCode.ADMIN_PERMISSION_DENIED.getCode());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        log.error("unexpected error", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), ErrorCode.INTERNAL_ERROR.getCode());
    }

    private ResponseEntity<ApiResponse<Void>> buildResponse(HttpStatus status, String message, String errorCode) {
        ApiResponse<Void> response = ApiResponse.failure(status.value(), message, errorCode);
        return ResponseEntity.status(status).body(response);
    }
}
