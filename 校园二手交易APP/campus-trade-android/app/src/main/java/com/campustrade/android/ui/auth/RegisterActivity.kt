package com.campustrade.android.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.campustrade.android.databinding.ActivityRegisterBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: RegisterViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        setupObservers()
    }
    
    private fun setupUI() {
        // 返回按钮
        binding.btnBack.setOnClickListener {
            finish()
        }
        
        // 输入框监听
        binding.etUsername.doAfterTextChanged { validateInputs() }
        binding.etPassword.doAfterTextChanged { validateInputs() }
        binding.etConfirmPassword.doAfterTextChanged { validateInputs() }
        binding.etEmail.doAfterTextChanged { validateInputs() }
        
        // 注册按钮点击事件
        binding.btnRegister.setOnClickListener {
            val username = binding.etUsername.text?.toString()?.trim() ?: ""
            val password = binding.etPassword.text?.toString() ?: ""
            val email = binding.etEmail.text?.toString()?.trim() ?: ""
            val phone = binding.etPhone.text?.toString()?.trim()
            val realName = binding.etRealName.text?.toString()?.trim()
            val school = binding.etSchool.text?.toString()?.trim()
            
            // 验证密码一致性
            val confirmPassword = binding.etConfirmPassword.text?.toString() ?: ""
            if (password != confirmPassword) {
                binding.tvError.text = "两次输入的密码不一致"
                binding.tvError.visibility = android.view.View.VISIBLE
                return@setOnClickListener
            }
            
            viewModel.register(username, password, email, phone, realName, school)
        }
        
        // 登录链接点击事件
        binding.tvLogin.setOnClickListener {
            finish()
        }
    }
    
    private fun setupObservers() {
        viewModel.uiState.observe(this) { state ->
            // 更新加载状态
            binding.progressBar.visibility = if (state.isLoading) android.view.View.VISIBLE else android.view.View.GONE
            binding.btnRegister.isEnabled = !state.isLoading
            
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
            
            // 注册成功处理
            if (state.isSuccess) {
                Toast.makeText(this, "注册成功，请登录", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    
    private fun validateInputs() {
        val username = binding.etUsername.text?.toString()?.trim() ?: ""
        val password = binding.etPassword.text?.toString() ?: ""
        val email = binding.etEmail.text?.toString()?.trim() ?: ""
        
        val isValid = username.isNotEmpty() && 
                     password.isNotEmpty() && password.length >= 6 &&
                     email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
        
        binding.btnRegister.isEnabled = isValid
    }
}