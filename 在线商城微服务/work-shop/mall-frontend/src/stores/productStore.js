import { defineStore } from 'pinia'
import api from '../api/request'

const allowMockFallback = import.meta.env.DEV

// 本地模拟商品数据
const mockProducts = [
  {
    id: 1,
    name: '智能手表',
    price: 1299,
    stock: 50,
    coverImage: 'https://placehold.co/500x500/e2e8f0/64748b?text=智能手表',
    description: '多功能智能手表，支持心率监测、运动追踪等功能'
  },
  {
    id: 2,
    name: '无线耳机',
    price: 399,
    stock: 100,
    coverImage: 'https://placehold.co/500x500/e2e8f0/64748b?text=无线耳机',
    description: '高清音质无线耳机，佩戴舒适'
  },
  {
    id: 3,
    name: '便携充电宝',
    price: 199,
    stock: 200,
    coverImage: 'https://placehold.co/500x500/e2e8f0/64748b?text=便携充电宝',
    description: '20000mAh大容量，快速充电'
  },
  {
    id: 4,
    name: '机械键盘',
    price: 499,
    stock: 80,
    coverImage: 'https://placehold.co/500x500/e2e8f0/64748b?text=机械键盘',
    description: '青轴机械键盘，手感舒适，打字流畅'
  }
]

const mockCategories = [
  { id: 1, name: '电子产品' },
  { id: 2, name: '数码配件' },
  { id: 3, name: '智能设备' }
]

export const useProductStore = defineStore('product', {
  state: () => ({
    products: [],
    categories: [
      { id: null, name: '全部' }
    ],
    currentProduct: null,
    currentCategory: null,
    loading: false,
    error: null
  }),
  
  actions: {
    async fetchProducts(categoryId = null) {
      this.loading = true
      this.error = null
      try {
        let url = '/api/product/list'
        if (categoryId) {
          url += `?categoryId=${categoryId}`
        }
        const response = await api.get(url)
        const productList = response.data?.data?.list || response.data?.data || []
        // 映射后端字段到前端字段
        this.products = Array.isArray(productList) ? productList.map(product => ({
          id: product.id,
          name: product.productName || product.name,
          price: product.price,
          stock: product.stock,
          coverImage: product.productImage || product.coverImage || product.image,
          description: product.description,
          categoryId: product.categoryId
        })) : []
      } catch (error) {
        if (allowMockFallback) {
          console.error('获取商品列表失败，使用本地数据:', error)
          if (categoryId) {
            this.products = mockProducts.slice(0, 2)
          } else {
            this.products = mockProducts
          }
          this.error = null
        } else {
          this.products = []
          this.error = error.response?.data?.message || error.message || '获取商品列表失败'
        }
      } finally {
        this.loading = false
      }
    },
    
    async fetchProductDetail(id) {
      this.loading = true
      this.error = null
      try {
        const response = await api.get(`/api/product/detail/${id}`)
        const product = response.data?.data
        if (product) {
          // 映射后端字段到前端字段
          this.currentProduct = {
            id: product.id,
            name: product.productName || product.name,
            price: product.price,
            stock: product.stock,
            coverImage: product.productImage || product.coverImage || product.image,
            description: product.description,
            categoryId: product.categoryId
          }
        } else {
          throw new Error('Product data not found')
        }
      } catch (error) {
        if (allowMockFallback) {
          console.error('获取商品详情失败，使用本地数据:', error)
          this.currentProduct = mockProducts.find(p => p.id === parseInt(id)) || mockProducts[0]
          this.error = null
        } else {
          this.currentProduct = null
          this.error = error.response?.data?.message || error.message || '获取商品详情失败'
        }
      } finally {
        this.loading = false
      }
    },
    
    async fetchCategories() {
      this.loading = true
      this.error = null
      try {
        const response = await api.get('/api/product/categories')
        const categories = response.data?.data?.categories || []
        this.categories = [
          { id: null, name: '全部' },
          ...categories
        ]
      } catch (error) {
        if (allowMockFallback) {
          console.error('获取分类失败，使用本地数据:', error)
          this.categories = [
            { id: null, name: '全部' },
            ...mockCategories
          ]
          this.error = null
        } else {
          this.categories = [{ id: null, name: '全部' }]
          this.error = error.response?.data?.message || error.message || '获取分类失败'
        }
      } finally {
        this.loading = false
      }
    }
  }
})