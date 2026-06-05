<template>
  <div>
    <div class="mb-8">
      <div class="flex gap-3 overflow-x-auto pb-4 -mx-1 px-1">
        <button
          v-for="category in categories"
          :key="category.id"
          @click="selectCategory(category.id)"
          :class="[
            'px-5 py-2 rounded-xl font-semibold whitespace-nowrap transition-all duration-300',
            selectedCategory === category.id
              ? 'bg-gradient-to-r from-blue-500 to-purple-500 text-white shadow-lg'
              : 'bg-white text-gray-700 hover:bg-gray-50 border border-gray-200'
          ]"
        >
          {{ category.name }}
        </button>
      </div>
    </div>

    <div v-if="productStore.loading" class="flex items-center justify-center py-20">
      <div class="w-12 h-12 border-4 border-blue-200 border-t-blue-500 rounded-full animate-spin"></div>
    </div>

    <div v-else-if="productStore.error" class="text-center py-20">
      <p class="text-red-500 font-medium">{{ productStore.error }}</p>
      <button @click="fetchProducts" class="mt-4 px-6 py-2 bg-blue-500 text-white rounded-xl hover:bg-blue-600 transition-colors">
        重试
      </button>
    </div>

    <div v-else-if="products.length === 0" class="text-center py-20">
      <div class="w-20 h-20 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-6">
        <svg class="w-10 h-10 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4"/>
        </svg>
      </div>
      <p class="text-gray-600 font-medium">暂无商品</p>
    </div>

    <div v-else class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-6">
      <router-link
        v-for="product in products"
        :key="product.id"
        :to="`/product/${product.id}`"
        class="bg-white rounded-xl shadow-md border border-gray-100 overflow-hidden hover:shadow-lg hover:-translate-y-1 transition-all duration-300 group"
      >
        <div class="aspect-square bg-gray-50 flex items-center justify-center overflow-hidden">
          <img
            :src="product.coverImage || 'https://placehold.co/400x400/e2e8f0/64748b?text=商品图片'"
            :alt="product.name"
            class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
            @error="$event.target.src = 'https://placehold.co/400x400/e2e8f0/64748b?text=商品图片'"
          >
        </div>
        <div class="p-4">
          <h3 class="font-semibold text-gray-800 mb-2 truncate">{{ product.name }}</h3>
          <div class="flex items-baseline gap-2 mb-2">
            <span class="text-2xl font-bold bg-gradient-to-r from-blue-500 to-purple-500 bg-clip-text text-transparent">
              ¥{{ product.price?.toFixed(2) || '0.00' }}
            </span>
          </div>
          <p class="text-sm text-gray-500">
            库存: <span :class="product.stock > 0 ? 'text-green-600' : 'text-red-500'">
              {{ product.stock > 0 ? `${product.stock}件` : '缺货' }}
            </span>
          </p>
        </div>
      </router-link>
    </div>
  </div>
</template>

<script setup>
import { onMounted, computed } from 'vue'
import { useProductStore } from '../stores/productStore'
import { useCartStore } from '../stores/cartStore'

const productStore = useProductStore()
const cartStore = useCartStore()

const products = computed(() => productStore.products)
const categories = computed(() => productStore.categories)
const selectedCategory = computed(() => productStore.currentCategory || null)

const selectCategory = (categoryId) => {
  productStore.currentCategory = categoryId
  productStore.fetchProducts(categoryId)
}

const fetchProducts = () => {
  productStore.fetchProducts(selectedCategory.value)
}

onMounted(async () => {
  await Promise.all([
    productStore.fetchCategories(),
    productStore.fetchProducts()
  ])
  cartStore.fetchCart()
})
</script>
