package com.campustrade.android.model

data class User(
    val id: Long = 0,
    val username: String = "",
    val password: String? = null,
    val email: String = "",
    val phone: String? = null,
    val realName: String? = null,
    val school: String? = null,
    val avatar: String? = null,
    val contactInfo: String? = null,
    val role: String = "STUDENT",
    val status: String = "ACTIVE",
    val emailVerified: Boolean = false,
    val lastLogin: String? = null,
    val createTime: String? = null,
    val updateTime: String? = null
)