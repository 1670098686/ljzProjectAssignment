package com.campustrade.android.model.enums

/**
 * 审核状态枚举。
 */
enum class AuditStatus(val displayName: String) {
    PENDING("待审核"),
    APPROVED("已通过"),
    REJECTED("已驳回")
}
