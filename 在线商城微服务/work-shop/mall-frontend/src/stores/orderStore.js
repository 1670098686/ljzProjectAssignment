import { defineStore } from 'pinia'
import api from '../api/request'

const resolveOrderId = (order) => {
  if (!order || typeof order !== 'object') return null
  const rawId = order.id ?? order.orderId
  if (rawId === null || rawId === undefined || rawId === '') return null
  const id = Number(rawId)
  return Number.isFinite(id) ? id : null
}

const normalizeOrderItem = (item = {}, index = 0) => ({
  id: item.id ?? `${index}`,
  productId: item.productId,
  name: item.name ?? item.productName ?? '商品',
  image: item.image ?? item.productImage ?? '',
  price: Number(item.price ?? 0),
  quantity: Number(item.quantity ?? 0),
  amount: Number(item.amount ?? (Number(item.price ?? 0) * Number(item.quantity ?? 0)))
})

const normalizeOrderSummary = (order = {}) => {
  const id = resolveOrderId(order)
  return {
    ...order,
    id,
    orderId: id,
    totalAmount: Number(order.totalAmount ?? 0),
    createdAt: order.createdAt ?? order.createTime ?? null,
    items: Array.isArray(order.items)
      ? order.items.map((item, index) => normalizeOrderItem(item, index))
      : []
  }
}

const normalizeOrderDetail = (order = {}) => {
  const normalized = normalizeOrderSummary(order)
  const address = order.address || {}
  return {
    ...normalized,
    paidAt: order.paidAt ?? order.paymentTime ?? null,
    receiverName: order.receiverName ?? address.receiver ?? '-',
    receiverPhone: order.receiverPhone ?? address.phone ?? '-',
    receiverAddress: order.receiverAddress ?? address.detailAddress ?? '-'
  }
}

export const useOrderStore = defineStore('order', {
  state: () => ({
    orders: [],
    currentOrder: null,
    loading: false,
    error: null
  }),
  
  actions: {
    async fetchOrders(page = 1, pageSize = 10, status = null) {
      this.loading = true
      this.error = null
      try {
        const params = { page, pageSize }
        if (status) {
          params.status = status
        }
        const response = await api.get('/api/order/list', { params })
        const rawOrders = response.data?.data?.list || []
        this.orders = rawOrders.map(normalizeOrderSummary)
      } catch (error) {
        this.error = {
          message: error.response?.data?.message || error.message || '请求失败',
          code: error.response?.status,
          details: error.response?.data
        }
      } finally {
        this.loading = false
      }
    },
    
    async fetchOrderDetail(id) {
      this.loading = true
      this.error = null
      try {
        const orderId = Number(id)
        if (!Number.isFinite(orderId) || orderId <= 0) {
          throw new Error('无效订单ID')
        }
        const response = await api.get(`/api/order/detail/${orderId}`)
        this.currentOrder = normalizeOrderDetail(response.data?.data || {})
      } catch (error) {
        this.error = {
          message: error.response?.data?.message || error.message || '请求失败',
          code: error.response?.status,
          details: error.response?.data
        }
      } finally {
        this.loading = false
      }
    },
    
    async submitOrder(cartIds, addressId = 1) {
      this.loading = true
      this.error = null
      try {
        const response = await api.post('/api/order/submit', {
          cartIds,
          addressId
        })
        return normalizeOrderDetail(response.data?.data || {})
      } catch (error) {
        this.error = {
          message: error.response?.data?.message || error.message || '请求失败',
          code: error.response?.status,
          details: error.response?.data
        }
        throw error
      } finally {
        this.loading = false
      }
    },
    
    async payOrder(orderId, paymentType = 'online') {
      this.loading = true
      this.error = null
      try {
        const id = Number(orderId)
        if (!Number.isFinite(id) || id <= 0) {
          throw new Error('无效订单ID')
        }
        const response = await api.post('/api/order/pay', {
          orderId: id.toString(),
          paymentType
        })
        await this.fetchOrders(1, 10)
        return response.data.data
      } catch (error) {
        this.error = {
          message: error.response?.data?.message || error.message || '请求失败',
          code: error.response?.status,
          details: error.response?.data
        }
        throw error
      } finally {
        this.loading = false
      }
    },

    async cancelOrder(orderId, reason = '用户主动取消') {
      this.loading = true
      this.error = null
      try {
        const id = Number(orderId)
        if (!Number.isFinite(id) || id <= 0) {
          throw new Error('无效订单ID')
        }
        const response = await api.post('/api/order/cancel', {
          orderId: id.toString(),
          reason
        })
        await this.fetchOrders(1, 10)
        return response.data.data
      } catch (error) {
        this.error = {
          message: error.response?.data?.message || error.message || '请求失败',
          code: error.response?.status,
          details: error.response?.data
        }
        throw error
      } finally {
        this.loading = false
      }
    },

    async confirmReceipt(orderId) {
      this.loading = true
      this.error = null
      try {
        const id = Number(orderId)
        if (!Number.isFinite(id) || id <= 0) {
          throw new Error('无效订单ID')
        }
        const response = await api.post('/api/order/confirm', {
          orderId: id.toString()
        })
        await this.fetchOrders(1, 10)
        return response.data.data
      } catch (error) {
        this.error = {
          message: error.response?.data?.message || error.message || '请求失败',
          code: error.response?.status,
          details: error.response?.data
        }
        throw error
      } finally {
        this.loading = false
      }
    }
  }
})