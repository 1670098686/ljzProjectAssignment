package com.campustrade.android.ui.product.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.campustrade.android.databinding.ItemProductImageBinding
import com.campustrade.android.util.MemoryOptimizer

class ProductImageAdapter(private var images: List<String>) : 
    RecyclerView.Adapter<ProductImageAdapter.ProductImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductImageViewHolder {
        val binding = ItemProductImageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductImageViewHolder, position: Int) {
        holder.bind(images[position])
    }

    override fun getItemCount(): Int = images.size

    fun updateImages(newImages: List<String>) {
        images = newImages
        notifyDataSetChanged()
    }

    inner class ProductImageViewHolder(
        private val binding: ItemProductImageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(imageUrl: String) {
            if (imageUrl == "default") {
                // 显示默认图片
                binding.ivProductImage.setImageResource(android.R.drawable.ic_menu_gallery)
            } else {
                // 加载网络图片（使用内存优化版本）
                MemoryOptimizer.loadLargeImage(binding.ivProductImage.context, imageUrl, binding.ivProductImage)
            }
        }
    }
}