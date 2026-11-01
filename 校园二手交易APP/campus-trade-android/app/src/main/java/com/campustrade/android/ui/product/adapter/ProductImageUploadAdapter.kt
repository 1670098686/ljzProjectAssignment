package com.campustrade.android.ui.product.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.campustrade.android.databinding.ItemProductImageUploadBinding

class ProductImageUploadAdapter(
    private var images: List<Uri>,
    private val onAddClick: () -> Unit,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_ADD = 0
        private const val TYPE_IMAGE = 1
        private const val MAX_IMAGES = 5
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == images.size && images.size < MAX_IMAGES) {
            TYPE_ADD
        } else {
            TYPE_IMAGE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_ADD -> {
                val binding = ItemProductImageUploadBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                AddImageViewHolder(binding, onAddClick)
            }
            TYPE_IMAGE -> {
                val binding = ItemProductImageUploadBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                ImageViewHolder(binding, onDelete)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ImageViewHolder -> holder.bind(images[position], position)
            is AddImageViewHolder -> holder.bind()
        }
    }

    override fun getItemCount(): Int {
        return images.size + if (images.size < MAX_IMAGES) 1 else 0
    }

    fun updateImages(newImages: List<Uri>) {
        images = newImages
        notifyDataSetChanged()
    }

    class AddImageViewHolder(
        private val binding: ItemProductImageUploadBinding,
        private val onAddClick: () -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind() {
            // 显示添加按钮，隐藏图片和删除按钮
            binding.ivProductImage.visibility = View.GONE
            binding.btnDeleteImage.visibility = View.GONE
            binding.btnAddImage.visibility = View.VISIBLE

            binding.btnAddImage.setOnClickListener {
                onAddClick()
            }
        }
    }

    class ImageViewHolder(
        private val binding: ItemProductImageUploadBinding,
        private val onDelete: (Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(imageUri: Uri, position: Int) {
            // 显示图片和删除按钮，隐藏添加按钮
            binding.ivProductImage.visibility = View.VISIBLE
            binding.btnDeleteImage.visibility = View.VISIBLE
            binding.btnAddImage.visibility = View.GONE

            // 加载图片
            Glide.with(binding.ivProductImage.context)
                .load(imageUri)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .into(binding.ivProductImage)

            // 设置删除按钮点击事件
            binding.btnDeleteImage.setOnClickListener {
                onDelete(position)
            }
        }
    }
}