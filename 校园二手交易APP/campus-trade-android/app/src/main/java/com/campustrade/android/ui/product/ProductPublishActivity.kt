package com.campustrade.android.ui.product

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.campustrade.android.R
import com.campustrade.android.databinding.ActivityProductPublishBinding
import com.campustrade.android.model.enums.ProductCategory
import com.campustrade.android.ui.product.PublishState
import com.campustrade.android.ui.product.adapter.ProductImageUploadAdapter
import dagger.hilt.android.AndroidEntryPoint
import com.campustrade.android.util.observe
import java.math.BigDecimal

@AndroidEntryPoint
class ProductPublishActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductPublishBinding
    private val viewModel: ProductViewModel by viewModels()
    private lateinit var imageUploadAdapter: ProductImageUploadAdapter
    private val imageUris = mutableListOf<Uri>()

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                imageUris.add(uri)
                imageUploadAdapter.updateImages(imageUris)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductPublishBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupObservers()
    }

    private fun setupUI() {
        setupToolbar()
        setupCategorySpinner()
        setupImageUpload()
        setupPublishButton()
    }

    private fun setupToolbar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "发布商品"
    }

    private fun setupCategorySpinner() {
        val categories = ProductCategory.values().map { it.displayName }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = adapter
    }

    private fun setupImageUpload() {
        imageUploadAdapter = ProductImageUploadAdapter(
            images = imageUris,
            onAddClick = { pickImage() },
            onDelete = { position ->
                imageUris.removeAt(position)
                imageUploadAdapter.updateImages(imageUris)
            }
        )

        binding.rvProductImages.apply {
            layoutManager = LinearLayoutManager(this@ProductPublishActivity, 
                LinearLayoutManager.HORIZONTAL, false)
            adapter = imageUploadAdapter
        }
    }

    private fun setupPublishButton() {
        binding.btnPublish.setOnClickListener {
            validateAndPublish()
        }
    }

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        pickImageLauncher.launch(intent)
    }

    private fun validateAndPublish() {
        val title = binding.etProductTitle.text.toString().trim()
        val priceText = binding.etProductPrice.text.toString().trim()
        val description = binding.etProductDescription.text.toString().trim()
        val contactInfo = binding.etContactInfo.text.toString().trim()
        val location = binding.etLocation.text.toString().trim()

        if (title.isEmpty()) {
            binding.etProductTitle.error = "请输入商品标题"
            return
        }

        if (priceText.isEmpty()) {
            binding.etProductPrice.error = "请输入商品价格"
            return
        }

        val price = try {
            priceText.toBigDecimal()
        } catch (e: NumberFormatException) {
            binding.etProductPrice.error = "价格格式不正确"
            return
        }

        if (price <= BigDecimal.ZERO) {
            binding.etProductPrice.error = "价格必须大于0"
            return
        }

        if (description.isEmpty()) {
            binding.etProductDescription.error = "请输入商品描述"
            return
        }

        if (contactInfo.isEmpty()) {
            binding.etContactInfo.error = "请输入联系方式"
            return
        }

        val category = ProductCategory.values()[binding.spinnerCategory.selectedItemPosition]

        // 发布商品
        viewModel.publishProduct(
            title = title,
            price = price,
            description = description,
            category = category,
            contactInfo = contactInfo,
            location = location,
            images = imageUris.map { it.toString() }.toList()
        )
    }

    private fun setupObservers() {
        viewModel.publishState.observe(this) { state ->
            when (state) {
                is PublishState.Loading -> {
                    binding.btnPublish.isEnabled = false
                    binding.btnPublish.text = "发布中..."
                }
                is PublishState.Success -> {
                    binding.btnPublish.isEnabled = true
                    binding.btnPublish.text = "发布商品"
                    Toast.makeText(this, "商品发布成功", Toast.LENGTH_SHORT).show()
                    finish()
                }
                is PublishState.Error -> {
                    binding.btnPublish.isEnabled = true
                    binding.btnPublish.text = "发布商品"
                    Toast.makeText(this, "发布失败: ${state.message}", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    binding.btnPublish.isEnabled = true
                    binding.btnPublish.text = "发布商品"
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    companion object {
        fun start(activity: Activity) {
            val intent = Intent(activity, ProductPublishActivity::class.java)
            activity.startActivity(intent)
        }
    }
}