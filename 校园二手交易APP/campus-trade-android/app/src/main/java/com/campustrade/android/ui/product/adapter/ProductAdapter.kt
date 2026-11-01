package com.campustrade.android.ui.product.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.campustrade.android.databinding.ItemProductBinding
import com.campustrade.android.model.Product
import com.campustrade.android.model.enums.ProductStatus
import com.campustrade.android.util.MemoryOptimizer
import java.text.NumberFormat
import java.util.Locale

class ProductAdapter(
    private val onItemClick: (Product) -> Unit
) : ListAdapter<Product, ProductAdapter.ProductViewHolder>(ProductDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = getItem(position)
        holder.bind(product)
    }

    inner class ProductViewHolder(
        private val binding: ItemProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product) {
            binding.apply {
                // 设置商品标题
                tvProductTitle.text = product.title
                
                // 设置商品价格
                tvProductPrice.text = formatPrice(product.price)
                
                // 设置商品分类
                tvProductCategory.text = product.category.displayName
                
                // 设置商品状态
                tvProductStatus.text = product.status.displayName
                
                // 设置状态背景颜色
                val statusColor = when (product.status) {
                    ProductStatus.ON_SALE -> android.graphics.Color.parseColor("#4CAF50")
                    ProductStatus.SOLD -> android.graphics.Color.parseColor("#F44336")
                    ProductStatus.OFF_SALE -> android.graphics.Color.parseColor("#9E9E9E")
                    ProductStatus.DELETED -> android.graphics.Color.parseColor("#9E9E9E")
                }
                tvProductStatus.setBackgroundColor(statusColor)
                
                // 设置卖家信息
                tvSellerInfo.text = product.seller?.username ?: "匿名卖家"
                
                // 设置浏览量和点赞数
                tvViewCount.text = product.viewCount.toString()
                tvLikeCount.text = product.likeCount.toString()
                
                // 加载商品图片（使用内存优化版本）
                if (product.images.isNotEmpty()) {
                    MemoryOptimizer.loadProductImage(ivProductImage.context, product.images.first(), ivProductImage)
                } else {
                    ivProductImage.setImageResource(android.R.drawable.ic_menu_gallery)
                }
                
                // 设置点击事件
                root.setOnClickListener {
                    onItemClick(product)
                }
            }
        }
    }

    private fun formatPrice(price: java.math.BigDecimal): String {
        val format = NumberFormat.getCurrencyInstance(Locale.CHINA)
        return format.format(price)
    }

    private class ProductDiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem == newItem
        }
    }
}