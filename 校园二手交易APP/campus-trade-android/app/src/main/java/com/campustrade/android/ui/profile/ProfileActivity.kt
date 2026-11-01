package com.campustrade.android.ui.profile

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.campustrade.android.databinding.ActivityProfileBinding
import com.campustrade.android.repository.ProfileRepository
import com.campustrade.android.ui.auth.LoginActivity
import com.campustrade.android.ui.order.OrderListActivity
import com.campustrade.android.ui.product.ProductListActivity
import com.campustrade.android.ui.profile.model.ProfileData
import com.campustrade.android.ui.profile.model.Statistics
import com.campustrade.android.ui.profile.model.UserInfo
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var viewModel: ProfileViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 手动创建ViewModel实例
        val profileRepository = createProfileRepository()
        viewModel = ProfileViewModel(profileRepository)

        setupUI()
        setupObservers()
        loadProfileData()
    }

    private fun setupUI() {
        setupToolbar()
        setupClickListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupClickListeners() {
        // 编辑个人资料
        binding.btnEditProfile.setOnClickListener {
            // 跳转到编辑资料页面
            Toast.makeText(this, "编辑个人资料", Toast.LENGTH_SHORT).show()
        }

        // 我的发布
        binding.layoutMyProducts.setOnClickListener {
            // 跳转到我的发布页面
            ProductListActivity.start(this, ProductListActivity.MODE_MY_PRODUCTS)
        }

        // 我的收藏
        binding.layoutFavorites.setOnClickListener {
            // 跳转到我的收藏页面
            ProductListActivity.start(this, ProductListActivity.MODE_FAVORITES)
        }

        // 订单历史
        binding.layoutOrderHistory.setOnClickListener {
            // 跳转到订单历史页面
            OrderListActivity.start(this)
        }

        // 设置
        binding.layoutSettings.setOnClickListener {
            Toast.makeText(this, "设置", Toast.LENGTH_SHORT).show()
        }

        // 关于我们
        binding.layoutAbout.setOnClickListener {
            Toast.makeText(this, "关于我们", Toast.LENGTH_SHORT).show()
        }

        // 退出登录
        binding.layoutLogout.setOnClickListener {
            showLogoutConfirmation()
        }

        // 重试按钮
        binding.btnRetry.setOnClickListener {
            loadProfileData()
        }
    }

    private fun loadProfileData() {
        // 这里应该从用户管理器中获取当前用户ID
        val currentUserId = 1L
        viewModel.loadProfileData(currentUserId)
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.progressBar.visibility = if (state.isLoading) android.view.View.VISIBLE else android.view.View.GONE
                binding.errorState.visibility = if (state.error != null && !state.isLoading) android.view.View.VISIBLE else android.view.View.GONE

                if (!state.isLoading && state.error == null) {
                    state.profileData?.let { profileData ->
                        updateUI(profileData)
                    }
                }

                if (state.error != null) {
                    binding.tvErrorMessage.text = state.error
                }
            }
        }

        lifecycleScope.launch {
            viewModel.operationState.collect { state ->
                // TODO: 修复OperationState引用问题
                // when (state) {
                //     is ProfileViewModel.OperationState.Loading -> {
                //         // 显示加载状态
                //     }
                //     is ProfileViewModel.OperationState.Success -> {
                //         Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                //         if (state.message == "退出成功") {
                //             navigateToLogin()
                //         }
                //         viewModel.resetOperationState()
                //     }
                //     is ProfileViewModel.OperationState.Error -> {
                //         Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                //         viewModel.resetOperationState()
                //     }
                //     else -> {
                //         // 空闲状态
                //     }
                // }
            }
        }
    }

    private fun updateUI(profileData: com.campustrade.android.ui.profile.model.ProfileData) {
        // 更新用户信息
        val userInfo = profileData.userInfo
        binding.tvUsername.text = userInfo.username
        binding.tvSchool.text = userInfo.school ?: "未设置学校"
        binding.tvRole.text = when (userInfo.role) {
            "STUDENT" -> "学生"
            "TEACHER" -> "教师"
            "STAFF" -> "教职工"
            else -> "用户"
        }

        // 更新统计信息
        val statistics = profileData.statistics
        binding.tvPublishedCount.text = statistics.publishedCount.toString()
        binding.tvSoldCount.text = statistics.soldCount.toString()
        binding.tvPurchasedCount.text = statistics.purchasedCount.toString()
        binding.tvFavoriteCount.text = statistics.favoriteCount.toString()

        // 这里应该加载用户头像
        // Glide.with(this).load(userInfo.avatar).into(binding.ivAvatar)
    }

    private fun showLogoutConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("确认退出")
            .setMessage("确定要退出登录吗？")
            .setPositiveButton("确定") { _, _ ->
                viewModel.logout()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    companion object {
        fun start(activity: android.app.Activity) {
            val intent = Intent(activity, ProfileActivity::class.java)
            activity.startActivity(intent)
        }
    }

    private fun createProfileRepository(): ProfileRepository {
        // 创建一个简单的ProfileRepository实例，不使用Hilt依赖注入
        return object : ProfileRepository() {
            override suspend fun getProfileData(userId: Long): ProfileData {
                // 实现具体逻辑
                return ProfileData(
                    userInfo = UserInfo(
                        id = userId,
                        username = "test_user",
                        email = "test@example.com",
                        phone = "",
                        realName = "",
                        school = "",
                        avatar = null,
                        contactInfo = "",
                        role = "STUDENT",
                        status = "ACTIVE",
                        emailVerified = true,
                        lastLogin = ""
                    ),
                    statistics = Statistics(
                        publishedCount = 0,
                        soldCount = 0,
                        purchasedCount = 0,
                        favoriteCount = 0,
                        totalRevenue = 0.0,
                        totalSpent = 0.0
                    ),
                    favoriteProducts = emptyList(),
                    publishedProducts = emptyList(),
                    orderHistory = emptyList()
                )
            }
        }
    }
}