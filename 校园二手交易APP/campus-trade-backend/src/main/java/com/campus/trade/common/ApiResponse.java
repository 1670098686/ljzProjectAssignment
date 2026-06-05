package com.campus.trade.common;

import java.time.Instant;

public class ApiResponse<T> {

    private int code;
    private String message;
    private String errorCode;
    private Instant timestamp;
    private T data;

    public ApiResponse() {
    }

    public ApiResponse(int code, String message, String errorCode, Instant timestamp, T data) {
        this.code = code;
        this.message = message;
        this.errorCode = errorCode;
        this.timestamp = timestamp;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "OK", null, Instant.now(), data);
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(200, message, null, Instant.now(), data);
    }

    public static ApiResponse<Void> success() {
        return success(null);
    }

    public static ApiResponse<Void> success(String message) {
        return success(null, message);
    }

    // 专门用于返回字符串消息的方法
    public static ApiResponse<String> successMessage(String message) {
        return success(message, message);
    }

    public static ApiResponse<Void> failure(int code, String message, String errorCode) {
        return new ApiResponse<>(code, message, errorCode, Instant.now(), null);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
