<template>
  <div class="min-h-screen bg-gradient-to-br from-blue-50 via-indigo-50 to-purple-50 flex items-start justify-center pt-8 p-4">
    <div class="w-[1280px] flex gap-6">
      <div class="w-full lg:w-64 flex-shrink-0">
        <div class="bg-white rounded-2xl shadow-md border border-gray-100 p-6 flex flex-col">
          <div class="flex items-center gap-4 p-4 mb-6 bg-gradient-to-r from-blue-500 to-purple-500 rounded-xl flex-shrink-0">
            <div class="w-12 h-12 bg-white/20 rounded-full flex items-center justify-center">
              <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"/>
              </svg>
            </div>
            <div>
              <p class="text-white font-semibold">{{ userStore.user?.username || '用户' }}</p>
              <p class="text-white/80 text-sm">欢迎回来</p>
            </div>
          </div>
          
          <div class="space-y-2 flex-1">
            <button
              v-for="item in menuItems"
              :key="item.key"
              @click="activeTab = item.key"
              :class="[
                'w-full flex items-center gap-3 px-4 py-3 rounded-lg transition-all duration-300 text-sm',
                activeTab === item.key
                  ? 'bg-gradient-to-r from-blue-500 to-purple-500 text-white shadow-md'
                  : 'text-gray-600 hover:bg-gray-50'
              ]"
            >
              <template v-if="item.icon === 'profile'">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"/>
                </svg>
              </template>
              <template v-else-if="item.icon === 'orders'">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2M9 5a2 2 0 0 1 2-2h2a2 2 0 0 1 2 2M9 5v2h6V5m-9 6h6m-6 4h6m2 5v1a2 2 0 0 1-2 2h-4a2 2 0 0 1-2-2v-1"/>
                </svg>
              </template>
              <template v-else-if="item.icon === 'favorites'">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 0 0 0 6.364L12 20.364l7.682-7.682a4.5 4.5 0 0 0-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 0 0-6.364 0z"/>
                </svg>
              </template>
              <template v-else-if="item.icon === 'address'">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 0 1-2.827 0l-4.244-4.243a8 8 0 1 1 11.314 0z"/>
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 1 1-6 0 3 3 0 0 16 0z"/>
                </svg>
              </template>
              <span class="font-medium">{{ item.label }}</span>
            </button>
            
            <div class="my-4 border-t border-gray-100"></div>
            
            <button
              @click="handleLogout"
              class="w-full flex items-center gap-3 px-4 py-3 rounded-lg text-red-500 hover:bg-red-50 transition-all duration-300 text-sm"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4-4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"/>
              </svg>
              <span class="font-medium">退出登录</span>
            </button>
          </div>
        </div>
      </div>

      <div class="flex-1 min-w-0">
        <div v-if="activeTab === 'profile'" class="bg-white rounded-2xl shadow-md border border-gray-100 p-8 min-h-[750px]">
          <h2 class="text-2xl font-bold text-gray-800 mb-6">个人资料</h2>
          
          <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <label class="block text-sm font-semibold text-gray-700 mb-2">用户名</label>
              <input
                v-model="profileForm.username"
                type="text"
                class="w-full px-4 py-3 bg-gray-50 border-2 border-gray-200 rounded-lg text-gray-700 placeholder-gray-400 focus:outline-none focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-50 transition-all duration-300 text-sm"
                disabled
              >
            </div>
            
            <div>
              <label class="block text-sm font-semibold text-gray-700 mb-2">手机号</label>
              <input
                v-model="profileForm.phone"
                type="text"
                class="w-full px-4 py-3 bg-gray-50 border-2 border-gray-200 rounded-lg text-gray-700 placeholder-gray-400 focus:outline-none focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-50 transition-all duration-300 text-sm"
                placeholder="请输入手机号"
              >
            </div>
          </div>
          
          <div class="mt-8">
            <button
              @click="handleSaveProfile"
              class="px-8 py-3 bg-gradient-to-r from-blue-500 to-purple-500 text-white font-semibold rounded-lg shadow-md hover:shadow-lg hover:from-indigo-600 hover:to-purple-600 transition-all duration-300 text-sm"
            >
              保存修改
            </button>
          </div>
        </div>

        <div v-else-if="activeTab === 'orders'" class="bg-white rounded-2xl shadow-md border border-gray-100 p-8 min-h-[750px]">
          <h2 class="text-2xl font-bold text-gray-800 mb-6">我的订单</h2>
          
          <div class="flex gap-3 mb-6 overflow-x-auto pb-2 flex-shrink-0">
            <button
              v-for="(tab, index) in orderTabs"
              :key="tab.value"
              @click="orderTab = tab.value"
              :class="[
                'px-6 py-2.5 rounded-lg font-semibold whitespace-nowrap transition-all duration-300 text-sm',
                orderTab === tab.value
                  ? 'bg-gradient-to-r from-blue-500 to-purple-500 text-white shadow-md'
                  : 'bg-gray-50 text-gray-700 hover:bg-gray-100'
              ]"
            >
              {{ tab.label }}
            </button>
          </div>

          <div v-if="orderStore.loading" class="flex items-center justify-center py-20">
            <div class="w-12 h-12 border-4 border-blue-200 border-t-blue-500 rounded-full animate-spin"></div>
          </div>

          <div v-else-if="filteredOrders.length === 0" class="text-center py-20">
            <div class="w-32 h-32 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-6">
              <svg class="w-16 h-16 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2M9 5a2 2 0 0 1 2-2h2a2 2 0 0 1 2 2M9 5v2h6V5m-9 6h6m-6 4h6m2 5v1a2 2 0 0 1-2 2h-4a2 2 0 0 1-2-2v-1"/>
              </svg>
            </div>
            <p class="text-gray-600 font-medium">暂无订单</p>
          </div>

          <div v-else class="space-y-4">
            <div
              v-for="order in filteredOrders"
              :key="order.id"
              class="border border-gray-100 rounded-lg overflow-hidden"
            >
              <div class="px-6 py-4 bg-gray-50 flex items-center justify-between">
                <div class="flex items-center gap-4">
                  <span class="text-sm text-gray-600">订单号：</span>
                  <span class="font-mono font-semibold text-gray-800 text-sm">{{ order.orderNo }}</span>
                </div>
                <span :class="getOrderStatusClass(order.status)" class="px-4 py-1.5 rounded-md text-sm font-semibold">
                  {{ getOrderStatusLabel(order.status) }}
                </span>
              </div>

              <div class="px-6 py-4">
                <div class="flex items-center gap-4">
                  <div class="w-20 h-20 bg-gray-50 rounded-md flex items-center justify-center overflow-hidden flex-shrink-0">
                    <img
                      :src="order.items?.[0]?.image || 'https://placehold.co/80x80/e2e8f0/64748b?text=商品'"
                      :alt="order.items?.[0]?.name"
                      class="w-full h-full object-cover"
                      @error="$event.target.src = 'https://placehold.co/80x80/e2e8f0/64748b?text=商品'"
                    >
                  </div>
                  <div class="flex-1 min-w-0">
                    <p class="font-semibold text-gray-800 truncate">{{ order.items?.[0]?.name }}</p>
                    <p v-if="order.items?.length > 1" class="text-sm text-gray-500 mt-1">
                      等 {{ order.items?.length }} 件商品
                    </p>
                  </div>
                </div>
              </div>

              <div class="px-6 py-4 bg-gray-50 flex items-center justify-between">
                <div class="text-gray-600 text-sm">
                  合计：<span class="text-xl font-bold bg-gradient-to-r from-blue-500 to-purple-500 bg-clip-text text-transparent">
                    ¥{{ order.totalAmount?.toFixed(2) || '0.00' }}
                  </span>
                </div>
                <div class="flex items-center gap-3">
                  <router-link
                    :to="'/order/' + order.id"
                    class="px-6 py-2 bg-white text-gray-700 font-semibold border-2 border-gray-200 rounded-lg hover:bg-gray-50 transition-all duration-300 text-sm"
                  >
                    查看详情
                  </router-link>
                  <button
                    v-if="order.status === 'PENDING_PAYMENT'"
                    @click="handlePayOrder(order)"
                    :disabled="payingOrders[order.id]"
                    class="px-6 py-2 bg-gradient-to-r from-blue-500 to-purple-500 text-white font-semibold rounded-lg hover:from-indigo-600 hover:to-purple-600 transition-all duration-300 text-sm disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    {{ payingOrders[order.id] ? '支付中...' : '去支付' }}
                  </button>
                  <button
                    v-if="order.status === 'PENDING_PAYMENT'"
                    @click="handleCancelOrder(order)"
                    :disabled="cancellingOrders[order.id]"
                    class="px-6 py-2 bg-white text-red-500 font-semibold border-2 border-red-200 rounded-lg hover:bg-red-50 transition-all duration-300 text-sm disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    {{ cancellingOrders[order.id] ? '取消中...' : '取消订单' }}
                  </button>
                  <button
                    v-if="order.status === 'PAID' || order.status === 'SHIPPED'"
                    @click="handleConfirmOrder(order)"
                    :disabled="confirmingOrders[order.id]"
                    class="px-6 py-2 bg-gradient-to-r from-green-500 to-emerald-500 text-white font-semibold rounded-lg hover:from-green-600 hover:to-emerald-600 transition-all duration-300 text-sm disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    {{ confirmingOrders[order.id] ? '处理中...' : '完成订单' }}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-else-if="activeTab === 'favorites'" class="bg-white rounded-2xl shadow-md border border-gray-100 p-8 min-h-[750px]">
          <h2 class="text-2xl font-bold text-gray-800 mb-6">我的收藏</h2>

          <div v-if="favoriteLoading" class="flex items-center justify-center py-20">
            <div class="w-12 h-12 border-4 border-blue-200 border-t-blue-500 rounded-full animate-spin"></div>
          </div>

          <div v-else-if="favoriteList.length === 0" class="text-center py-20">
            <div class="w-32 h-32 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-6">
              <svg class="w-16 h-16 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 0 0 0 6.364L12 20.364l7.682-7.682a4.5 4.5 0 0 0-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 0 0-6.364 0z"/>
              </svg>
            </div>
            <p class="text-gray-600 font-medium mb-4">暂无收藏商品</p>
            <router-link to="/" class="inline-flex items-center gap-2 px-8 py-3 bg-gradient-to-r from-blue-500 to-purple-500 text-white font-semibold rounded-lg hover:from-indigo-600 hover:to-purple-600 transition-all duration-300 text-sm">
              去购物
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 8l4 4m0 0l-4 4m4-4H3"/>
              </svg>
            </router-link>
          </div>

          <div v-else class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div
              v-for="favorite in favoriteList"
              :key="favorite.favoriteId"
              class="border border-gray-100 rounded-xl p-4 hover:shadow-md transition-all duration-300"
            >
              <div class="flex gap-4">
                <div class="w-24 h-24 bg-gray-50 rounded-lg overflow-hidden flex-shrink-0">
                  <img
                    :src="favorite.productImage || 'https://placehold.co/96x96/e2e8f0/64748b?text=商品'"
                    :alt="favorite.productName"
                    class="w-full h-full object-cover"
                    @error="$event.target.src = 'https://placehold.co/96x96/e2e8f0/64748b?text=商品'"
                  >
                </div>
                <div class="flex-1 min-w-0">
                  <p class="font-semibold text-gray-800 truncate">{{ favorite.productName }}</p>
                  <p class="text-blue-600 font-bold mt-2">¥{{ Number(favorite.price || 0).toFixed(2) }}</p>
                  <p class="text-sm text-gray-500 mt-1">库存 {{ favorite.stock ?? 0 }}</p>
                  <div class="mt-3 flex gap-2">
                    <router-link
                      :to="'/product/' + favorite.productId"
                      class="px-4 py-1.5 bg-white text-gray-700 border border-gray-200 rounded-md text-sm hover:bg-gray-50"
                    >
                      查看商品
                    </router-link>
                    <button
                      @click="handleRemoveFavorite(favorite)"
                      :disabled="removingFavorites[favorite.favoriteId]"
                      class="px-4 py-1.5 bg-white text-red-500 border border-red-200 rounded-md text-sm hover:bg-red-50 disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      {{ removingFavorites[favorite.favoriteId] ? '移除中...' : '取消收藏' }}
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-else-if="activeTab === 'address'" class="bg-white rounded-2xl shadow-md border border-gray-100 p-8 min-h-[750px]">
          <div class="flex items-center justify-between mb-6">
            <h2 class="text-2xl font-bold text-gray-800">收货地址</h2>
            <button
              @click="openAddressModal()"
              class="inline-flex items-center gap-2 px-6 py-2.5 bg-gradient-to-r from-blue-500 to-purple-500 text-white font-semibold rounded-lg hover:from-indigo-600 hover:to-purple-600 transition-all duration-300 text-sm"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
              </svg>
              添加地址
            </button>
          </div>

          <div v-if="userStore.loadingAddresses" class="flex items-center justify-center py-20">
            <div class="w-12 h-12 border-4 border-blue-200 border-t-blue-500 rounded-full animate-spin"></div>
          </div>

          <div v-else-if="userStore.addresses.length === 0" class="text-center py-20">
            <div class="w-32 h-32 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-6">
              <svg class="w-16 h-16 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 0 1-2.827 0l-4.244-4.243a8 8 0 1 1 11.314 0z"/>
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 11a3 3 0 1 1-6 0 3 3 0 0 16 0z"/>
              </svg>
            </div>
            <p class="text-gray-600 font-medium mb-4">暂无收货地址</p>
            <button
              @click="openAddressModal()"
              class="inline-flex items-center gap-2 px-8 py-3 bg-gradient-to-r from-blue-500 to-purple-500 text-white font-semibold rounded-lg hover:from-indigo-600 hover:to-purple-600 transition-all duration-300 text-sm"
            >
              添加地址
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
              </svg>
            </button>
          </div>

          <div v-else class="space-y-4">
            <div
              v-for="(address, index) in userStore.addresses"
              :key="address.id || index"
              class="border border-gray-100 rounded-lg p-6 hover:shadow-md transition-all duration-300"
              :class="address.isDefault ? 'border-blue-300 bg-blue-50/30' : ''"
            >
              <div class="flex items-start justify-between">
                <div class="flex-1">
                  <div class="flex items-center gap-3 mb-2">
                    <span class="font-semibold text-gray-800 text-lg">{{ address.name || '' }}</span>
                    <span class="text-gray-600">{{ address.phone || '' }}</span>
                    <span v-if="address.isDefault" class="px-3 py-0.5 bg-gradient-to-r from-blue-500 to-purple-500 text-white text-xs rounded-full">
                      默认
                    </span>
                  </div>
                  <p class="text-gray-600">{{ (address.province || '') + (address.city || '') + (address.district || '') + (address.detail || '') }}</p>
                </div>
                <div class="flex gap-2">
                  <button
                    @click="openAddressModal(address)"
                    class="px-4 py-1.5 text-blue-600 hover:bg-blue-50 rounded-md transition-all duration-300 text-sm"
                  >
                    编辑
                  </button>
                  <button
                    @click="handleDeleteAddress(address.id)"
                    class="px-4 py-1.5 text-red-500 hover:bg-red-50 rounded-md transition-all duration-300 text-sm"
                  >
                    删除
                  </button>
                </div>
              </div>
              <div class="mt-4 flex gap-3">
                <button
                  v-if="!address.isDefault"
                  @click="handleSetDefaultAddress(address.id)"
                  class="px-4 py-1.5 text-gray-600 hover:text-blue-600 hover:bg-gray-50 rounded-md transition-all duration-300 text-sm"
                >
                  设为默认
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <div v-if="showAddressModal" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50" @click.self="closeAddressModal">
    <div class="bg-white rounded-2xl shadow-xl w-full max-w-md mx-4 overflow-hidden">
      <div class="px-6 py-4 border-b border-gray-100 flex items-center justify-between">
        <h3 class="text-lg font-bold text-gray-800">{{ editingAddress ? '编辑地址' : '添加地址' }}</h3>
        <button @click="closeAddressModal" class="text-gray-400 hover:text-gray-600">
          <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>
      
      <div class="p-6 space-y-4">
        <div>
          <label class="block text-sm font-semibold text-gray-700 mb-2">收货人 *</label>
          <input
            v-model="addressForm.name"
            type="text"
            class="w-full px-4 py-3 bg-gray-50 border-2 border-gray-200 rounded-lg text-gray-700 placeholder-gray-400 focus:outline-none focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-50 transition-all duration-300 text-sm"
            placeholder="请输入收货人姓名"
          >
        </div>
        
        <div>
          <label class="block text-sm font-semibold text-gray-700 mb-2">手机号 *</label>
          <input
            v-model="addressForm.phone"
            type="text"
            class="w-full px-4 py-3 bg-gray-50 border-2 border-gray-200 rounded-lg text-gray-700 placeholder-gray-400 focus:outline-none focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-50 transition-all duration-300 text-sm"
            placeholder="请输入手机号"
          >
        </div>
        
        <div class="grid grid-cols-3 gap-3">
          <div>
            <label class="block text-sm font-semibold text-gray-700 mb-2">省份 *</label>
            <input
              v-model="addressForm.province"
              type="text"
              class="w-full px-4 py-3 bg-gray-50 border-2 border-gray-200 rounded-lg text-gray-700 placeholder-gray-400 focus:outline-none focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-50 transition-all duration-300 text-sm"
              placeholder="省份"
            >
          </div>
          <div>
            <label class="block text-sm font-semibold text-gray-700 mb-2">城市 *</label>
            <input
              v-model="addressForm.city"
              type="text"
              class="w-full px-4 py-3 bg-gray-50 border-2 border-gray-200 rounded-lg text-gray-700 placeholder-gray-400 focus:outline-none focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-50 transition-all duration-300 text-sm"
              placeholder="城市"
            >
          </div>
          <div>
            <label class="block text-sm font-semibold text-gray-700 mb-2">区县 *</label>
            <input
              v-model="addressForm.district"
              type="text"
              class="w-full px-4 py-3 bg-gray-50 border-2 border-gray-200 rounded-lg text-gray-700 placeholder-gray-400 focus:outline-none focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-50 transition-all duration-300 text-sm"
              placeholder="区县"
            >
          </div>
        </div>
        
        <div>
          <label class="block text-sm font-semibold text-gray-700 mb-2">详细地址 *</label>
          <textarea
            v-model="addressForm.detail"
            rows="3"
            class="w-full px-4 py-3 bg-gray-50 border-2 border-gray-200 rounded-lg text-gray-700 placeholder-gray-400 focus:outline-none focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-50 transition-all duration-300 text-sm resize-none"
            placeholder="请输入详细地址"
          ></textarea>
        </div>
        
        <div class="flex items-center">
          <input
            v-model="addressForm.isDefault"
            type="checkbox"
            id="isDefault"
            class="w-4 h-4 text-blue-600 bg-gray-100 border-gray-300 rounded focus:ring-blue-500"
          >
          <label for="isDefault" class="ml-2 text-sm text-gray-600">设为默认地址</label>
        </div>
      </div>
      
      <div class="px-6 py-4 border-t border-gray-100 flex gap-3">
        <button
          @click="closeAddressModal"
          class="flex-1 px-6 py-3 bg-gray-50 text-gray-700 font-semibold rounded-lg hover:bg-gray-100 transition-all duration-300 text-sm"
        >
          取消
        </button>
        <button
          @click="handleSaveAddress"
          class="flex-1 px-6 py-3 bg-gradient-to-r from-blue-500 to-purple-500 text-white font-semibold rounded-lg hover:from-indigo-600 hover:to-purple-600 transition-all duration-300 text-sm"
        >
          保存
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted, inject, watch } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api/request'
import { useUserStore } from '../stores/userStore'
import { useOrderStore } from '../stores/orderStore'

