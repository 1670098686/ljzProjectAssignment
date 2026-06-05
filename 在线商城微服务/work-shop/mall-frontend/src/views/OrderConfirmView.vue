<template>
  <div class="pb-32">
    <h1 class="text-3xl font-bold text-gray-800 mb-8">确认订单</h1>

    <div v-if="loading" class="flex items-center justify-center py-24">
      <div class="w-14 h-14 border-4 border-blue-200 border-t-blue-500 rounded-full animate-spin"></div>
    </div>

    <div v-else-if="error" class="text-center py-24">
      <p class="text-red-500 font-medium text-lg">{{ error }}</p>
      <router-link to="/cart" class="mt-4 inline-flex items-center gap-2 px-8 py-3 bg-gradient-to-r from-blue-500 to-purple-500 text-white font-semibold rounded-xl hover:from-indigo-600 hover:to-purple-600 transition-all duration-300">
        返回购物车
      </router-link>
    </div>

    <div v-else class="space-y-6">
      <!-- 收货地址 -->
      <div class="bg-white rounded-2xl shadow-md border border-gray-100 p-8">
        <div class="flex items-center justify-between mb-6">
          <h2 class="text-xl font-bold text-gray-800">收货地址</h2>
          <router-link to="/user/center?tab=address" class="text-blue-500 hover:text-blue-600 font-medium">
            管理地址
          </router-link>
        </div>

        <div v-if="userStore.addresses.length > 0" class="space-y-4">
          <div
            v-for="address in userStore.addresses"
            :key="address.id"
            @click="selectAddress(address)"
            class="border-2 rounded-xl p-5 cursor-pointer transition-all duration-300"
            :class="selectedAddressId === address.id ? 'border-blue-500 bg-blue-50' : 'border-gray-100 hover:border-blue-300'"
          >
            <div class="flex items-start justify-between">
              <div>
                <div class="flex items-center gap-3 mb-2">
                  <span class="font-semibold text-gray-800 text-lg">{{ address.name }}</span>
                  <span class="text-gray-600">{{ address.phone }}</span>
                  <span v-if="address.isDefault" class="px-2 py-0.5 bg-blue-100 text-blue-700 text-sm rounded">默认</span>
                </div>
                <p class="text-gray-600">{{ address.fullAddress || address.address }}</p>
              </div>
              <div v-if="selectedAddressId === address.id" class="text-blue-500">
                <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
                </svg>
              </div>
            </div>
          </div>
        </div>

        <div v-else class="text-center py-12">
          <svg class="w-16 h-16 text-gray-400 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 0 1-2.827 0l-4.244-4.243a8 8 0 1 1 11.314 0z"/>
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 1 1-6 0 3 3 0 0 1 6 0z"/>
          </svg>
          <p class="text-gray-600 mb-4">暂无收货地址</p>
          <router-link to="/user/center?tab=address" class="inline-flex items-center gap-2 px-6 py-3 bg-gradient-to-r from-blue-500 to-purple-500 text-white font-semibold rounded-xl hover:from-indigo-600 hover:to-purple-600 transition-all duration-300">
            添加地址
          </router-link>
        </div>
      </div>

      <!-- 商品列表 -->
      <div class="bg-white rounded-2xl shadow-md border border-gray-100 p-8">
        <h2 class="text-xl font-bold text-gray-800 mb-6">商品清单</h2>
        <div class="space-y-4">
          <div
            v-for="item in orderItems"
            :key="item.id"
            class="flex items-center gap-5 p-4 bg-gray-50 rounded-xl"
          >
            <div class="w-20 h-20 bg-white rounded-lg flex items-center justify-center overflow-hidden flex-shrink-0">
              <img
                :src="item.coverImage || item.image || 'https://placehold.co/80x80/e2e8f0/64748b?text=商品'"
                :alt="item.name"
                class="w-full h-full object-cover"
                @error="$event.target.src = 'https://placehold.co/80x80/e2e8f0/64748b?text=商品'"
              >
            </div>
            <div class="flex-1 min-w-0">
              <h3 class="font-semibold text-gray-800 truncate">{{ item.name }}</h3>
              <p class="text-gray-500 text-sm mt-1">数量：{{ item.quantity }}</p>
            </div>
            <div class="text-right flex-shrink-0">
              <p class="text-xl font-bold bg-gradient-to-r from-blue-500 to-purple-500 bg-clip-text text-transparent">
                ¥{{ (item.price * item.quantity).toFixed(2) }}
              </p>
            </div>
          </div>
        </div>
      </div>

      <!-- 订单金额 -->
      <div class="bg-white rounded-2xl shadow-md border border-gray-100 p-8">
        <h2 class="text-xl font-bold text-gray-800 mb-6">订单金额</h2>
        <div class="space-y-3">
          <div class="flex items-center justify-between">
            <span class="text-gray-600">商品金额</span>
            <span class="text-gray-800 font-semibold">¥{{ totalPrice.toFixed(2) }}</span>
          </div>
          <div class="flex items-center justify-between">
            <span class="text-gray-600">运费</span>
            <span class="text-gray-800 font-semibold">¥0.00</span>
          </div>
          <div class="border-t border-gray-200 my-4 pt-4">
            <div class="flex items-center justify-between">
              <span class="text-gray-800 font-bold text-lg">应付金额</span>
              <span class="text-3xl font-bold bg-gradient-to-r from-blue-500 to-purple-500 bg-clip-text text-transparent">
                ¥{{ totalPrice.toFixed(2) }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部提交栏 -->
    <div class="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 shadow-lg px-6 py-6">
      <div class="max-w-7xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-6">
        <div class="flex items-center gap-4 w-full sm:w-auto">
          <span class="text-gray-600 text-lg">应付：</span>
          <span class="text-3xl font-bold bg-gradient-to-r from-blue-500 to-purple-500 bg-clip-text text-transparent">
            ¥{{ totalPrice.toFixed(2) }}
          </span>
        </div>
        <button
          @click="handleSubmitOrder"
          :disabled="!selectedAddressId || submitting"
          class="flex-1 sm:flex-none px-12 py-5 bg-gradient-to-r from-blue-500 to-purple-500 text-white font-semibold rounded-xl shadow-lg hover:shadow-xl hover:from-indigo-600 hover:to-purple-600 transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed text-lg"
        >
          {{ submitting ? '提交中...' : '提交订单' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useCartStore } from '../stores/cartStore'
import { useOrderStore } from '../stores/orderStore'
import { useUserStore } from '../stores/userStore'

const router = useRouter()
const route = useRoute()
const cartStore = useCartStore()
const orderStore = useOrderStore()
const userStore = useUserStore()

const loading = ref(false)
const submitting = ref(false)
const error = ref(null)
const selectedAddressId = ref(null)
const selectedCartIds = ref([])

const orderItems = computed(() => {
  return cartStore.cartItems.filter(item => selectedCartIds.value.includes(item.id))
})

const totalPrice = computed(() => {
  return orderItems.value.reduce((sum, item) => sum + (item.price * item.quantity), 0)
})

const selectAddress = (address) => {
  selectedAddressId.value = address.id
}

const extractSubmitErrorCode = (err) => {
  const directCode = Number(err?.businessCode ?? err?.response?.data?.code)
  if (Number.isFinite(directCode)) {
    return directCode
  }

  const messageCandidates = [
    err?.response?.data?.message,
    err?.message
  ].filter(Boolean)

  for (const text of messageCandidates) {
    if (typeof text !== 'string') continue
    const runtimeCodeMatch = text.match(/code\s*=\s*(\d{4})/i)
    if (runtimeCodeMatch) {
      const code = Number(runtimeCodeMatch[1])
      if (Number.isFinite(code)) return code
    }
  }

  return null
}

const resolveSubmitErrorMessage = (err) => {
  const code = extractSubmitErrorCode(err)
  if (code === 5401) {
    return '库存锁冲突，请稍后重试'
  }
  if (code === 5402) {
    return '库存服务响应超时，请稍后重试'
  }
  if (code === 5403) {
    return '库存不足，部分商品暂时无法下单'
  }
  return '提交订单失败，请稍后重试'
}

const handleSubmitOrder = async () => {
  if (!selectedAddressId.value || submitting.value) return

  submitting.value = true
  try {
    let order
    try {
      order = await orderStore.submitOrder(selectedCartIds.value, selectedAddressId.value)
    } catch (submitErr) {
      console.error('提交订单API失败:', submitErr)
      alert(resolveSubmitErrorMessage(submitErr))
      return
    }

    await cartStore.fetchCart()
    await orderStore.fetchOrders(1, 20, '')

    if (order && order.id) {
      try {
        await orderStore.payOrder(order.id, 'online')
        alert('支付成功！')
      } catch (payErr) {
        console.error('支付失败:', payErr)
        alert('订单创建成功，但支付失败，请在订单列表中重新支付')
      }
      router.push('/order')
    } else {
      alert('订单创建成功！')
      router.push('/order')
    }
  } catch (err) {
    console.error('处理订单失败:', err)
    alert('处理订单时发生错误，请稍后重试')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  loading.value = true
  try {
    const cartIds = route.query.cartIds
    if (cartIds) {
      selectedCartIds.value = cartIds.split(',').map(id => parseInt(id)).filter(id => !isNaN(id))
    }

    await cartStore.fetchCart()
    await userStore.fetchAddresses()

    if (userStore.addresses.length > 0) {
      const defaultAddress = userStore.addresses.find(a => a.isDefault) || userStore.addresses[0]
      selectedAddressId.value = defaultAddress.id
    }

    if (selectedCartIds.value.length === 0) {
      router.push('/cart')
    }
  } catch (err) {
    console.error('加载数据失败:', err)
    error.value = '加载数据失败，请稍后重试'
  } finally {
    loading.value = false
  }
})
</script>
