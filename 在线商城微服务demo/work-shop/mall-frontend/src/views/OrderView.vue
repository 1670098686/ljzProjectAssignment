<template>
  <div class="bg-white rounded-2xl shadow-md border border-gray-100 p-8">
    <h1 class="text-3xl font-bold text-gray-800 mb-8">我的订单</h1>

    <div class="flex gap-3 mb-8 overflow-x-auto pb-2">
      <button
        v-for="(tab, index) in tabs"
        :key="tab.value"
        @click="currentTab = tab.value"
        :class="[
          'px-6 py-3 rounded-xl font-semibold whitespace-nowrap transition-all duration-300',
          currentTab === tab.value
            ? 'bg-gradient-to-r from-blue-500 to-purple-500 text-white shadow-lg'
            : 'bg-gray-50 text-gray-700 hover:bg-gray-100'
        ]"
      >
        {{ tab.label }}
      </button>
    </div>

    <div v-if="orderStore.loading" class="flex items-center justify-center py-24">
      <div class="w-14 h-14 border-4 border-blue-200 border-t-blue-500 rounded-full animate-spin"></div>
    </div>

    <div v-else-if="orderStore.error" class="text-center py-24">
      <p class="text-red-500 font-medium text-lg">{{ orderStore.error?.message || '加载失败' }}</p>
      <button @click="fetchOrders" class="mt-4 px-8 py-3 bg-gradient-to-r from-blue-500 to-purple-500 text-white font-semibold rounded-xl hover:from-indigo-600 hover:to-purple-600 transition-all duration-300">
        重试
      </button>
    </div>

    <div v-else-if="filteredOrders.length === 0" class="text-center py-24">
      <div class="w-32 h-32 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-6">
        <svg class="w-16 h-16 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2M9 5a2 2 0 0 0 2 2h2a2 2 0 0 0 2-2"/>
        </svg>
      </div>
      <p class="text-gray-600 font-medium text-lg mb-4">暂无订单</p>
      <router-link to="/" class="inline-flex items-center gap-2 px-8 py-4 bg-gradient-to-r from-blue-500 to-purple-500 text-white font-semibold rounded-xl hover:from-indigo-600 hover:to-purple-600 transition-all duration-300">
        去购物
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 8l4 4m0 0l-4 4m4-4H3"/>
        </svg>
      </router-link>
    </div>

    <div v-else class="space-y-6">
      <div
        v-for="order in filteredOrders"
        :key="order.id"
        class="border border-gray-100 rounded-xl overflow-hidden"
      >
        <div class="px-6 py-4 bg-gray-50 flex items-center justify-between">
          <div class="flex items-center gap-4">
            <span class="text-sm text-gray-600">订单号：</span>
            <span class="font-mono font-semibold text-gray-800">{{ order.orderNo }}</span>
          </div>
          <span :class="getStatusClass(order.status)" class="px-4 py-2 rounded-lg text-sm font-semibold">
            {{ getStatusLabel(order.status) }}
          </span>
        </div>

        <div class="px-6 py-5">
          <div class="flex items-center gap-5">
            <div class="w-20 h-20 bg-gray-50 rounded-lg flex items-center justify-center overflow-hidden flex-shrink-0">
              <img
                :src="order.items?.[0]?.image || 'https://placehold.co/80x80/e2e8f0/64748b?text=商品'"
                :alt="order.items?.[0]?.name"
                class="w-full h-full object-cover"
                @error="$event.target.src = 'https://placehold.co/80x80/e2e8f0/64748b?text=商品'"
              >
            </div>
            <div class="flex-1 min-w-0">
              <p class="font-semibold text-gray-800 text-lg truncate">{{ order.items?.[0]?.name }}</p>
              <p v-if="order.items?.length > 1" class="text-sm text-gray-500 mt-1">
                等 {{ order.items?.length }} 件商品
              </p>
            </div>
          </div>
        </div>

        <div class="px-6 py-4 bg-gray-50 flex items-center justify-between">
          <div class="text-gray-600">
            <span class="text-sm">下单时间：{{ formatDate(order.createdAt) }}</span>
          </div>
          <div class="flex items-center gap-4">
            <div class="text-right">
              <span class="text-gray-600">合计：</span>
              <span class="text-2xl font-bold bg-gradient-to-r from-blue-500 to-purple-500 bg-clip-text text-transparent">
                ¥{{ order.totalAmount?.toFixed(2) || '0.00' }}
              </span>
            </div>
            <router-link
              :to="'/order/' + order.id"
              class="px-5 py-2.5 bg-white text-gray-700 font-semibold border-2 border-gray-200 rounded-xl hover:bg-gray-50 transition-all duration-300"
            >
              查看详情
            </router-link>
            <button
              v-if="order.status === 'PENDING_PAYMENT'"
              @click="handlePay(order)"
              :disabled="paying[order.id]"
              class="px-5 py-2.5 bg-gradient-to-r from-blue-500 to-purple-500 text-white font-semibold rounded-xl hover:from-indigo-600 hover:to-purple-600 transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {{ paying[order.id] ? '支付中...' : '去支付' }}
            </button>
            <button
              v-if="order.status === 'PENDING_PAYMENT'"
              @click="handleCancel(order)"
              :disabled="cancelling[order.id]"
              class="px-5 py-2.5 bg-white text-red-500 font-semibold border-2 border-red-200 rounded-xl hover:bg-red-50 transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {{ cancelling[order.id] ? '取消中...' : '取消订单' }}
            </button>
            <button
              v-if="order.status === 'PAID' || order.status === 'SHIPPED'"
              @click="handleConfirm(order)"
              :disabled="confirming[order.id]"
              class="px-5 py-2.5 bg-gradient-to-r from-green-500 to-emerald-500 text-white font-semibold rounded-xl hover:from-green-600 hover:to-emerald-600 transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {{ confirming[order.id] ? '处理中...' : '完成订单' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useOrderStore } from '../stores/orderStore'