const router = useRouter()
const userStore = useUserStore()
const orderStore = useOrderStore()
const toast = inject('toast')

const activeTab = ref('profile')
const orderTab = ref('all')
const showAddressModal = ref(false)
const editingAddress = ref(null)
const payingOrders = ref({})
const cancellingOrders = ref({})
const confirmingOrders = ref({})
const favoriteLoading = ref(false)
const favoriteList = ref([])
const removingFavorites = ref({})



const profileForm = reactive({
  username: '',
  phone: ''
})

const addressForm = reactive({
  name: '',
  phone: '',
  province: '',
  city: '',
  district: '',
  detail: '',
  isDefault: false
})

const menuItems = [
  { key: 'profile', label: '个人资料', icon: 'profile' },
  { key: 'orders', label: '我的订单', icon: 'orders' },
  { key: 'favorites', label: '我的收藏', icon: 'favorites' },
  { key: 'address', label: '收货地址', icon: 'address' }
]

const orderTabs = [
  { label: '全部', value: 'all' },
  { label: '待支付', value: 'PENDING_PAYMENT' },
  { label: '待发货', value: 'PAID' },
  { label: '待收货', value: 'SHIPPED' },
  { label: '已完成', value: 'COMPLETED' }
]

const filteredOrders = computed(() => {
  if (orderTab.value === 'all') {
    return orderStore.orders
  }
  return orderStore.orders?.filter(order => order.status === orderTab.value) || []
})

