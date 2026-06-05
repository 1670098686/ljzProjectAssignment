<template>
  <div>
    <button
      @click="$router.back()"
      class="flex items-center gap-2 text-gray-600 hover:text-blue-600 transition-colors mb-6"
    >
      <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
      </svg>
      返回订单列表
    </button>

    <div v-if="orderStore.loading" class="flex items-center justify-center py-20">
      <div class="w-12 h-12 border-4 border-blue-200 border-t-blue-500 rounded-full animate-spin"></div>
    </div>

    <div v-else-if="orderStore.error" class="text-center py-20">
      <p class="text-red-500 font-medium">{{ orderStore.error?.message || '加载失败' }}</p>
      <button @click="fetchOrderDetail" class="mt-4 px-6 py-2 bg-blue-500 text-white rounded-xl hover:bg-blue-600 transition-colors">
        重试
      </button>
    </div>

    <div v-else-if="!order" class="text-center py-20">
      <div class="w-20 h-20 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-6">
        <svg class="w-10 h-10 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.172 16.172a4 4 0 015.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
        </svg>
      </div>
      <p class="text-gray-600 font-medium">订单不存在</p>
    </div>

    <div v-else class="space-y-6 lg:space-y-8">
      <div class="bg-white rounded-2xl shadow-md border border-gray-100 p-6 lg:p-8">
        <div class="flex items-start justify-between mb-8">
          <div>
            <p class="text-sm text-gray-600 mb-2">订单号</p>
            <p class="font-mono font-semibold text-gray-800 text-lg">{{ order.orderNo }}</p>
          </div>
          <span :class="getStatusClass(order.status)" class="px-5 py-2.5 rounded-xl text-base font-semibold">
            {{ getStatusLabel(order.status) }}
          </span>
        </div>
        
        <div class="grid grid-cols-1 md:grid-cols-2 gap-8 pt-8 border-t border-gray-100">
          <div>
            <p class="text-sm text-gray-600 mb-2">下单时间</p>
            <p class="text-gray-800 text-lg">{{ formatDate(order.createdAt) }}</p>
          </div>
          <div v-if="order.paidAt">
            <p class="text-sm text-gray-600 mb-2">支付时间</p>
            <p class="text-gray-800 text-lg">{{ formatDate(order.paidAt) }}</p>
          </div>
        </div>
      </div>

      <div class="bg-white rounded-2xl shadow-md border border-gray-100 p-6 lg:p-8">
        <h2 class="text-xl font-bold text-gray-800 mb-6">收货信息</h2>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <p class="text-sm text-gray-600 mb-2">收货人</p>
            <p class="text-gray-800 font-medium text-lg">{{ order.receiverName || '-' }}</p>
          </div>
          <div>
            <p class="text-sm text-gray-600 mb-2">联系电话</p>
            <p class="text-gray-800 font-medium text-lg">{{ order.receiverPhone || '-' }}</p>
          </div>
          <div class="md:col-span-2">
            <p class="text-sm text-gray-600 mb-2">收货地址</p>
            <p class="text-gray-800 text-lg">{{ order.receiverAddress || '-' }}</p>
          </div>
        </div>
      </div>

      <div class="bg-white rounded-2xl shadow-md border border-gray-100 p-6 lg:p-8">
        <h2 class="text-xl font-bold text-gray-800 mb-6">商品清单</h2>
        <div class="space-y-4">
          <div
            v-for="item in order.items"
            :key="item.id"
            class="flex items-center gap-6 py-6 border-b border-gray-100 last:border-b-0 last:pb-0"
          >
            <div class="w-24 h-24 bg-gray-50 rounded-xl flex items-center justify-center overflow-hidden flex-shrink-0">
              <img
                :src="item.image || 'https://placehold.co/96x96/e2e8f0/64748b?text=商品'"
                :alt="item.name"
                class="w-full h-full object-cover"
                @error="$event.target.src = 'https://placehold.co/96x96/e2e8f0/64748b?text=商品'"
              >
            </div>
            <div class="flex-1 min-w-0">
              <p class="font-semibold text-gray-800 text-lg truncate">{{ item.name }}</p>
              <p class="text-base text-gray-500 mt-2">¥{{ item.price?.toFixed(2) || '0.00' }} × {{ item.quantity }}</p>
            </div>
            <div class="text-right flex-shrink-0">
              <p class="font-semibold text-gray-800 text-xl">¥{{ (item.price * item.quantity)?.toFixed(2) || '0.00' }}</p>
              <button
                v-if="canReviewItem(item)"
                @click="openReviewModal(item)"
                class="mt-3 px-4 py-2 bg-gradient-to-r from-amber-500 to-orange-500 text-white rounded-lg text-sm font-semibold hover:from-amber-600 hover:to-orange-600 transition-all duration-300"
              >
                评价商品
              </button>
              <p
                v-else-if="order.status === 'COMPLETED'"
                class="mt-3 text-sm text-green-600 font-medium"
              >
                已评价或不可评价
              </p>
            </div>
          </div>
        </div>
      </div>

      <div class="bg-white rounded-2xl shadow-md border border-gray-100 p-6 lg:p-8">
        <h2 class="text-xl font-bold text-gray-800 mb-6">金额明细</h2>
        <div class="space-y-4 max-w-md">
          <div class="flex justify-between text-lg">
            <span class="text-gray-600">商品总额</span>
            <span class="text-gray-800">¥{{ order.totalAmount?.toFixed(2) || '0.00' }}</span>
          </div>
          <div class="flex justify-between text-lg">
            <span class="text-gray-600">运费</span>
            <span class="text-gray-800">¥0.00</span>
          </div>
          <div class="flex justify-between pt-5 border-t border-gray-100">
            <span class="text-xl font-semibold text-gray-800">应付总额</span>
            <span class="text-3xl font-bold bg-gradient-to-r from-blue-500 to-purple-500 bg-clip-text text-transparent">
              ¥{{ order.totalAmount?.toFixed(2) || '0.00' }}
            </span>
          </div>
        </div>
      </div>

      <div class="flex gap-4 max-w-lg">
        <button
          v-if="order.status === 'PENDING_PAYMENT'"
          @click="handlePay"
          :disabled="paying"
          class="flex-1 px-8 py-4 bg-gradient-to-r from-blue-500 to-purple-500 text-white font-semibold rounded-xl shadow-lg hover:shadow-xl hover:from-indigo-600 hover:to-purple-600 transition-all duration-300 text-lg disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {{ paying ? '支付中...' : '立即支付' }}
        </button>
        <button
          v-if="order.status === 'PENDING_PAYMENT'"
          @click="handleCancel"
          :disabled="cancelling"
          class="flex-1 px-8 py-4 bg-white text-red-500 font-semibold border-2 border-red-200 rounded-xl hover:bg-red-50 transition-all duration-300 text-lg disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {{ cancelling ? '取消中...' : '取消订单' }}
        </button>
        <button
          v-if="order.status === 'PAID' || order.status === 'SHIPPED'"
          @click="handleConfirmReceipt"
          :disabled="confirming"
          class="flex-1 px-8 py-4 bg-gradient-to-r from-green-500 to-emerald-500 text-white font-semibold rounded-xl shadow-lg hover:shadow-xl hover:from-green-600 hover:to-emerald-600 transition-all duration-300 text-lg disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {{ confirming ? '处理中...' : '完成订单' }}
        </button>
        <button
          @click="$router.back()"
          class="flex-1 px-8 py-4 bg-white text-gray-700 font-semibold border-2 border-gray-200 rounded-xl hover:bg-gray-50 transition-all duration-300 text-lg"
        >
          返回订单列表
        </button>
      </div>
    </div>
  </div>

  <div v-if="showReviewModal" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50" @click.self="closeReviewModal">
    <div class="bg-white rounded-2xl shadow-xl w-full max-w-lg mx-4 overflow-hidden">
      <div class="px-6 py-4 border-b border-gray-100 flex items-center justify-between">
        <h3 class="text-lg font-bold text-gray-800">评价商品</h3>
        <button @click="closeReviewModal" class="text-gray-400 hover:text-gray-600">
          <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>

      <div class="p-6 space-y-4">
        <p class="text-gray-800 font-semibold">{{ reviewTarget?.name || '商品' }}</p>

        <div>
          <label class="block text-sm text-gray-700 mb-2">评分</label>
          <div class="flex gap-2">
            <button
              v-for="n in 5"
              :key="n"
              @click="reviewRating = n"
              class="w-10 h-10 rounded-lg border transition-all duration-200"
              :class="n <= reviewRating ? 'bg-amber-500 border-amber-500 text-white' : 'bg-white border-gray-200 text-gray-500 hover:border-amber-400'"
            >
              {{ n }}
            </button>
          </div>
        </div>

        <div>
          <label class="block text-sm text-gray-700 mb-2">评价内容</label>
          <textarea
            v-model="reviewContent"
            rows="4"
            class="w-full px-4 py-3 border-2 border-gray-200 rounded-lg focus:outline-none focus:border-amber-400"
            placeholder="请填写你的使用感受"
          ></textarea>
        </div>
      </div>

      <div class="px-6 py-4 border-t border-gray-100 flex gap-3">
        <button
          @click="closeReviewModal"
          class="flex-1 px-4 py-2.5 bg-gray-100 text-gray-700 rounded-lg font-semibold"
        >
          取消
        </button>
        <button
          @click="submitReview"
          :disabled="reviewSubmitting"
          class="flex-1 px-4 py-2.5 bg-gradient-to-r from-amber-500 to-orange-500 text-white rounded-lg font-semibold disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {{ reviewSubmitting ? '提交中...' : '提交评价' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../api/request'
