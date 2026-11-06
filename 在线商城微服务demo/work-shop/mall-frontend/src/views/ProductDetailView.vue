<template>
  <div>
    <button
      @click="$router.back()"
      class="flex items-center gap-2 text-gray-600 hover:text-blue-600 transition-colors mb-6"
    >
      <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
      </svg>
      返回
    </button>

    <div v-if="productStore.loading" class="flex items-center justify-center py-20">
      <div class="w-12 h-12 border-4 border-blue-200 border-t-blue-500 rounded-full animate-spin"></div>
    </div>

    <div v-else-if="productStore.error" class="text-center py-20">
      <p class="text-red-500 font-medium">{{ productStore.error }}</p>
      <button @click="fetchProductDetail" class="mt-4 px-6 py-2 bg-blue-500 text-white rounded-xl hover:bg-blue-600 transition-colors">
        重试
      </button>
    </div>

    <div v-else-if="!product" class="text-center py-20">
      <div class="w-20 h-20 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-6">
        <svg class="w-10 h-10 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.172 16.172a4 4 0 015.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
        </svg>
      </div>
      <p class="text-gray-600 font-medium">商品不存在</p>
    </div>

    <div v-else class="bg-white rounded-2xl shadow-md border border-gray-100 overflow-hidden">
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-12 p-8 lg:p-12">
        <div>
          <div class="aspect-square bg-gray-50 rounded-2xl flex items-center justify-center overflow-hidden">
            <img
              :src="product.coverImage || 'https://placehold.co/500x500/e2e8f0/64748b?text=商品大图'"
              :alt="product.name"
              class="w-full h-full object-cover"
              @error="$event.target.src = 'https://placehold.co/500x500/e2e8f0/64748b?text=商品大图'"
            >
          </div>
        </div>

        <div class="flex flex-col">
          <h1 class="text-3xl lg:text-4xl font-bold text-gray-800 mb-6">{{ product.name }}</h1>
          
          <div class="flex items-baseline gap-3 mb-8">
            <span class="text-4xl lg:text-5xl font-bold bg-gradient-to-r from-blue-500 to-purple-500 bg-clip-text text-transparent">
              ¥{{ product.price?.toFixed(2) || '0.00' }}
            </span>
          </div>

          <div class="mb-8">
            <p class="text-sm font-medium text-gray-700 mb-3">库存</p>
            <p :class="product.stock > 0 ? 'text-green-600' : 'text-red-500'" class="text-xl font-semibold">
              {{ product.stock > 0 ? `${product.stock} 件` : '缺货' }}
            </p>
          </div>

          <div v-if="product.description" class="mb-10">
            <p class="text-sm font-medium text-gray-700 mb-3">商品描述</p>
            <p class="text-gray-600 leading-relaxed text-lg">{{ product.description }}</p>
          </div>

          <div class="mt-auto space-y-4">
            <div class="flex items-center gap-4 mb-6">
              <label class="text-base font-medium text-gray-700">数量</label>
              <div class="flex items-center gap-3">
                <button
                  @click="quantity = Math.max(1, quantity - 1)"
                  :disabled="quantity <= 1"
                  class="w-12 h-12 bg-gray-100 hover:bg-gray-200 disabled:opacity-50 disabled:cursor-not-allowed rounded-xl flex items-center justify-center transition-colors"
                >
                  <svg class="w-6 h-6 text-gray-700" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 12H4"/>
                  </svg>
                </button>
                <span class="w-16 text-center font-semibold text-gray-800 text-xl">{{ quantity }}</span>
                <button
                  @click="quantity = Math.min(product.stock, quantity + 1)"
                  :disabled="quantity >= product.stock"
                  class="w-12 h-12 bg-gray-100 hover:bg-gray-200 disabled:opacity-50 disabled:cursor-not-allowed rounded-xl flex items-center justify-center transition-colors"
                >
                  <svg class="w-6 h-6 text-gray-700" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
                  </svg>
                </button>
              </div>
            </div>

            <div class="flex gap-4">
              <button
                @click="handleAddToCart"
                :disabled="product.stock <= 0 || cartStore.loading"
                class="flex-1 px-8 py-4 bg-white text-blue-600 font-semibold border-2 border-blue-500 rounded-xl hover:bg-blue-50 transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2 text-lg"
              >
                <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5"/>
                </svg>
                {{ cartStore.loading ? '加入中...' : '加入购物车' }}
              </button>
              
              <button
                @click="handleBuyNow"
                :disabled="product.stock <= 0 || cartStore.loading"
                class="flex-1 px-8 py-4 bg-gradient-to-r from-blue-500 to-purple-500 text-white font-semibold rounded-xl shadow-lg hover:shadow-xl hover:from-blue-600 hover:to-purple-600 transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2 text-lg"
              >
                <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
                </svg>
                立即购买
              </button>
            </div>

            <button
              @click="handleToggleFavorite"
              :disabled="favoriteLoading"
              class="w-full px-8 py-4 font-semibold border-2 rounded-xl transition-all duration-300 flex items-center justify-center gap-2 text-lg disabled:opacity-50 disabled:cursor-not-allowed"
              :class="isFavorite ? 'bg-red-50 text-red-600 border-red-200 hover:bg-red-100' : 'bg-gray-50 text-gray-700 border-gray-200 hover:bg-gray-100 hover:border-gray-300'"
            >
              <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 0 0 0 6.364L12 20.364l7.682-7.682a4.5 4.5 0 0 0-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 0 0-6.364 0z"/>
              </svg>
              {{ favoriteLoading ? '处理中...' : (isFavorite ? '已收藏，点击取消' : '收藏') }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <p v-if="addToCartSuccess" class="fixed bottom-8 right-8 px-6 py-3 bg-green-500 text-white font-semibold rounded-xl shadow-lg">
      已加入购物车
    </p>

    <div v-if="product" class="mt-10 bg-white rounded-2xl shadow-md border border-gray-100 overflow-hidden">
      <div class="p-8 border-b border-gray-100">
        <h2 class="text-2xl font-bold text-gray-800 mb-6">商品评价</h2>
        
        <div v-if="reviewsLoading" class="flex items-center justify-center py-12">
          <div class="w-10 h-10 border-4 border-blue-200 border-t-blue-500 rounded-full animate-spin"></div>
        </div>
        
        <div v-else-if="reviewSummary.totalCount === 0" class="text-center py-12">
          <div class="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4">
            <svg class="w-8 h-8 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 8h10M7 12h4m1 8l-4-4H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-3l-4 4z"/>
            </svg>
          </div>
          <p class="text-gray-500 text-lg">暂无评价</p>
          <p class="text-gray-400 text-sm mt-2">购买后即可成为第一个评价者</p>
        </div>

        <div v-else>
          <div class="flex items-start gap-10 mb-8">
            <div class="text-center">
              <div class="text-5xl font-bold text-orange-500 mb-2">{{ Number(reviewSummary.averageRating || 0).toFixed(1) }}</div>
              <div class="flex gap-1 mb-2">
                <svg v-for="i in 5" :key="i" class="w-5 h-5" :class="i <= Math.round(reviewSummary.averageRating || 0) ? 'text-orange-400' : 'text-gray-300'" fill="currentColor" viewBox="0 0 20 20">
                  <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z"/>
                </svg>
              </div>
              <p class="text-gray-500 text-sm">{{ reviewSummary.totalCount }} 条评价</p>
            </div>
            
            <div class="flex flex-wrap gap-2">
              <button
                v-for="rating in [5, 4, 3, 2, 1]"
                :key="rating"
                @click="selectedRating = selectedRating === rating ? null : rating"
                class="px-4 py-2 rounded-full text-sm font-medium transition-colors"
                :class="selectedRating === rating ? 'bg-blue-50 text-blue-600' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'"
              >
                {{ rating }}星 ({{ reviewSummary.ratingDistribution?.[rating] || 0 }})
              </button>
            </div>
          </div>

          <div class="space-y-6">
            <div v-for="review in reviews" :key="review.reviewId" class="border-b border-gray-100 pb-6 last:border-0 last:pb-0">
              <div class="flex items-start gap-4">
                <div class="w-10 h-10 bg-gradient-to-br from-blue-400 to-purple-500 rounded-full flex items-center justify-center text-white font-semibold">
                  {{ (review.username || '匿名').charAt(0) }}
                </div>
                <div class="flex-1">
                  <div class="flex items-center gap-2 mb-2">
                    <span class="font-medium text-gray-800">{{ review.username || '匿名用户' }}</span>
                    <div class="flex gap-0.5">
                      <svg v-for="i in 5" :key="i" class="w-4 h-4" :class="i <= review.rating ? 'text-orange-400' : 'text-gray-300'" fill="currentColor" viewBox="0 0 20 20">
                        <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z"/>
                      </svg>
                    </div>
                  </div>
                  <p class="text-gray-600 leading-relaxed mb-3">{{ review.content }}</p>
                  <div v-if="review.images && review.images.length > 0" class="flex gap-2 mb-3">
                    <img
                      v-for="(img, idx) in review.images"
                      :key="idx"
                      :src="img"
                      class="w-20 h-20 object-cover rounded-lg cursor-pointer hover:opacity-80 transition-opacity"
                      @error="$event.target.src = 'https://placehold.co/80x80/e2e8f0/64748b?text=图片'"
                    >
                  </div>
                  <p class="text-gray-400 text-sm">{{ review.createTime }}</p>
                </div>
              </div>
            </div>
          </div>

          <button
            v-if="reviewSummary.totalCount > 3"
            @click="showAllReviews = true"
            class="mt-6 w-full py-3 text-blue-600 font-medium hover:bg-blue-50 rounded-xl transition-colors"
          >
            查看全部 {{ reviewSummary.totalCount }} 条评价
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../api/request'
import { useProductStore } from '../stores/productStore'
import { useCartStore } from '../stores/cartStore'

const route = useRoute()
const router = useRouter()
const productStore = useProductStore()
const cartStore = useCartStore()

const quantity = ref(1)
const addToCartSuccess = ref(false)
const isFavorite = ref(false)
const favoriteId = ref(null)
const favoriteLoading = ref(false)

const reviews = ref([])
const reviewSummary = ref({ totalCount: 0, averageRating: 0, ratingDistribution: {1: 0, 2: 0, 3: 0, 4: 0, 5: 0} })
const reviewsLoading = ref(false)
const showAllReviews = ref(false)
const selectedRating = ref(null)

const product = computed(() => productStore.currentProduct)

const fetchProductDetail = async () => {
  const productId = route.params.id
  await productStore.fetchProductDetail(productId)
  await fetchFavoriteStatus(productId)
  await fetchProductReviews(productId)
}

const fetchFavoriteStatus = async (productId) => {
  try {
    const response = await api.get(`/api/favorite/check/${productId}`)
    isFavorite.value = Boolean(response.data?.data?.isFavorite)
    favoriteId.value = response.data?.data?.favoriteId || null
  } catch (error) {
    console.error('获取收藏状态失败:', error)
    isFavorite.value = false
    favoriteId.value = null
  }
}

const fetchProductReviews = async (productId) => {
  reviewsLoading.value = true
  try {
    const response = await api.get(`/api/review/list?productId=${productId}&page=1&pageSize=3`)
    if (response.data?.data) {
      const data = response.data.data
      reviews.value = data.list || []
      reviewSummary.value = data.summary || { totalCount: 0, averageRating: 0, ratingDistribution: {1: 0, 2: 0, 3: 0, 4: 0, 5: 0} }
    }
  } catch (error) {
    console.error('获取评价列表失败:', error)
  } finally {
    reviewsLoading.value = false
  }
}

const handleAddToCart = async () => {
  if (!product.value) return
  
  try {
    await cartStore.addToCart(product.value.id, quantity.value, product.value)
    addToCartSuccess.value = true
    setTimeout(() => {
      addToCartSuccess.value = false
    }, 2000)
  } catch (error) {
    console.error('Failed to add to cart:', error)
  }
}

const handleBuyNow = async () => {
  if (!product.value) return
  
  try {
    await cartStore.addToCart(product.value.id, quantity.value, product.value)
    router.push('/cart')
  } catch (error) {
    console.error('Failed to add to cart:', error)
  }
}

const handleToggleFavorite = async () => {
  if (!product.value || favoriteLoading.value) return

  favoriteLoading.value = true
  try {
    if (isFavorite.value && favoriteId.value) {
      await api.delete(`/api/favorite/${favoriteId.value}`)
      isFavorite.value = false
      favoriteId.value = null
      alert('已取消收藏')
      return
    }

    const response = await api.post('/api/favorite/add', {
      productId: product.value.id
    })
    isFavorite.value = true
    favoriteId.value = response.data?.data?.favoriteId || null
    alert('收藏成功')
  } catch (error) {
    console.error('收藏操作失败:', error)
    alert(error.response?.data?.message || '收藏操作失败，请稍后重试')
  } finally {
    favoriteLoading.value = false
  }
}

onMounted(() => {
  fetchProductDetail()
})
</script>
