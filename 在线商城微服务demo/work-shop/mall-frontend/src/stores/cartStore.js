import { defineStore } from 'pinia'
import api from '../api/request'

const allowMockFallback = import.meta.env.DEV

// 本地模拟购物车数据
const getMockCart = () => {
  try {
    const cart = JSON.parse(localStorage.getItem('mockCart') || '[]')
    return Array.isArray(cart) ? cart : []
  } catch {
    return []
  }
}

const saveMockCart = (cart) => {
  localStorage.setItem('mockCart', JSON.stringify(cart))
}

export const useCartStore = defineStore('cart', {
  state: () => ({
    cartItems: getMockCart(),
    loading: false,
    error: null
  }),
  
  getters: {
    totalPrice: (state) => {
      return state.cartItems.reduce((total, item) => {
        return total + (item.totalPrice || item.price * item.quantity)
      }, 0)
    },
    totalItems: (state) => {
      return state.cartItems.reduce((total, item) => {
        return total + item.quantity
      }, 0)
    }
  },
  
  actions: {
    async fetchCart() {
      this.loading = true
      this.error = null
      try {
        const response = await api.get('/api/cart/list')
        const cartItems = response.data?.data?.cartItems || response.data?.data || []
        // 映射后端字段到前端字段
        this.cartItems = Array.isArray(cartItems) ? cartItems.map(item => ({
          id: item.cartId || item.id,
          productId: item.productId,
          name: item.productName || item.name,
          price: item.price,
          quantity: item.quantity,
          image: item.productImage || item.image,
          coverImage: item.productImage || item.coverImage || item.image,
          checked: item.checked,
          stock: item.stock,
          totalPrice: item.totalPrice
        })) : []
        saveMockCart(this.cartItems)
      } catch (error) {
        if (allowMockFallback) {
          console.error('获取购物车失败，使用本地数据:', error)
          this.cartItems = getMockCart()
          this.error = null
        } else {
          this.error = error.response?.data?.message || error.message || '获取购物车失败'
        }
      } finally {
        this.loading = false
      }
    },
    
    async addToCart(productId, quantity, product = null) {
      this.loading = true
      this.error = null
      try {
        await api.post('/api/cart/add', {
          productId,
          quantity
        })
        await this.fetchCart()
      } catch (error) {
        if (allowMockFallback) {
          console.error('添加购物车失败，使用本地数据:', error)
          const existingIndex = this.cartItems.findIndex(item => item.productId === productId)
          if (existingIndex >= 0) {
            this.cartItems[existingIndex].quantity += quantity
          } else {
            this.cartItems.push({
              id: Date.now(),
              productId,
              quantity,
              name: product?.name || `商品${productId}`,
              price: product?.price || 99,
              image: product?.coverImage || ''
            })
          }
          saveMockCart(this.cartItems)
        } else {
          this.error = error.response?.data?.message || error.message || '添加购物车失败'
          throw error
        }
      } finally {
        this.loading = false
      }
    },
    
    async updateCartItem(cartId, quantity) {
      this.loading = true
      this.error = null
      try {
        await api.put('/api/cart/update', {
          cartId,
          quantity
        })
        await this.fetchCart()
      } catch (error) {
        if (allowMockFallback) {
          console.error('更新购物车失败，使用本地数据:', error)
          const item = this.cartItems.find(item => item.id === cartId)
          if (item) {
            item.quantity = quantity
            saveMockCart(this.cartItems)
          }
        } else {
          this.error = error.response?.data?.message || error.message || '更新购物车失败'
          throw error
        }
      } finally {
        this.loading = false
      }
    },
    
    async removeFromCart(cartId) {
      this.loading = true
      this.error = null
      try {
        await api.delete(`/api/cart/${cartId}`)
        await this.fetchCart()
      } catch (error) {
        if (allowMockFallback) {
          console.error('删除购物车失败，使用本地数据:', error)
          this.cartItems = this.cartItems.filter(item => item.id !== cartId)
          saveMockCart(this.cartItems)
        } else {
          this.error = error.response?.data?.message || error.message || '删除购物车失败'
          throw error
        }
      } finally {
        this.loading = false
      }
    },
    
    clearError() {
      this.error = null
    }
  }
})