package com.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ApiResponse", description = "统一的API响应包装结构")
public class ApiResponse<T> {

    @Schema(description = "业务状态码，200表示成功", example = "200")
    private int code;

    @Schema(description = "提示信息或错误描述", example = "success")
    private String message;

    @Schema(description = "真实的数据载荷，不同接口返回不同结构")
    private T data;

    @Schema(description = "请求是否成功", example = "true")
    private boolean success;

    // Constructor
    public ApiResponse() {
        // Default constructor
    }

    public ApiResponse(int code, String message, T data, boolean success) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.success = success;
    }

    // Getter methods
    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public boolean isSuccess() {
        return success;
    }

    // Setter methods
    public void setCode(int code) {
        this.code = code;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setData(T data) {
        this.data = data;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    // Builder pattern
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static class Builder<T> {
        private int code;
        private String message;
        private T data;
        private boolean success;

        public Builder<T> code(int code) {
            this.code = code;
            return this;
        }

        public Builder<T> message(String message) {
            this.message = message;
            return this;
        }

        public Builder<T> data(T data) {
            this.data = data;
            return this;
        }

        public Builder<T> success(boolean success) {
            this.success = success;
            return this;
        }

        public ApiResponse<T> build() {
            return new ApiResponse<>(code, message, data, success);
        }
    }

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .code(200)
                .message("success")
                .data(data)
                .success(true)
                .build();
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .code(200)
                .message(message)
                .data(data)
                .success(true)
                .build();
    }

    public static ApiResponse<Void> successMessage(String message) {
        return ApiResponse.<Void>builder()
                .code(200)
                .message(message)
                .success(true)
                .build();
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .success(false)
                .build();
    }
}
