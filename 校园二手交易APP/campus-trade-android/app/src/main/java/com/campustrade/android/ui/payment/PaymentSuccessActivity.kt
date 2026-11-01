package com.campustrade.android.ui.payment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.campustrade.android.databinding.ActivityPaymentSuccessBinding
import com.campustrade.android.ui.MainActivity
import com.campustrade.android.ui.product.ProductListActivity
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PaymentSuccessActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaymentSuccessBinding

    private var paymentId: String = ""
    private var orderId: String = ""
    private var productName: String = ""
    private var sellerName: String = ""
    private var location: String = ""
    private var amount: BigDecimal = BigDecimal.ZERO
    private var paymentMethod: String = ""
    private var paymentTime: String = ""

    companion object {
        private const val EXTRA_PAYMENT_ID = "extra_payment_id"
        private const val EXTRA_ORDER_ID = "extra_order_id"
        private const val EXTRA_PRODUCT_NAME = "extra_product_name"
        private const val EXTRA_SELLER_NAME = "extra_seller_name"
        private const val EXTRA_LOCATION = "extra_location"
        private const val EXTRA_AMOUNT = "extra_amount"
        private const val EXTRA_PAYMENT_METHOD = "extra_payment_method"
        private const val EXTRA_PAYMENT_TIME = "extra_payment_time"

        fun start(
            context: Context,
            paymentId: String,
            orderId: String,
            productName: String,
            sellerName: String,
            location: String,
            amount: BigDecimal,
            paymentMethod: String,
            paymentTime: String
        ) {
            val intent = Intent(context, PaymentSuccessActivity::class.java).apply {
                putExtra(EXTRA_PAYMENT_ID, paymentId)
                putExtra(EXTRA_ORDER_ID, orderId)
                putExtra(EXTRA_PRODUCT_NAME, productName)
                putExtra(EXTRA_SELLER_NAME, sellerName)
                putExtra(EXTRA_LOCATION, location)
                putExtra(EXTRA_AMOUNT, amount.toString())
                putExtra(EXTRA_PAYMENT_METHOD, paymentMethod)
                putExtra(EXTRA_PAYMENT_TIME, paymentTime)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentSuccessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initData()
        setupUI()
        setupClickListeners()
    }

    private fun initData() {
        paymentId = intent.getStringExtra(EXTRA_PAYMENT_ID) ?: generatePaymentId()
        orderId = intent.getStringExtra(EXTRA_ORDER_ID) ?: ""
        productName = intent.getStringExtra(EXTRA_PRODUCT_NAME) ?: ""
        sellerName = intent.getStringExtra(EXTRA_SELLER_NAME) ?: ""
        location = intent.getStringExtra(EXTRA_LOCATION) ?: ""
        amount = try {
            BigDecimal(intent.getStringExtra(EXTRA_AMOUNT) ?: "0")
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
        paymentMethod = intent.getStringExtra(EXTRA_PAYMENT_METHOD) ?: "模拟支付"
        paymentTime = intent.getStringExtra(EXTRA_PAYMENT_TIME) ?: getCurrentTime()
    }

    private fun setupUI() {
        // 设置工具栏
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        // 设置支付详情
        binding.tvOrderId.text = orderId
        binding.tvPaymentMethod.text = paymentMethod
        binding.tvPaymentAmount.text = "¥${amount.setScale(2)}"
        binding.tvPaymentTime.text = paymentTime
        binding.tvPaymentStatus.text = "支付成功"

        // 设置订单信息
        binding.tvProductName.text = productName
        binding.tvSellerName.text = sellerName
        binding.tvLocation.text = location
    }

    private fun setupClickListeners() {
        binding.btnViewOrder.setOnClickListener {
            viewOrderDetails()
        }

        binding.btnBackHome.setOnClickListener {
            backToHome()
        }
    }

    private fun viewOrderDetails() {
        // 这里可以跳转到订单详情页面
        Toast.makeText(this, "查看订单详情功能待实现", Toast.LENGTH_SHORT).show()
        
        // 临时跳转到我的订单页面
        OrderListActivity.start(this)
    }

    private fun backToHome() {
        // 清空返回栈并返回首页
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }

    private fun generatePaymentId(): String {
        val timestamp = System.currentTimeMillis()
        return "PAY${timestamp}"
    }

    private fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    override fun onBackPressed() {
        // 按返回键时直接返回首页
        backToHome()
    }
}