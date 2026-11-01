package com.campustrade.android.ui.product

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.campustrade.android.databinding.ActivityProductDetailBinding
import com.campustrade.android.model.Product
import com.campustrade.android.model.enums.ProductStatus
import com.campustrade.android.ui.product.adapter.ProductImageAdapter
import com.campustrade.android.util.MemoryOptimizer
import com.campustrade.android.util.observe
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.Locale

@AndroidEntryPoint
class ProductDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductDetailBinding
    private val viewModel: ProductViewModel by viewModels()
    private lateinit var productImageAdapter: ProductImageAdapter
    private var productId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        productId = intent.getLongExtra(EXTRA_PRODUCT_ID, 0)
        if (productId == 0L) {
            finish()
            return
        }

        setupUI()
        setupObservers()
        loadProductDetail()
    }

    private fun setupUI() {
        // 设置图片轮播适配器
        productImageAdapter = ProductImageAdapter(emptyList())
        binding.viewPagerImages.apply {
            adapter = productImageAdapter
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    updateImageIndicators(position)
                }
            })
        }

        // 联系卖家按钮
        binding.btnContactSeller.setOnClickListener {
            contactSeller()
        }

        // 收藏按钮
        binding.btnFavorite.setOnClickListener {
            toggleFavorite()
        }

        // 立即购买按钮
        binding.btnBuyNow.setOnClickListener {
            buyNow()
        }
    }

    private fun setupObservers() {
        viewModel.uiState.observe(this) { state ->
            // 处理加载状态
            if (state.isLoading) {
                showLoading()
            } else {
                hideLoading()
            }

            // 处理错误信息
            state.error?.let { error ->
                showError(error)
                viewModel.clearError()
            }

            // 处理商品详情
            state.selectedProduct?.let { product ->
                showProductDetail(product)
            }
        }
    }

    private fun loadProductDetail() {
        viewModel.loadProductDetail(productId)
    }

    private fun showProductDetail(product: Product) {
        // 设置商品标题
        binding.tvProductTitle.text = product.title

        // 设置商品价格
        binding.tvProductPrice.text = formatPrice(product.price)

        // 设置商品分类
        binding.tvProductCategory.text = product.category.displayName

        // 设置商品状态
        binding.tvProductStatus.text = product.status.displayName

        // 设置状态背景颜色
        val statusColor = when (product.status) {
            ProductStatus.ON_SALE -> android.graphics.Color.parseColor("#4CAF50")
            ProductStatus.SOLD -> android.graphics.Color.parseColor("#F44336")
            ProductStatus.OFF_SALE, ProductStatus.DELETED -> android.graphics.Color.parseColor("#9E9E9E")
        }
        binding.tvProductStatus.setBackgroundColor(statusColor)

        // 设置浏览量和点赞数
        binding.tvViewCount.text = "${product.viewCount} 浏览"
        binding.tvLikeCount.text = "${product.likeCount} 点赞"

        // 设置商品描述
        binding.tvProductDescription.text = product.description

        // 设置卖家信息
        product.seller?.let { seller ->
            binding.tvSellerName.text = seller.username
            binding.tvSellerSchool.text = seller.school ?: "未知学校"
            
            // 加载卖家头像（使用内存优化版本）
            if (!seller.avatar.isNullOrEmpty()) {
                MemoryOptimizer.loadUserAvatar(this, seller.avatar, binding.ivSellerAvatar)
            }
        }

        // 设置交易信息
        binding.tvLocation.text = product.location ?: "面议"
        binding.tvContactInfo.text = product.contactInfo ?: "请通过平台联系"

        // 设置商品图片
        if (product.images.isNotEmpty()) {
            productImageAdapter.updateImages(product.images)
            setupImageIndicators(product.images.size)
        } else {
            // 如果没有图片，显示默认图片
            productImageAdapter.updateImages(listOf("default"))
            setupImageIndicators(1)
        }

        // 根据商品状态更新按钮状态
        updateButtonState(product.status)
    }

    private fun setupImageIndicators(imageCount: Int) {
        binding.layoutIndicators.removeAllViews()
        
        if (imageCount <= 1) {
            binding.layoutIndicators.visibility = View.GONE
            return
        }
        
        binding.layoutIndicators.visibility = View.VISIBLE
        
        for (i in 0 until imageCount) {
            val indicator = View(this).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width),
                    resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_height)
                ).apply {
                    setMargins(4, 0, 4, 0)
                }
                background = resources.getDrawable(android.R.drawable.presence_invisible, theme)
            }
            binding.layoutIndicators.addView(indicator)
        }
        
        updateImageIndicators(0)
    }

    private fun updateImageIndicators(currentPosition: Int) {
        for (i in 0 until binding.layoutIndicators.childCount) {
            val indicator = binding.layoutIndicators.getChildAt(i)
            if (i == currentPosition) {
                indicator.background = resources.getDrawable(android.R.drawable.presence_online, theme)
            } else {
                indicator.background = resources.getDrawable(android.R.drawable.presence_invisible, theme)
            }
        }
    }

    private fun updateButtonState(status: ProductStatus) {
        when (status) {
            ProductStatus.ON_SALE -> {
                binding.btnBuyNow.isEnabled = true
                binding.btnBuyNow.text = "立即购买"
            }
            ProductStatus.SOLD -> {
                binding.btnBuyNow.isEnabled = false
                binding.btnBuyNow.text = "已售出"
            }
            ProductStatus.OFF_SALE, ProductStatus.DELETED -> {
                binding.btnBuyNow.isEnabled = false
                binding.btnBuyNow.text = "已下架"
            }
        }
    }

    private fun formatPrice(price: java.math.BigDecimal): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale.CHINA)
        return formatter.format(price)
    }

    private fun contactSeller() {
        // 跳转到聊天页面
        Toast.makeText(this, "联系卖家功能开发中...", Toast.LENGTH_SHORT).show()
    }

    private fun toggleFavorite() {
        // 收藏/取消收藏功能
        Toast.makeText(this, "收藏功能开发中...", Toast.LENGTH_SHORT).show()
    }

    private fun buyNow() {
        // 跳转到购买页面
        Toast.makeText(this, "购买功能开发中...", Toast.LENGTH_SHORT).show()
    }

    private fun showLoading() {
        // 显示加载状态
        binding.viewPagerImages.visibility = View.GONE
        // 可以添加加载动画
    }

    private fun hideLoading() {
        binding.viewPagerImages.visibility = View.VISIBLE
    }

    private fun showError(error: String) {
        Toast.makeText(this, error, Toast.LENGTH_LONG).show()
    }

    companion object {
        private const val EXTRA_PRODUCT_ID = "extra_product_id"
        
        fun start(activity: AppCompatActivity, productId: Long) {
            val intent = android.content.Intent(activity, ProductDetailActivity::class.java)
            intent.putExtra(EXTRA_PRODUCT_ID, productId)
            activity.startActivity(intent)
        }
    }
}