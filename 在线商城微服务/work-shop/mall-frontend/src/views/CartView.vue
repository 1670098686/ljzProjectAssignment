<template>
  <div class="pb-32">
    <h1 class="text-3xl font-bold text-gray-800 mb-8">购物车</h1>

    <div v-if="cartStore.loading" class="flex items-center justify-center py-24">
      <div class="w-14 h-14 border-4 border-blue-200 border-t-blue-500 rounded-full animate-spin"></div>
    </div>

    <div v-else-if="cartStore.error" class="text-center py-24">
      <div class="w-32 h-32 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-6">
        <svg v-if="cartStore.error.needLogin" class="w-16 h-16 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"/>
        </svg>
        <svg v-else class="w-16 h-16 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/>
        </svg>
      </div>
      <p class="text-xl font-semibold text-gray-800 mb-2">{{ cartStore.error.message }}</p>
      <p class="text-gray-500 mb-6">{{ cartStore.error.details ? '' : '请稍后重试' }}</p>
      <div class="flex items-center justify-center gap-4">
        <button v-if="cartStore.error.needLogin" @click="goToLogin" class="px-8 py-4 bg-gradient-to-r from-blue-500 to-purple-500 text-white font-semibold rounded-xl hover:from-indigo-600 hover:to-purple-600 transition-all duration-300">
          去登录
        </button>
        <button @click="retryFetchCart" class="px-8 py-4 bg-gray-100 text-gray-700 font-semibold rounded-xl hover:bg-gray-200 transition-colors">
          重试
        </button>
      </div>
    </div>

    <div v-else-if="cartStore.cartItems.length === 0" class="text-center py-24">
      <div class="w-32 h-32 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-6">
        <svg class="w-16 h-16 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5"/>
        </svg>
      </div>
      <p class="text-gray-600 font-medium text-lg mb-4">购物车是空的</p>
      <router-link to="/" class="inline-flex items-center gap-2 px-8 py-4 bg-gradient-to-r from-blue-500 to-purple-500 text-white font-semibold rounded-xl hover:from-indigo-600 hover:to-purple-600 transition-all duration-300">
        去购物
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 8l4 4m0 0l-4 4m4-4H3"/>
        </svg>
      </router-link>
    </div>

    <div v-else class="space-y-6">
      <div class="flex items-center gap-4 px-6 py-5 bg-white rounded-xl shadow-md border border-gray-100">
        <input
          v-model="selectAll"
          type="checkbox"
          @change="toggleSelectAll"
          class="w-5 h-5 rounded border-gray-300 text-blue-500 focus:ring-blue-500"
        >
        <label class="font-semibold text-gray-700">全选</label>
      </div>

      <div class="space-y-4">
        <div
          v-for="item in cartStore.cartItems"
          :key="item.id"
          class="bg-white rounded-xl shadow-md border border-gray-100 overflow-hidden"
        >
          <div class="flex flex-col sm:flex-row items-start sm:items-center gap-6 px-6 py-5">
            <div class="flex items-center gap-4 w-full sm:w-auto">
              <input
                v-model="selectedItems"
                type="checkbox"
                :value="item.id"
                class="w-5 h-5 rounded border-gray-300 text-blue-500 focus:ring-blue-500 flex-shrink-0"
              >
              <div class="w-24 h-24 bg-gray-50 rounded-xl flex items-center justify-center overflow-hidden flex-shrink-0">
                <img
                  :src="item.coverImage || item.image || 'https://placehold.co/96x96/e2e8f0/64748b?text=商品'"
                  :alt="item.name"
                  class="w-full h-full object-cover"
                  @error="$event.target.src = 'https://placehold.co/96x96/e2e8f0/64748b?text=商品'"
                >
              </div>
              <div class="flex-1 min-w-0">
                <h3 class="font-semibold text-gray-800 text-lg truncate">{{ item.name }}</h3>
                <p class="text-xl font-bold bg-gradient-to-r from-blue-500 to-purple-500 bg-clip-text text-transparent mt-2">¥{{ item.price?.toFixed(2) || '0.00' }}</p>
              </div>
            </div>
            
            <div class="flex items-center justify-between w-full sm:w-auto gap-6">
              <div class="flex items-center gap-3 flex-shrink-0">
                <button
                  @click="updateQuantity(item.id, item.quantity - 1)"
                  :disabled="item.quantity <= 1"
                  class="w-12 h-12 bg-gray-100 hover:bg-gray-200 disabled:opacity-50 disabled:cursor-not-allowed rounded-xl flex items-center justify-center transition-colors"
                >
                  <svg class="w-6 h-6 text-gray-700" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 12H4"/>
                  </svg>
                </button>
                <span class="w-14 text-center font-semibold text-gray-800 text-lg">{{ item.quantity }}</span>
                <button
                  @click="updateQuantity(item.id, item.quantity + 1)"
                  class="w-12 h-12 bg-gray-100 hover:bg-gray-200 rounded-xl flex items-center justify-center transition-colors"
                >
                  <svg class="w-6 h-6 text-gray-700" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
                  </svg>
                </button>
              </div>

              <div class="text-right flex-shrink-0">
                <p class="text-2xl font-bold text-gray-800">¥{{ (item.price * item.quantity)?.toFixed(2) || '0.00' }}</p>
              </div>

              <button
                @click="removeItem(item.id)"
                class="w-12 h-12 text-gray-400 hover:text-red-500 transition-colors flex-shrink-0"
              >
                <svg class="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
                </svg>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="cartStore.cartItems.length > 0" class="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 shadow-lg px-6 py-6">
      <div class="max-w-7xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-6">
        <div class="flex items-center gap-4 w-full sm:w-auto">
          <input
            v-model="selectAll"
            type="checkbox"
            @change="toggleSelectAll"
            class="w-5 h-5 rounded border-gray-300 text-blue-500 focus:ring-blue-500"
          >
          <label class="font-semibold text-gray-700">全选</label>
          <span class="text-gray-600">已选 <span class="font-semibold text-gray-800">{{ selectedCount }}</span> 件</span>
        </div>
        <div class="flex items-center gap-8 w-full sm:w-auto">
          <div class="flex-1 sm:flex-none text-right">
            <span class="text-gray-600 text-lg">合计：</span>
            <span class="text-3xl font-bold bg-gradient-to-r from-blue-500 to-purple-500 bg-clip-text text-transparent">
              ¥{{ totalPrice.toFixed(2) }}
            </span>
          </div>
          <button
            @click="handleCheckout"
            :disabled="selectedCount === 0 || cartStore.loading"
            class="flex-1 sm:flex-none px-12 py-5 bg-gradient-to-r from-blue-500 to-purple-500 text-white font-semibold rounded-xl shadow-lg hover:shadow-xl hover:from-indigo-600 hover:to-purple-600 transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed text-lg"
          >
            {{ cartStore.loading ? '处理中...' : '结算' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '../stores/cartStore'

const router = useRouter()
const cartStore = useCartStore()

const selectedItems = ref([])

const selectAll = computed({
  get: () => cartStore.cartItems.length > 0 && selectedItems.value.length === cartStore.cartItems.length,
  set: (value) => {
    if (value) {
      selectedItems.value = cartStore.cartItems.map(item => item.id) || []
    } else {
      selectedItems.value = []
    }
  }
})

const selectedCount = computed(() => selectedItems.value.length)

const selectedCartItems = computed(() => {
  return cartStore.cartItems.filter(item => selectedItems.value.includes(item.id)) || []
})

const totalPrice = computed(() => {
  return selectedCartItems.value.reduce((sum, item) => {
    return sum + (item.price * item.quantity)
  }, 0)
})

const toggleSelectAll = () => {
  if (selectAll.value) {
    selectedItems.value = cartStore.cartItems.map(item => item.id) || []
  } else {
    selectedItems.value = []
  }
}

const updateQuantity = async (itemId, newQuantity) => {
  if (newQuantity < 1) return
  await cartStore.updateCartItem(itemId, newQuantity)
}

const removeItem = async (itemId) => {
  await cartStore.removeFromCart(itemId)
  selectedItems.value = selectedItems.value.filter(id => id !== itemId)
}

const handleCheckout = () => {
  if (selectedCount.value === 0) return
  router.push({
    path: '/order/confirm',
    query: {
      cartIds: selectedItems.value.join(',')
    }
  })
}

const goToLogin = () => {
  router.push('/auth')
}

const retryFetchCart = () => {
  cartStore.clearError()
  cartStore.fetchCart()
}

onMounted(() => {
  cartStore.fetchCart()
})
</script>