const getOrderStatusLabel = (status) => {
  const statusMap = {
    'PENDING_PAYMENT': '待支付',
    'PAID': '待发货',
    'SHIPPED': '待收货',
    'COMPLETED': '已完成',
    'CANCELLED': '已取消'
  }
  return statusMap[status] || status
}

const getOrderStatusClass = (status) => {
  const classMap = {
    'PENDING_PAYMENT': 'bg-yellow-100 text-yellow-700',
    'PAID': 'bg-blue-100 text-blue-700',
    'SHIPPED': 'bg-purple-100 text-purple-700',
    'COMPLETED': 'bg-green-100 text-green-700',
    'CANCELLED': 'bg-gray-100 text-gray-500'
  }
  return classMap[status] || 'bg-gray-100 text-gray-500'
}

const handleSaveProfile = () => {
  toast.success('保存成功！')
}

const handleLogout = async () => {
  if (confirm('确定要退出登录吗？')) {
    await userStore.logout()
    router.push('/auth')
  }
}

const openAddressModal = (address = null) => {
  editingAddress.value = address
  if (address && address.id) {
    Object.assign(addressForm, {
      name: address.name || '',
      phone: address.phone || '',
      province: address.province || '',
      city: address.city || '',
      district: address.district || '',
      detail: address.detail || '',
      isDefault: address.isDefault || false
    })
  } else {
    Object.assign(addressForm, {
      name: '',
      phone: '',
      province: '',
      city: '',
      district: '',
      detail: '',
      isDefault: userStore.addresses.length === 0
    })
  }
  showAddressModal.value = true
}

