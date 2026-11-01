package com.campustrade.android.ui.order

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.campustrade.android.R
import com.campustrade.android.databinding.ActivityOrderListBinding
import com.campustrade.android.model.enums.OrderStatus
import com.campustrade.android.ui.order.adapter.OrderAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OrderListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderListBinding
    private val viewModel: OrderViewModel by viewModels()
    private lateinit var orderAdapter: OrderAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupObservers()
        loadOrders()
    }

    private fun setupUI() {
        setupToolbar()
        setupTabLayout()
        setupRecyclerView()
        setupRetryButton()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> loadOrders(status = null) // 全部
                    1 -> loadOrders(status = OrderStatus.PENDING.name) // 待支付
                    2 -> loadOrders(status = OrderStatus.PAID.name) // 待发货
                    3 -> loadOrders(status = OrderStatus.SHIPPED.name) // 待收货
                    4 -> loadOrders(status = OrderStatus.COMPLETED.name) // 已完成
                }
            }

            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
    }

    private fun setupRecyclerView() {
        orderAdapter = OrderAdapter(
            onItemClick = { order ->
                // 点击订单项，跳转到订单详情
                OrderDetailActivity.start(this, order.id)
            },
            onPayClick = { order ->
                // 支付订单
                showPaymentDialog(order)
            },
            onCancelClick = { order ->
                // 取消订单
                showCancelDialog(order)
            },
            onConfirmReceiptClick = { order ->
                // 确认收货
                showConfirmReceiptDialog(order)
            }
        )

        binding.rvOrders.apply {
            layoutManager = LinearLayoutManager(this@OrderListActivity)
            adapter = orderAdapter
        }
    }

    private fun setupRetryButton() {
        binding.btnRetry.setOnClickListener {
            loadOrders()
        }
    }

    private fun loadOrders(status: String? = null) {
        viewModel.loadOrders(status = status)
    }

    private fun setupObservers() {
        viewModel.uiState.observe(this) { state ->
            binding.progressBar.visibility = if (state.isLoading) android.view.View.VISIBLE else android.view.View.GONE
            binding.rvOrders.visibility = if (state.isLoading || state.error != null) android.view.View.GONE else android.view.View.VISIBLE
            binding.emptyState.visibility = if (state.orders.isEmpty() && !state.isLoading && state.error == null) android.view.View.VISIBLE else android.view.View.GONE
            binding.errorState.visibility = if (state.error != null) android.view.View.VISIBLE else android.view.View.GONE

            if (state.error != null) {
                binding.tvErrorMessage.text = state.error
            }

            if (!state.isLoading && state.error == null) {
                orderAdapter.submitList(state.orders)
            }
        }

        viewModel.operationState.observe(this) { state ->
            when (state) {
                is OrderViewModel.OperationState.Loading -> {
                    // 显示加载状态
                }
                is OrderViewModel.OperationState.Success -> {
                    Toast.makeText(this, "操作成功", Toast.LENGTH_SHORT).show()
                    viewModel.resetOperationState()
                }
                is OrderViewModel.OperationState.Error -> {
                    Toast.makeText(this, "操作失败: ${state.message}", Toast.LENGTH_SHORT).show()
                    viewModel.resetOperationState()
                }
                else -> {}
            }
        }
    }

    private fun showPaymentDialog(order: com.campustrade.android.model.Order) {
        // 显示支付对话框
        val paymentMethods = arrayOf("支付宝", "微信支付")
        android.app.AlertDialog.Builder(this)
            .setTitle("选择支付方式")
            .setItems(paymentMethods) { _, which ->
                val method = paymentMethods[which]
                viewModel.payOrder(order.id, method)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showCancelDialog(order: com.campustrade.android.model.Order) {
        android.app.AlertDialog.Builder(this)
            .setTitle("取消订单")
            .setMessage("确定要取消这个订单吗？")
            .setPositiveButton("确定") { _, _ ->
                viewModel.cancelOrder(order.id)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showConfirmReceiptDialog(order: com.campustrade.android.model.Order) {
        android.app.AlertDialog.Builder(this)
            .setTitle("确认收货")
            .setMessage("确认收到商品后，订单将完成交易")
            .setPositiveButton("确认收货") { _, _ ->
                viewModel.confirmReceipt(order.id)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    companion object {
        fun start(activity: android.app.Activity) {
            val intent = android.content.Intent(activity, OrderListActivity::class.java)
            activity.startActivity(intent)
        }
    }
}