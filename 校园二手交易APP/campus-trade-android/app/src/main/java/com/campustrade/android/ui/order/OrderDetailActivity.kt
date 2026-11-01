package com.campustrade.android.ui.order

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.campustrade.android.databinding.ActivityOrderDetailBinding
import com.campustrade.android.model.enums.OrderStatus
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OrderDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderDetailBinding
    private val viewModel: OrderViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val orderId = intent.getLongExtra(EXTRA_ORDER_ID, -1L)
        if (orderId == -1L) {
            Toast.makeText(this, "订单ID无效", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI()
        setupObservers()
        loadOrderDetail(orderId)
    }

    private fun setupUI() {
        setupToolbar()
        setupActionButtons()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupActionButtons() {
        binding.btnPay.setOnClickListener {
            showPaymentDialog()
        }

        binding.btnCancel.setOnClickListener {
            showCancelDialog()
        }

        binding.btnConfirmReceipt.setOnClickListener {
            showConfirmReceiptDialog()
        }
    }

    private fun loadOrderDetail(orderId: Long) {
        viewModel.loadOrderDetail(orderId)
    }

    private fun setupObservers() {
        viewModel.uiState.observe(this) { state ->
            binding.progressBar.visibility = if (state.isLoading) android.view.View.VISIBLE else android.view.View.GONE

            if (!state.isLoading && state.error == null && state.currentOrder != null) {
                displayOrderInfo(state.currentOrder)
            }

            if (state.error != null) {
                Toast.makeText(this, "加载失败: ${state.error}", Toast.LENGTH_SHORT).show()
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
                    // 重新加载订单详情
                    val orderId = intent.getLongExtra(EXTRA_ORDER_ID, -1L)
                    loadOrderDetail(orderId)
                }
                is OrderViewModel.OperationState.Error -> {
                    Toast.makeText(this, "操作失败: ${state.message}", Toast.LENGTH_SHORT).show()
                    viewModel.resetOperationState()
                }
                else -> {}
            }
        }
    }

    private fun displayOrderInfo(order: com.campustrade.android.model.Order) {
        binding.apply {
            // 设置订单基本信息
            tvOrderNumber.text = "订单号: ${order.orderNumber}"
            tvOrderStatus.text = order.status.displayName
            tvOrderTime.text = "下单时间: ${formatDate(order.createTime)}"

            // 设置商品信息
            order.product?.let { product ->
                tvProductTitle.text = product.title
                tvProductPrice.text = formatPrice(product.price)
                tvQuantity.text = "数量: ${order.quantity}"
                tvProductAmount.text = formatPrice(order.totalAmount)
            }

            // 设置订单金额
            tvShippingFee.text = formatPrice(order.shippingFee)
            tvTotalAmount.text = formatPrice(order.totalAmount)

            // 设置操作按钮可见性
            setupActionButtonsVisibility(order.status)
        }
    }

    private fun setupActionButtonsVisibility(status: OrderStatus) {
        binding.apply {
            when (status) {
                OrderStatus.PENDING -> {
                    btnPay.visibility = android.view.View.VISIBLE
                    btnCancel.visibility = android.view.View.VISIBLE
                    btnConfirmReceipt.visibility = android.view.View.GONE
                }
                OrderStatus.PAID -> {
                    btnPay.visibility = android.view.View.GONE
                    btnCancel.visibility = android.view.View.VISIBLE
                    btnConfirmReceipt.visibility = android.view.View.GONE
                }
                OrderStatus.SHIPPED -> {
                    btnPay.visibility = android.view.View.GONE
                    btnCancel.visibility = android.view.View.GONE
                    btnConfirmReceipt.visibility = android.view.View.VISIBLE
                }
                OrderStatus.COMPLETED, OrderStatus.CANCELLED -> {
                    btnPay.visibility = android.view.View.GONE
                    btnCancel.visibility = android.view.View.GONE
                    btnConfirmReceipt.visibility = android.view.View.GONE
                }
            }
        }
    }

    private fun showPaymentDialog() {
        val orderId = intent.getLongExtra(EXTRA_ORDER_ID, -1L)
        val paymentMethods = arrayOf("支付宝", "微信支付")
        android.app.AlertDialog.Builder(this)
            .setTitle("选择支付方式")
            .setItems(paymentMethods) { _, which ->
                val method = paymentMethods[which]
                viewModel.payOrder(orderId, method)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showCancelDialog() {
        val orderId = intent.getLongExtra(EXTRA_ORDER_ID, -1L)
        android.app.AlertDialog.Builder(this)
            .setTitle("取消订单")
            .setMessage("确定要取消这个订单吗？")
            .setPositiveButton("确定") { _, _ ->
                viewModel.cancelOrder(orderId)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showConfirmReceiptDialog() {
        val orderId = intent.getLongExtra(EXTRA_ORDER_ID, -1L)
        android.app.AlertDialog.Builder(this)
            .setTitle("确认收货")
            .setMessage("确认收到商品后，订单将完成交易")
            .setPositiveButton("确认收货") { _, _ ->
                viewModel.confirmReceipt(orderId)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun formatPrice(price: java.math.BigDecimal): String {
        val format = java.text.NumberFormat.getCurrencyInstance(java.util.Locale.CHINA)
        return format.format(price)
    }

    private fun formatDate(date: java.time.LocalDateTime): String {
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.CHINA)
        return formatter.format(java.util.Date.from(date.atZone(java.time.ZoneId.systemDefault()).toInstant()))
    }

    companion object {
        private const val EXTRA_ORDER_ID = "extra_order_id"

        fun start(activity: android.app.Activity, orderId: Long) {
            val intent = android.content.Intent(activity, OrderDetailActivity::class.java)
            intent.putExtra(EXTRA_ORDER_ID, orderId)
            activity.startActivity(intent)
        }
    }
}