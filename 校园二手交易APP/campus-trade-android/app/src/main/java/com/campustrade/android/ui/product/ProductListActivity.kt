package com.campustrade.android.ui.product

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.campustrade.android.databinding.ActivityProductListBinding
import com.campustrade.android.model.Product
import com.campustrade.android.ui.product.adapter.ProductAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProductListActivity : AppCompatActivity() {

    companion object {
        const val MODE_ALL = "all"
        const val MODE_MY_PRODUCTS = "my_products"
        const val MODE_FAVORITES = "favorites"
        private const val EXTRA_MODE = "extra_mode"

        fun start(activity: android.app.Activity, mode: String = MODE_ALL) {
            val intent = android.content.Intent(activity, ProductListActivity::class.java)
            intent.putExtra(EXTRA_MODE, mode)
            activity.startActivity(intent)
        }

        fun start(activity: AppCompatActivity) {
            val intent = android.content.Intent(activity, ProductListActivity::class.java)
            activity.startActivity(intent)
        }

        fun start(activity: AppCompatActivity, mode: String) {
            val intent = android.content.Intent(activity, ProductListActivity::class.java)
            intent.putExtra(EXTRA_MODE, mode)
            activity.startActivity(intent)
        }
    }

    private lateinit var binding: ActivityProductListBinding
    private val viewModel: ProductViewModel by viewModels()
    private lateinit var productAdapter: ProductAdapter
    private var currentPage = 0
    private var isLoadingMore = false
    private var currentMode = MODE_ALL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentMode = intent.getStringExtra(EXTRA_MODE) ?: MODE_ALL
        
        setupUI()
        setupObservers()
        loadProducts()
    }

    private fun setupUI() {
        // 根据模式设置标题
        when (currentMode) {
            MODE_MY_PRODUCTS -> {
                binding.toolbar.title = "我的发布"
                binding.fabAddProduct.visibility = View.VISIBLE
            }
            MODE_FAVORITES -> {
                binding.toolbar.title = "我的收藏"
                binding.fabAddProduct.visibility = View.GONE
            }
            else -> {
                binding.toolbar.title = "商品列表"
                binding.fabAddProduct.visibility = View.VISIBLE
            }
        }

        // 设置RecyclerView
        productAdapter = ProductAdapter { product ->
            // 点击商品跳转到详情页面
            ProductDetailActivity.start(this, product.id)
        }

        binding.rvProducts.apply {
            layoutManager = LinearLayoutManager(this@ProductListActivity)
            adapter = productAdapter
            setHasFixedSize(true)
        }

        // 搜索功能
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }

        // 筛选按钮
        binding.btnFilter.setOnClickListener {
            showFilterDialog()
        }

        // 发布按钮
        binding.fabAddProduct.setOnClickListener {
            // 跳转到发布商品页面
            ProductPublishActivity.start(this)
        }

        // 设置滚动监听，实现加载更多
        binding.rvProducts.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                if (!isLoadingMore && viewModel.uiState.value.hasMore) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                        && firstVisibleItemPosition >= 0
                        && totalItemCount >= 20) {
                        loadMoreProducts()
                    }
                }
            }
        })
    }

    private fun setupObservers() {
        viewModel.uiState.observe(this) { state ->
            // 处理加载状态
            if (state.isLoading) {
                if (currentPage == 0) {
                    showLoading()
                } else {
                    showLoadMoreLoading()
                }
            } else {
                hideLoading()
                hideLoadMoreLoading()
            }

            // 处理错误信息
            state.error?.let { error ->
                showError(error)
                viewModel.clearError()
            }

            // 处理商品数据
            if (state.products.isNotEmpty()) {
                showProducts(state.products)
            } else {
                showEmptyState()
            }
        }
    }

    private fun loadProducts(page: Int = 0, category: String? = null, keyword: String? = null) {
        currentPage = page
        when (currentMode) {
            MODE_MY_PRODUCTS -> viewModel.loadMyProducts(page)
            MODE_FAVORITES -> viewModel.loadFavoriteProducts(page)
            else -> viewModel.loadProducts(page, category, keyword)
        }
    }

    private fun loadMoreProducts() {
        if (!isLoadingMore && viewModel.uiState.value.hasMore) {
            isLoadingMore = true
            currentPage++
            viewModel.loadProducts(currentPage)
        }
    }

    private fun performSearch() {
        val keyword = binding.etSearch.text.toString().trim()
        if (keyword.isNotEmpty()) {
            currentPage = 0
            loadProducts(keyword = keyword)
        } else {
            loadProducts()
        }
    }

    private fun showFilterDialog() {
        // 实现筛选对话框
        Toast.makeText(this, "筛选功能开发中...", Toast.LENGTH_SHORT).show()
    }

    private fun showLoading() {
        binding.layoutEmpty.visibility = View.GONE
        binding.tvError.visibility = View.GONE
        binding.rvProducts.visibility = View.GONE
        // 可以添加加载动画
    }

    private fun hideLoading() {
        binding.rvProducts.visibility = View.VISIBLE
    }

    private fun showLoadMoreLoading() {
        binding.pbLoadMore.visibility = View.VISIBLE
        isLoadingMore = true
    }

    private fun hideLoadMoreLoading() {
        binding.pbLoadMore.visibility = View.GONE
        isLoadingMore = false
    }

    private fun showError(error: String) {
        binding.tvError.text = error
        binding.tvError.visibility = View.VISIBLE
        binding.rvProducts.visibility = View.GONE
        binding.layoutEmpty.visibility = View.GONE
    }

    private fun showProducts(products: List<Product>) {
        if (currentPage == 0) {
            productAdapter.submitList(products)
        } else {
            val currentList = productAdapter.currentList.toMutableList()
            currentList.addAll(products)
            productAdapter.submitList(currentList)
        }
        
        binding.tvError.visibility = View.GONE
        binding.layoutEmpty.visibility = View.GONE
        binding.rvProducts.visibility = View.VISIBLE
    }

    private fun showEmptyState() {
        if (currentPage == 0) {
            binding.layoutEmpty.visibility = View.VISIBLE
            binding.rvProducts.visibility = View.GONE
            binding.tvError.visibility = View.GONE
        }
    }


}