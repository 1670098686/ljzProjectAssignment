package com.campus.trade.model.enums;

public enum EmailTemplateType {
    EMAIL_VERIFICATION("email/verification.txt", "校园二手交易 - 邮箱验证"),
    PASSWORD_RESET("email/password-reset.txt", "校园二手交易 - 密码重置验证码"),
    PAYMENT_SUCCESS_BUYER("email/payment-success-buyer.txt", "订单支付成功 - {{orderNo}}"),
    PAYMENT_SUCCESS_SELLER("email/payment-success-seller.txt", "买家已支付 - {{orderNo}}"),
    ORDER_STATUS_UPDATE("email/order-status-update.txt", "订单状态更新 - {{orderNo}}");

    private final String templatePath;
    private final String subjectTemplate;

    EmailTemplateType(String templatePath, String subjectTemplate) {
        this.templatePath = templatePath;
        this.subjectTemplate = subjectTemplate;
    }

    public String getTemplatePath() {
        return templatePath;
    }

    public String getSubjectTemplate() {
        return subjectTemplate;
    }
}