const closeAddressModal = () => {
  showAddressModal.value = false
  editingAddress.value = null
}

const handleSaveAddress = async () => {
  if (!addressForm.name || !addressForm.phone || !addressForm.province || !addressForm.city || !addressForm.district || !addressForm.detail) {
    toast.warning('请填写完整信息！')
    return
  }

  try {
    if (editingAddress.value) {
      await userStore.updateAddress(editingAddress.value.id, addressForm)
      toast.success('地址更新成功！')
    } else {
      await userStore.addAddress(addressForm)
      toast.success('地址添加成功！')
    }
    closeAddressModal()
  } catch (error) {
    console.error('保存地址失败:', error)
    toast.error('操作失败，请重试！')
  }
}

const handleDeleteAddress = async (id) => {
  if (confirm('确定要删除这个地址吗？')) {
    try {
      await userStore.deleteAddress(id)
      toast.success('删除成功！')
    } catch (error) {
      toast.error('删除失败，请重试！')
    }
  }
}

const handleSetDefaultAddress = async (id) => {
  try {
    await userStore.setDefaultAddress(id)
    toast.success('设置成功！')
  } catch (error) {
    toast.error('设置失败，请重试！')
  }
}

const handlePayOrder = async (order) => {
  if (!order?.id || payingOrders.value[order.id]) return

  payingOrders.value[order.id] = true
  try {
    await orderStore.payOrder(order.id, 'online')
    toast.success('支付成功！')
    await fetchOrdersByTab()
  } catch (error) {
    console.error('支付失败:', error)
    toast.error(orderStore.error?.message || '支付失败，请稍后重试')
  } finally {
    payingOrders.value[order.id] = false
  }
}

