<template>
  <div class="min-h-screen bg-[#f8fafc] flex flex-col">
    <header class="sticky top-0 z-50 bg-white shadow-sm h-16 flex-shrink-0">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-full flex items-center justify-between gap-4">
        <router-link to="/" class="flex items-center gap-3 flex-shrink-0">
          <div class="flex items-center justify-center w-10 h-10 rounded-xl bg-gradient-to-r from-blue-500 to-purple-500">
            <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5"/>
            </svg>
          </div>
          <span class="text-xl font-bold bg-gradient-to-r from-blue-500 to-purple-500 bg-clip-text text-transparent">在线商城</span>
        </router-link>
        
        <div class="hidden lg:flex flex-1 max-w-2xl mx-8">
          <div class="relative w-full">
            <input
              type="text"
              placeholder="搜索商品..."
              class="w-full px-4 py-2.5 pl-10 h-10 bg-gray-50 border-2 border-gray-200 rounded-xl text-gray-700 placeholder-gray-400 focus:outline-none focus:border-blue-400 focus:bg-white focus:ring-4 focus:ring-blue-100 transition-all duration-300"
            >
            <svg class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
            </svg>
          </div>
        </div>
        
        <div class="flex items-center gap-3 sm:gap-4">
          <template v-if="userStore.isLoggedIn">
            <router-link to="/cart" class="relative p-2.5 hover:bg-gray-100 rounded-xl transition-colors">
              <svg class="w-6 h-6 text-gray-700" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5"/>
              </svg>
              <span v-if="cartCount > 0" class="absolute -top-1 -right-1 min-w-5 h-5 bg-gradient-to-r from-blue-500 to-purple-500 text-white text-xs font-semibold rounded-full flex items-center justify-center">
                {{ cartCount > 99 ? '99+' : cartCount }}
              </span>
            </router-link>
          </template>
          
          <template v-if="userStore.isLoggedIn">
            <router-link to="/user/center" class="hidden sm:flex items-center gap-2 px-4 py-2 hover:bg-gray-100 rounded-xl transition-colors">
              <div class="w-9 h-9 rounded-full bg-gradient-to-r from-blue-500 to-purple-500 flex items-center justify-center text-white font-semibold text-base">
                {{ userStore.user?.username?.charAt(0) || 'U' }}
              </div>
              <span class="text-gray-700 font-medium">{{ userStore.user?.username }}</span>
            </router-link>
            <router-link to="/user/center" class="sm:hidden p-2.5 hover:bg-gray-100 rounded-xl transition-colors">
              <div class="w-8 h-8 rounded-full bg-gradient-to-r from-blue-500 to-purple-500 flex items-center justify-center text-white font-semibold text-sm">
                {{ userStore.user?.username?.charAt(0) || 'U' }}
              </div>
            </router-link>
          </template>
          <template v-else>
            <router-link to="/auth" class="px-5 sm:px-6 py-2.5 bg-gradient-to-r from-blue-500 to-purple-500 text-white font-semibold rounded-xl shadow-lg hover:shadow-xl hover:from-blue-600 hover:to-purple-600 transition-all duration-300 text-sm">
              登录/注册
            </router-link>
          </template>
        </div>
      </div>
    </header>
    
    <main class="flex-1 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-8 pb-8">
      <router-view />
    </main>
    
    <footer class="bg-[#1e293b] text-white h-20 flex items-center flex-shrink-0">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 w-full flex flex-col sm:flex-row items-center justify-between gap-2">
        <div class="flex items-center gap-2">
          <div class="flex items-center justify-center w-8 h-8 rounded-lg bg-gradient-to-r from-blue-500 to-purple-500">
            <svg class="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5"/>
            </svg>
          </div>
          <span class="font-semibold">在线商城</span>
        </div>
        <p class="text-gray-400 text-xs sm:text-sm">© 2026 在线商城系统. All rights reserved.</p>
      </div>
    </footer>
    
    <Toast ref="toastRef" />
  </div>
</template>

<script setup>
import { computed, ref, provide, onMounted } from 'vue'
import { useUserStore } from './stores/userStore'
import { useCartStore } from './stores/cartStore'
import Toast from './components/Toast.vue'

const userStore = useUserStore()
const cartStore = useCartStore()
const toastRef = ref(null)

onMounted(() => {
  userStore.initAuth()
})

provide('toast', {
  success: (msg) => toastRef.value?.success(msg),
  error: (msg) => toastRef.value?.error(msg),
  warning: (msg) => toastRef.value?.warning(msg),
  info: (msg) => toastRef.value?.info(msg)
})

const cartCount = computed(() => {
  if (!cartStore.cartItems || !Array.isArray(cartStore.cartItems)) {
    return 0
  }
  return cartStore.cartItems.reduce((total, item) => total + (item.quantity || 0), 0)
})
</script>

<style>
</style>
