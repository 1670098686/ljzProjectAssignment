package com.campustrade.android

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.campustrade.android.databinding.ActivityMainBinding
import com.campustrade.android.ui.auth.LoginActivity
import com.campustrade.android.ui.product.ProductListActivity
import com.campustrade.android.util.SharedPreferencesHelper

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    
    private lateinit var sharedPreferencesHelper: SharedPreferencesHelper
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 手动创建SharedPreferencesHelper实例
        sharedPreferencesHelper = SharedPreferencesHelper(this)
        
        // 检查用户登录状态
        if (!sharedPreferencesHelper.isLoggedIn()) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
    }
    
    private fun setupUI() {
        // 初始化界面组件
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    // 切换到首页
                    true
                }
                R.id.navigation_products -> {
                    // 切换到商品页面
                    ProductListActivity.start(this)
                    true
                }
                R.id.navigation_messages -> {
                    // 切换到消息页面
                    true
                }
                R.id.navigation_profile -> {
                    // 切换到个人中心
                    com.campustrade.android.ui.profile.ProfileActivity.start(this)
                    true
                }
                else -> false
            }
        }
    }
}