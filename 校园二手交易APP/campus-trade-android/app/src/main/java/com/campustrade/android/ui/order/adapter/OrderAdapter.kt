package com.campustrade.android.ui.order.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.campustrade.android.databinding.ItemOrderBinding
import com.campustrade.android.model.Order
import com.campustrade.android.model.enums.OrderStatus
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class OrderAdapter(
    private val onItemClick: (Order) -> Unit,
    private val onPayClick: (Order) -> Unit,
    private val onCancelClick: (Order) -> Unit,
    private val onConfirmReceiptClick: (Order) -> Unit
) : ListAdapter<Order, OrderAdapter.OrderViewHolder>(OrderDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemOrderBinding.inflate(
            LayoutInflater.from(parent.context), 
            parent, 
            false
        )
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = getItem(position)
        holder.bind(order)
    }

    inner class OrderViewHolder(private val binding: ItemOrderBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(order: Order) {
            binding.apply {
                // 设置订单基本信息
                tvOrderNumber.text = "订单号: ${order.orderNumber}"
                tvOrderStatus.text = order.status.displayName
                tvOrderTime.text = formatDate(order.createTime)
                
                // 设置商品信息
                order.product?.let { product ->
                    // 这里应该加载商品图片，暂时使用占位图
                    // ivProductImage.setImageResource(R.drawable.placeholder_product)
                    tvProductTitle.text = product.title
                    tvProductPrice.text = formatPrice(product.price)
                    tvQuantity.text = "数量: ${order.quantity}"
                }
                
                // 设置订单金额
                tvTotalAmount.text = formatPrice(order.totalAmount)
                
                // 设置操作按钮可见性
                setupActionButtons(order)
                
                // 设置点击事件
                root.setOnClickListener {
                    onItemClick(order)
                }
                
                btnPay.setOnClickListener {
                    onPayClick(order)
                }
                
                btnCancel.setOnClickListener {
                    onCancelClick(order)
                }
                
                btnConfirmReceipt.setOnClickListener {
                    onConfirmReceiptClick(order)
                }
            }
        }
        
        private fun setupActionButtons(order: Order) {
            binding.apply {
                // 根据订单状态显示不同的按钮
                when (order.status) {
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
        
        private fun formatPrice(price: java.math.BigDecimal): String {
            val format = NumberFormat.getCurrencyInstance(Locale.CHINA)
            return format.format(price)
        }
        
        private fun formatDate(date: java.time.LocalDateTime): String {
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
            return formatter.format(java.util.Date.from(date.atZone(java.time.ZoneId.systemDefault()).toInstant()))
        }
    }

    companion object {
        private val OrderDiffCallback = object : DiffUtil.ItemCallback<Order>() {
            override fun areItemsTheSame(oldItem: Order, newItem: Order): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Order, newItem: Order): Boolean {
                return oldItem == newItem
            }
        }
    }
}