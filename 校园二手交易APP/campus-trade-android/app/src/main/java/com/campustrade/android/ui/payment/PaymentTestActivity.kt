package com.campustrade.android.ui.payment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.campustrade.android.databinding.ActivityPaymentTestBinding
import java.math.BigDecimal

class PaymentTestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaymentTestBinding

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, PaymentTestActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupClickListeners()
    }

    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupClickListeners() {
        binding.btnTestPayment1.setOnClickListener {
            testPayment("二手iPhone 13", "张三", "北京大学图书馆", BigDecimal("3500.00"))
        }

        binding.btnTestPayment2.setOnClickListener {
            testPayment("Java编程思想", "李四", "清华大学图书馆", BigDecimal("45.00"))
        }

        binding.btnTestPayment3.setOnClickListener {
            testPayment("笔记本电脑", "王五", "复旦大学图书馆", BigDecimal("2800.00"))
        }

        binding.btnTestPayment4.setOnClickListener {
            testPayment("运动鞋", "赵六", "上海交通大学图书馆", BigDecimal("120.00"))
        }
    }

    private fun testPayment(productName: String, sellerName: String, location: String, amount: BigDecimal) {
        val orderId = "ORDER${System.currentTimeMillis()}"
        
        PaymentActivity.start(
            this,
            orderId,
            productName,
            sellerName,
            location,
            amount
        )
    }
}