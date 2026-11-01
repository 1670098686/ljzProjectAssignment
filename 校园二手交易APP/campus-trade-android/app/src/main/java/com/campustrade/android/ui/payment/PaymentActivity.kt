package com.campustrade.android.ui.payment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.campustrade.android.databinding.ActivityPaymentBinding
import com.campustrade.android.ui.payment.model.PaymentData
import com.campustrade.android.ui.payment.model.PaymentMethod
import com.campustrade.android.ui.payment.model.PaymentRequest
import dagger.hilt.android.AndroidEntryPoint
import java.math.BigDecimal

@AndroidEntryPoint
class PaymentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaymentBinding
    private val viewModel: PaymentViewModel by viewModels()

    private var orderId: String = ""
    private var productName: String = ""
    private var sellerName: String = ""
    private var location: String = ""
    private var amount: BigDecimal = BigDecimal.ZERO

    companion object {
        private const val EXTRA_ORDER_ID = "extra_order_id"
        private const val EXTRA_PRODUCT_NAME = "extra_product_name"
        private const val EXTRA_SELLER_NAME = "extra_seller_name"
        private const val EXTRA_LOCATION = "extra_location"
        private const val EXTRA_AMOUNT = "extra_amount"

        fun start(
            context: Context,
            orderId: String,
            productName: String,
            sellerName: String,
            location: String,
            amount: BigDecimal
        ) {
            val intent = Intent(context, PaymentActivity::class.java).apply {
                putExtra(EXTRA_ORDER_ID, orderId)
                putExtra(EXTRA_PRODUCT_NAME, productName)
                putExtra(EXTRA_SELLER_NAME, sellerName)
                putExtra(EXTRA_LOCATION, location)
                putExtra(EXTRA_AMOUNT, amount.toString())
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initData()
        setupUI()
        setupClickListeners()
        setupObservers()
    }

    private fun initData() {
        orderId = intent.getStringExtra(EXTRA_ORDER_ID) ?: ""
        productName = intent.getStringExtra(EXTRA_PRODUCT_NAME) ?: ""
        sellerName = intent.getStringExtra(EXTRA_SELLER_NAME) ?: ""
        location = intent.getStringExtra(EXTRA_LOCATION) ?: ""
        amount = try {
            BigDecimal(intent.getStringExtra(EXTRA_AMOUNT) ?: "0")
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
    }

    private fun setupUI() {
        // 设置工具栏
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        // 设置订单信息
        binding.tvProductName.text = productName
        binding.tvSellerName.text = sellerName
        binding.tvLocation.text = location
        binding.tvAmount.text = "¥${amount.setScale(2)}"
        binding.tvTotalAmount.text = "¥${amount.setScale(2)}"

        // 默认选中模拟支付
        binding.rbSimulated.isChecked = true
    }

    private fun setupClickListeners() {
        binding.btnConfirmPayment.setOnClickListener {
            confirmPayment()
        }
    }

    private fun setupObservers() {
        viewModel.paymentUiState.observe(this) { state ->
            when (state) {
                is PaymentViewModel.PaymentUiState.Loading -> {
                    showLoading(true)
                }
                is PaymentViewModel.PaymentUiState.Success -> {
                    showLoading(false)
                    handlePaymentSuccess(state.paymentData)
                }
                is PaymentViewModel.PaymentUiState.Error -> {
                    showLoading(false)
                    showError(state.message)
                }
                is PaymentViewModel.PaymentUiState.Idle -> {
                    showLoading(false)
                }
            }
        }

        viewModel.operationState.observe(this) { state ->
            when (state) {
                is PaymentViewModel.OperationState.Loading -> {
                    showLoading(true)
                }
                is PaymentViewModel.OperationState.Success -> {
                    showLoading(false)
                    when (state.operation) {
                        "payment" -> handlePaymentSuccess(state.data as PaymentData)
                        "refund" -> handleRefundSuccess()
                        "cancel" -> handleCancelSuccess()
                    }
                }
                is PaymentViewModel.OperationState.Error -> {
                    showLoading(false)
                    showError(state.message)
                }
                is PaymentViewModel.OperationState.Idle -> {
                    showLoading(false)
                }
            }
        }
    }

    private fun confirmPayment() {
        val selectedMethod = when {
            binding.rbSimulated.isChecked -> PaymentMethod.SIMULATED
            else -> PaymentMethod.SIMULATED
        }

        val paymentRequest = PaymentRequest(
            orderId = orderId,
            amount = amount,
            paymentMethod = selectedMethod,
            productName = productName,
            sellerName = sellerName,
            location = location
        )

        viewModel.executePayment(paymentRequest)
    }

    private fun handlePaymentSuccess(paymentData: PaymentData) {
        // 跳转到支付成功页面
        PaymentSuccessActivity.start(
            this,
            paymentData.paymentId,
            paymentData.orderId,
            productName,
            sellerName,
            location,
            amount,
            paymentData.paymentMethod.name,
            paymentData.updateTime ?: ""
        )
        
        // 返回结果给调用方
        val resultIntent = Intent().apply {
            putExtra("payment_id", paymentData.paymentId)
            putExtra("order_id", paymentData.orderId)
            putExtra("amount", paymentData.amount.toString())
            putExtra("status", paymentData.status.name)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    private fun handleRefundSuccess() {
        Toast.makeText(this, "退款申请已提交", Toast.LENGTH_SHORT).show()
    }

    private fun handleCancelSuccess() {
        Toast.makeText(this, "支付已取消", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.isVisible = show
        binding.btnConfirmPayment.isEnabled = !show
        binding.btnConfirmPayment.text = if (show) "支付中..." else "确认支付"
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun getSelectedPaymentMethod(): PaymentMethod {
        return when {
            binding.rbSimulated.isChecked -> PaymentMethod.SIMULATED
            else -> PaymentMethod.SIMULATED
        }
    }
}