import { useOrderStore } from '../stores/orderStore'

const route = useRoute()
const router = useRouter()
const orderStore = useOrderStore()

const order = computed(() => orderStore.currentOrder)
const paying = ref(false)
const cancelling = ref(false)
const confirming = ref(false)
const pendingReviewKeys = ref(new Set())
const showReviewModal = ref(false)
const reviewTarget = ref(null)
const reviewRating = ref(5)
const reviewContent = ref('')
const reviewSubmitting = ref(false)

const getStatusLabel = (status) => {
  const statusMap = {
    'PENDING_PAYMENT': '待支付',
    'PAID': '待发货',
    'SHIPPED': '待收货',
    'COMPLETED': '已完成',
    'CANCELLED': '已取消'
  }
  return statusMap[status] || status
}

const getStatusClass = (status) => {
  const classMap = {
    'PENDING_PAYMENT': 'bg-yellow-100 text-yellow-700',
    'PAID': 'bg-blue-100 text-blue-700',
    'SHIPPED': 'bg-purple-100 text-purple-700',
    'COMPLETED': 'bg-green-100 text-green-700',
    'CANCELLED': 'bg-gray-100 text-gray-500'
  }
  return classMap[status] || 'bg-gray-100 text-gray-500'
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const fetchOrderDetail = async () => {
  const orderId = route.params.id
  await orderStore.fetchOrderDetail(orderId)
  await fetchPendingReviews()
}

const fetchPendingReviews = async () => {
  if (!order.value?.orderId || order.value.status !== 'COMPLETED') {
    pendingReviewKeys.value = new Set()
    return
  }
  try {
    const response = await api.get('/api/review/pending-list', {
      params: { page: 1, pageSize: 200 }
    })
    const list = response.data?.data?.list || []
    const keys = new Set()
    list.forEach(item => {
      const key = `${item.orderId}_${item.productId}`
      keys.add(key)
    })
    pendingReviewKeys.value = keys
  } catch (err) {
    console.error('加载待评价列表失败:', err)
    pendingReviewKeys.value = new Set()
  }
}

const canReviewItem = (item) => {
  if (!order.value || order.value.status !== 'COMPLETED') return false
  const key = `${order.value.orderId}_${item.productId}`
  return pendingReviewKeys.value.has(key)
}

const openReviewModal = (item) => {
  reviewTarget.value = item
  reviewRating.value = 5
  reviewContent.value = ''
  showReviewModal.value = true
}

const closeReviewModal = () => {
  showReviewModal.value = false
  reviewTarget.value = null
}

const submitReview = async () => {
  if (!order.value?.orderId || !reviewTarget.value?.productId) return
  const content = reviewContent.value.trim()
  if (!content) {
    alert('请输入评价内容')
    return
  }

  reviewSubmitting.value = true
  try {
    await api.post('/api/review/add', {
      orderId: order.value.orderId,
      productId: reviewTarget.value.productId,
      rating: reviewRating.value,
      content,
      images: []
    })
    alert('评价成功')
    closeReviewModal()
    await fetchPendingReviews()
  } catch (err) {
    console.error('评价失败:', err)
    alert(err.response?.data?.message || '评价失败，请稍后重试')
  } finally {
    reviewSubmitting.value = false
  }
}

const handlePay = async () => {
  if (paying.value || !order.value?.orderId) return
  
  paying.value = true
  try {
    await orderStore.payOrder(order.value.orderId, 'online')
    alert('支付成功！')
    await fetchOrderDetail()
  } catch (err) {
    console.error('支付失败:', err)
    alert('支付失败，请稍后重试')
  } finally {
    paying.value = false
  }
}

const handleCancel = async () => {
  if (cancelling.value || !order.value?.orderId) return
  if (!confirm('确定取消该订单吗？')) return

  cancelling.value = true
  try {
    await orderStore.cancelOrder(order.value.orderId)
    alert('订单已取消')
    await fetchOrderDetail()
  } catch (err) {
    console.error('取消订单失败:', err)
    alert(orderStore.error?.message || '取消失败，请稍后重试')
  } finally {
    cancelling.value = false
  }
}

const handleConfirmReceipt = async () => {
  if (confirming.value || !order.value?.orderId) return
  if (!confirm('确认将订单标记为已完成吗？')) return

  confirming.value = true
  try {
    await orderStore.confirmReceipt(order.value.orderId)
    alert('订单已完成')
    await fetchOrderDetail()
  } catch (err) {
    console.error('确认收货失败:', err)
    alert(orderStore.error?.message || '确认失败，请稍后重试')
  } finally {
    confirming.value = false
  }
}

onMounted(() => {
  fetchOrderDetail()
})
</script>