const handleCancelOrder = async (order) => {
  if (!order?.id || cancellingOrders.value[order.id]) return
  if (!confirm('确定取消该订单吗？')) return

  cancellingOrders.value[order.id] = true
  try {
    await orderStore.cancelOrder(order.id)
    toast.success('订单已取消')
    await fetchOrdersByTab()
  } catch (error) {
    console.error('取消订单失败:', error)
    toast.error(orderStore.error?.message || '取消失败，请稍后重试')
  } finally {
    cancellingOrders.value[order.id] = false
  }
}

const handleConfirmOrder = async (order) => {
  if (!order?.id || confirmingOrders.value[order.id]) return
  if (!confirm('确认将订单标记为已完成吗？')) return

  confirmingOrders.value[order.id] = true
  try {
    await orderStore.confirmReceipt(order.id)
    toast.success('订单已完成')
    await fetchOrdersByTab()
  } catch (error) {
    console.error('确认收货失败:', error)
    toast.error(orderStore.error?.message || '确认失败，请稍后重试')
  } finally {
    confirmingOrders.value[order.id] = false
  }
}

const fetchFavorites = async () => {
  favoriteLoading.value = true
  try {
    const response = await api.get('/api/favorite/list', {
      params: { page: 1, pageSize: 100 }
    })
    favoriteList.value = response.data?.data?.list || []
  } catch (error) {
    console.error('加载收藏失败:', error)
    favoriteList.value = []
    toast.error(error.response?.data?.message || '加载收藏失败，请稍后重试')
  } finally {
    favoriteLoading.value = false
  }
}

const handleRemoveFavorite = async (favorite) => {
  if (!favorite?.favoriteId || removingFavorites.value[favorite.favoriteId]) return
  removingFavorites.value[favorite.favoriteId] = true
  try {
    await api.delete(`/api/favorite/${favorite.favoriteId}`)
    favoriteList.value = favoriteList.value.filter(item => item.favoriteId !== favorite.favoriteId)
    toast.success('已取消收藏')
  } catch (error) {
    console.error('取消收藏失败:', error)
    toast.error(error.response?.data?.message || '取消收藏失败，请稍后重试')
  } finally {
    removingFavorites.value[favorite.favoriteId] = false
  }
}

const fetchOrdersByTab = async () => {
  const status = orderTab.value === 'all' ? null : orderTab.value
  await orderStore.fetchOrders(1, 20, status)
}

onMounted(() => {
  if (userStore.user) {
    profileForm.username = userStore.user.username || ''
    profileForm.phone = userStore.user.phone || ''
  }
  fetchOrdersByTab()
  userStore.fetchAddresses()
})

watch(orderTab, () => {
  fetchOrdersByTab()
})

watch(activeTab, (tab) => {
  if (tab === 'favorites') {
    fetchFavorites()
  }
})
</script>