const orderStore = useOrderStore()
const paying = ref({})
const cancelling = ref({})
const confirming = ref({})

const tabs = [
  { label: '全部', value: 'all' },
  { label: '待支付', value: 'PENDING_PAYMENT' },
  { label: '待发货', value: 'PAID' },
  { label: '待收货', value: 'SHIPPED' },
  { label: '已完成', value: 'COMPLETED' }
]

const currentTab = ref('all')

const filteredOrders = computed(() => {
  if (currentTab.value === 'all') {
    return orderStore.orders
  }
  return orderStore.orders?.filter(order => order.status === currentTab.value) || []
})

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

const fetchOrders = () => {
  const status = currentTab.value === 'all' ? null : currentTab.value
  orderStore.fetchOrders(1, 20, status)
}

const handlePay = async (order) => {
  if (!order?.id || paying.value[order.id]) return
  
  paying.value[order.id] = true
  try {
    await orderStore.payOrder(order.id, 'online')
    alert('支付成功！')
    await fetchOrders()
  } catch (err) {
    console.error('支付失败:', err)
    alert('支付失败，请稍后重试')
  } finally {
    paying.value[order.id] = false
  }
}

const handleCancel = async (order) => {
  if (!order?.id || cancelling.value[order.id]) return
  if (!confirm('确定取消该订单吗？')) return

  cancelling.value[order.id] = true
  try {
    await orderStore.cancelOrder(order.id)
    alert('订单已取消')
    await fetchOrders()
  } catch (err) {
    console.error('取消订单失败:', err)
    alert(orderStore.error?.message || '取消失败，请稍后重试')
  } finally {
    cancelling.value[order.id] = false
  }
}

const handleConfirm = async (order) => {
  if (!order?.id || confirming.value[order.id]) return
  if (!confirm('确认将订单标记为已完成吗？')) return

  confirming.value[order.id] = true
  try {
    await orderStore.confirmReceipt(order.id)
    alert('订单已完成')
    await fetchOrders()
  } catch (err) {
    console.error('确认收货失败:', err)
    alert(orderStore.error?.message || '确认失败，请稍后重试')
  } finally {
    confirming.value[order.id] = false
  }
}

onMounted(() => {
  fetchOrders()
})

watch(currentTab, () => {
  fetchOrders()
})
</script>
