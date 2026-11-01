package com.campustrade.android.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.widget.doAfterTextChanged
import com.campustrade.android.databinding.ActivityLoginBinding
import com.campustrade.android.ui.MainActivity
import com.campustrade.android.ui.admin.AdminMainActivity
import com.campustrade.android.ui.base.BaseActivity
import com.campustrade.android.util.StartupOptimizer
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginActivity : BaseActivity() {
    
    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 记录Activity创建时间
        StartupOptimizer.monitor.onFirstActivityCreate()
        
        // 使用启动优化工具
        StartupOptimizer.optimizeColdStart(
            activity = this,
            criticalTask = {
                binding = ActivityLoginBinding.inflate(layoutInflater)
                setContentView(binding.root)
                setupCriticalUI()
            },
            nonCriticalTask = {
                setupNonCriticalUI()
                setupObservers()
            }
        )
    }
    
    /**
     * 设置关键UI（立即执行）
     * 只设置用户立即需要交互的UI组件
     */
    private fun setupCriticalUI() {
        // 设置登录按钮点击事件（用户立即需要）
        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString()
            val password = binding.etPassword.text.toString()
            
            // 根据用户名判断是普通用户还是管理员
            if (isAdminUsername(username)) {
                viewModel.adminLogin(username, password)
            } else {
                viewModel.login(username, password)
            }
        }
        
        // 设置注册跳转（用户立即需要）
        binding.tvRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
        
        // 添加管理员登录提示
        binding.tvAdminLoginHint.visibility = android.view.View.VISIBLE
    }
    
    /**
     * 判断是否为管理员用户名
     * 管理员用户名通常以"admin"开头或包含特定标识
     */
    private fun isAdminUsername(username: String): Boolean {
        return username.startsWith("admin") || username.contains("admin") || 
               username.startsWith("super") || username.contains("super")
    }
    
    /**
     * 设置非关键UI（延迟执行）
     * 延迟设置不需要立即使用的UI组件
     */
    private fun setupNonCriticalUI() {
        // 设置输入框监听（可以延迟设置）
        binding.etUsername.doAfterTextChanged {
            viewModel.onUsernameChanged(it.toString())
            validateInputs()
        }
        
        binding.etPassword.doAfterTextChanged {
            viewModel.onPasswordChanged(it.toString())
            validateInputs()
        }
        
        // 其他非关键UI初始化
        // 如动画效果、复杂的布局逻辑等
    }
    
    private fun setupObservers() {
        viewModel.uiState.observe(this) { state ->
            // 更新加载状态
            binding.progressBar.visibility = if (state.isLoading) android.view.View.VISIBLE else android.view.View.GONE
            binding.btnLogin.isEnabled = !state.isLoading
            
            // 显示错误信息
            if (state.error != null) {
                binding.tvError.text = state.error
                binding.tvError.visibility = android.view.View.VISIBLE
                // 3秒后自动清除错误信息
                binding.tvError.postDelayed({
                    viewModel.clearError()
                }, 3000)
            } else {
                binding.tvError.visibility = android.view.View.GONE
            }
            
            // 登录成功处理
            if (state.isSuccess) {
                val message = if (state.isAdmin) "管理员登录成功" else "登录成功"
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                
                // 根据用户类型跳转到不同的主页面
                if (state.isAdmin) {
                    navigateToAdminMainActivity()
                } else {
                    navigateToMainActivity()
                }
            }
        }
    }
    
    private fun validateInputs() {
        val username = binding.etUsername.text?.toString()?.trim() ?: ""
        val password = binding.etPassword.text?.toString() ?: ""
        
        val isValid = username.isNotEmpty() && password.isNotEmpty() && password.length >= 6
        binding.btnLogin.isEnabled = isValid
        
        // 根据用户名显示管理员登录提示
        if (isAdminUsername(username)) {
            binding.tvAdminLoginHint.text = "检测到管理员账号，将使用管理员登录"
        } else {
            binding.tvAdminLoginHint.text = "管理员请使用以'admin'开头的用户名登录"
        }
    }
    
    override fun onResume() {
        super.onResume()
        
        // 记录Activity可见时间（用于启动性能监控）
        StartupOptimizer.monitor.onFirstActivityVisible()
        
        // 检查登录状态，如果已登录则跳转到主页面
        if (viewModel.isUserLoggedIn()) {
            // 检查是否为管理员
            if (viewModel.isAdmin()) {
                navigateToAdminMainActivity()
            } else {
                navigateToMainActivity()
            }
        }
    }
    
    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
    
    private fun navigateToAdminMainActivity() {
        val intent = Intent(this, AdminMainActivity::class.java)
        startActivity(intent)
        finish()
    }
}