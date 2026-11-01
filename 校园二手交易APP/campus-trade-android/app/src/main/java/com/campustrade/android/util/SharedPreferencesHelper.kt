package com.campustrade.android.util

import android.content.Context
import android.content.SharedPreferences
import com.campustrade.android.model.User
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPreferencesHelper @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val PREFS_NAME = "campus_trade_prefs"
        private const val KEY_USER_TOKEN = "user_token"
        private const val KEY_USER_INFO = "user_info"
        private const val KEY_ADMIN_INFO = "admin_info"
        private const val KEY_USER_TYPE = "user_type"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }
    
    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    private val gson = Gson()
    
    fun saveUserToken(token: String) {
        sharedPreferences.edit().putString(KEY_USER_TOKEN, token).apply()
    }
    
    fun getUserToken(): String? {
        return sharedPreferences.getString(KEY_USER_TOKEN, null)
    }
    
    fun saveUserInfo(user: User) {
        val userJson = gson.toJson(user)
        sharedPreferences.edit().putString(KEY_USER_INFO, userJson).apply()
    }
    
    fun getUserInfo(): User? {
        val userJson = sharedPreferences.getString(KEY_USER_INFO, null)
        return if (userJson != null) {
            gson.fromJson(userJson, User::class.java)
        } else {
            null
        }
    }
    
    fun saveAdminInfo(admin: User) {
        val adminJson = gson.toJson(admin)
        sharedPreferences.edit().putString(KEY_ADMIN_INFO, adminJson).apply()
    }
    
    fun getAdminInfo(): User? {
        val adminJson = sharedPreferences.getString(KEY_ADMIN_INFO, null)
        return if (adminJson != null) {
            gson.fromJson(adminJson, User::class.java)
        } else {
            null
        }
    }
    
    fun setUserType(userType: String) {
        sharedPreferences.edit().putString(KEY_USER_TYPE, userType).apply()
    }
    
    fun getUserType(): String {
        return sharedPreferences.getString(KEY_USER_TYPE, "user") ?: "user"
    }
    
    fun setLoggedIn(isLoggedIn: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_IS_LOGGED_IN, isLoggedIn).apply()
    }
    
    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }
    
    fun clearAll() {
        sharedPreferences.edit().clear().apply()
    }
}