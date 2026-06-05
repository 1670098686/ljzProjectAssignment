package com.campus.trade.exception;

public enum ErrorCode {

    USER_ALREADY_EXISTS(409, "USER_ALREADY_EXISTS", "用户已存在"),
    EMAIL_NOT_VERIFIED(401, "EMAIL_NOT_VERIFIED", "邮箱未验证"),
    INVALID_CREDENTIALS(401, "INVALID_CREDENTIALS", "用户名或密码错误"),
    USER_NOT_FOUND(404, "USER_NOT_FOUND", "用户不存在"),
    VERIFICATION_CODE_INVALID(400, "VERIFICATION_CODE_INVALID", "验证码无效"),
    VERIFICATION_CODE_EXPIRED(400, "VERIFICATION_CODE_EXPIRED", "验证码已过期"),
    DELETE_REQUEST_PENDING(400, "DELETE_REQUEST_PENDING", "账号注销处理中"),
    ACCOUNT_DISABLED(403, "ACCOUNT_DISABLED", "账号已停用"),
    ACCOUNT_DELETED(403, "ACCOUNT_DELETED", "账号已注销"),
    PRODUCT_NOT_FOUND(404, "PRODUCT_NOT_FOUND", "商品不存在"),
    PRODUCT_STATUS_INVALID(400, "PRODUCT_STATUS_INVALID", "商品状态无效"),
    ORDER_NOT_FOUND(404, "ORDER_NOT_FOUND", "订单不存在"),
    ORDER_STATUS_INVALID(400, "ORDER_STATUS_INVALID", "订单状态无效"),
    PAYMENT_NOT_FOUND(404, "PAYMENT_NOT_FOUND", "支付记录不存在"),
    PAYMENT_STATUS_INVALID(400, "PAYMENT_STATUS_INVALID", "支付状态无效"),
    CART_ITEM_NOT_FOUND(404, "CART_ITEM_NOT_FOUND", "购物车条目不存在"),
    MESSAGE_FORBIDDEN(403, "MESSAGE_FORBIDDEN", "无权访问该消息"),
    IDEMPOTENCY_KEY_REQUIRED(400, "IDEMPOTENCY_KEY_REQUIRED", "缺少幂等键"),
    IDEMPOTENCY_REQUEST_IN_PROGRESS(409, "IDEMPOTENCY_REQUEST_IN_PROGRESS", "请求处理中，请勿重复提交"),
    IDEMPOTENCY_KEY_CONFLICT(409, "IDEMPOTENCY_KEY_CONFLICT", "幂等键已被使用"),
    IDEMPOTENCY_KEY_REPLAYED(409, "IDEMPOTENCY_KEY_REPLAYED", "幂等键对应请求已处理"),
    ADMIN_PERMISSION_DENIED(403, "ADMIN_PERMISSION_DENIED", "管理员权限不足"),
    FILE_TYPE_NOT_ALLOWED(400, "FILE_TYPE_NOT_ALLOWED", "文件类型不允许"),
    FILE_TOO_LARGE(400, "FILE_TOO_LARGE", "文件过大"),
    FILE_SCAN_FAILED(500, "FILE_SCAN_FAILED", "文件扫描失败"),
    FILE_VIRUS_DETECTED(400, "FILE_VIRUS_DETECTED", "检测到恶意文件"),
    PRODUCT_CATEGORY_INVALID(400, "PRODUCT_CATEGORY_INVALID", "商品分类无效"),
    INVALID_PARAM(400, "INVALID_PARAM", "参数不合法"),
    RESOURCE_NOT_FOUND(404, "RESOURCE_NOT_FOUND", "资源不存在"),
    BUSINESS_ERROR(400, "BUSINESS_ERROR", "业务异常"),
    INTERNAL_ERROR(500, "INTERNAL_ERROR", "系统异常");

    private final int httpStatus;
    private final String code;
    private final String defaultMessage;

    ErrorCode(int httpStatus, String code, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
