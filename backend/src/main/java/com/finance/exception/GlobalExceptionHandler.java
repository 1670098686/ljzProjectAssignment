package com.finance.exception;

import com.finance.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * 全局异常处理器
 * 统一处理所有异常，返回标准化的ApiResponse格式
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理业务异常
     * 返回400 Bad Request状态码
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e, WebRequest request) {
        log.error("业务异常: {}, 请求路径: {}", e.getMessage(), request.getDescription(false));
        ApiResponse<Void> response = ApiResponse.error(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 处理参数验证异常
     * 返回400 Bad Request状态码
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e, WebRequest request) {
        BindingResult result = e.getBindingResult();
        List<String> errorMessages = new ArrayList<>();
        
        for (FieldError error : result.getFieldErrors()) {
            errorMessages.add(error.getField() + ": " + error.getDefaultMessage());
        }
        
        String message = String.join(", ", errorMessages);
        log.error("参数验证失败: {}, 请求路径: {}", message, request.getDescription(false));
        
        ApiResponse<Void> response = ApiResponse.error(HttpStatus.BAD_REQUEST.value(), message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 处理权限不足异常
     * 返回403 Forbidden状态码
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException e, WebRequest request) {
        log.error("权限不足: {}, 请求路径: {}", e.getMessage(), request.getDescription(false));
        ApiResponse<Void> response = ApiResponse.error(HttpStatus.FORBIDDEN.value(), "权限不足，无法访问该资源");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * 处理所有其他异常
     * 返回500 Internal Server Error状态码
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAllOtherExceptions(Exception e, WebRequest request) {
        log.error("服务器内部错误: {}, 请求路径: {}", e.getMessage(), request.getDescription(false), e);
        ApiResponse<Void> response = ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "服务器内部错误，请稍后重试");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}