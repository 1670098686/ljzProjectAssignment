import { defineStore } from 'pinia'
import api from '../api/request'

export const useUserStore = defineStore('user', {
  state: () => ({
    user: JSON.parse(localStorage.getItem('user') || 'null'),
    token: localStorage.getItem('token') || null,
    loading: false,
    error: null,
    addresses: [],
    loadingAddresses: false
  }),
  
  getters: {
    isLoggedIn: (state) => !!state.token
  },
  
  actions: {
    initAuth() {
      // 确保 userId 在 localStorage 中
      if (this.user && this.user.id) {
        localStorage.setItem('userId', this.user.id)
      }
    },
    
    async login(username, password) {
      this.loading = true
      this.error = null
      try {
        const response = await api.post('/api/user/login', { username, password })
        const data = response.data?.data
        if (data) {
          const { token, user } = data
          this.token = token
          this.user = user
          localStorage.setItem('token', token)
          localStorage.setItem('user', JSON.stringify(user))
          if (user?.id) {
            localStorage.setItem('userId', user.id)
          }
        }
        return response.data
      } catch (error) {
        this.error = error.response?.data?.message || error.response?.data?.msg || '登录失败'
        throw error
      } finally {
        this.loading = false
      }
    },
    
    async register(username, password, phone) {
      this.loading = true
      this.error = null
      try {
        const response = await api.post('/api/user/register', { 
          username, 
          password, 
          phone 
        })
        return response.data
      } catch (error) {
        this.error = error.response?.data?.message || error.response?.data?.msg || '注册失败'
        throw error
      } finally {
        this.loading = false
      }
    },
    
    logout() {
      this.user = null
      this.token = null
      this.addresses = []
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      localStorage.removeItem('userId')
    },
    
    async fetchAddresses() {
      this.loadingAddresses = true
      try {
        const response = await api.get('/api/user/addresses')
        console.log('fetchAddresses 原始响应:', JSON.stringify(response.data))
        const data = response.data.data
        console.log('fetchAddresses data:', JSON.stringify(data))
        
        if (Array.isArray(data)) {
          this.addresses = data
        } else if (data && Array.isArray(data.addresses)) {
          this.addresses = data.addresses
        } else {
          this.addresses = []
        }
        
        console.log('fetchAddresses 最终 addresses:', JSON.stringify(this.addresses))
      } catch (error) {
        console.error('获取地址列表失败:', error)
        this.addresses = []
      } finally {
        this.loadingAddresses = false
      }
    },
    
    async addAddress(address) {
      try {
        console.log('addAddress 接收到的参数:', JSON.stringify(address))
        const payload = {
          name: address.name,
          phone: address.phone,
          province: address.province,
          city: address.city,
          district: address.district,
          detail: address.detail,
          isDefault: address.isDefault === true
        }
        console.log('addAddress 构造的 payload:', JSON.stringify(payload))
        const response = await api.post('/api/user/address', payload)
        console.log('addAddress 后端响应:', JSON.stringify(response.data))
        // 添加成功后，重新获取整个地址列表，确保数据一致
        await this.fetchAddresses()
        return response.data
      } catch (error) {
        console.error('添加地址失败:', error)
        throw error
      }
    },
    
    async updateAddress(id, address) {
      try {
        const payload = { ...address, id }
        const response = await api.put('/api/user/address', payload)
        // 更新成功后，重新获取整个地址列表，确保数据一致
        await this.fetchAddresses()
        return response.data
      } catch (error) {
        console.error('更新地址失败:', error)
        throw error
      }
    },
    
    async deleteAddress(id) {
      try {
        await api.delete(`/api/user/address/${id}`)
        this.addresses = this.addresses.filter(a => a.id !== id)
      } catch (error) {
        console.error('删除地址失败:', error)
        throw error
      }
    },
    
    async setDefaultAddress(id) {
      try {
        const address = this.addresses.find(a => a.id === id)
        if (!address) return
        const payload = {
          id: address.id,
          name: address.name,
          phone: address.phone,
          province: address.province,
          city: address.city,
          district: address.district,
          detail: address.detail,
          isDefault: true
        }
        const response = await api.put('/api/user/address', payload)
        // 设置默认地址成功后，重新获取整个地址列表，确保数据一致
        await this.fetchAddresses()
      } catch (error) {
        console.error('设置默认地址失败:', error)
        throw error
      }
    }
  }
})
