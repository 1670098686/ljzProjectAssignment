package com.campustrade.android.model

import com.google.gson.annotations.SerializedName

/**
 * 通用 API 响应结构，与后端 `ApiResponse` 字段保持一致。
 */
data class ApiResponse<T>(
    @SerializedName("success")
    val success: Boolean = false,
    @SerializedName("code")
    val code: Int = 0,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("errorCode")
    val errorCode: String? = null,
    @SerializedName("timestamp")
    val timestamp: String? = null,
    @SerializedName("data")
    val data: T? = null
) {
    val isSuccess: Boolean
        get() = success || code in 200..299

    companion object {
        fun <T> ok(data: T? = null, message: String? = null): ApiResponse<T> =
            ApiResponse(success = true, code = 200, message = message ?: "OK", data = data)

        fun error(code: Int, message: String, errorCode: String? = null): ApiResponse<Unit> =
            ApiResponse(success = false, code = code, message = message, errorCode = errorCode)
    }
}