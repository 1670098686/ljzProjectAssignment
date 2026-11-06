<template>
  <div class="min-h-screen bg-gradient-to-br from-blue-50 via-indigo-50 to-purple-50 flex items-center justify-center p-4 relative overflow-hidden">
    <div class="absolute inset-0 overflow-hidden">
      <div class="absolute -top-20 -right-20 w-[600px] h-[600px] bg-blue-400 rounded-full mix-blend-multiply filter blur-3xl opacity-60 animate-pulse"></div>
      <div class="absolute -bottom-20 -left-20 w-[600px] h-[600px] bg-purple-400 rounded-full mix-blend-multiply filter blur-3xl opacity-60 animate-pulse" style="animation-delay: 1s;"></div>
      <div class="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 w-[800px] h-[800px] bg-pink-400 rounded-full mix-blend-multiply filter blur-3xl opacity-40 animate-pulse" style="animation-delay: 2s;"></div>
    </div>
    <div class="w-full max-w-6xl flex bg-white rounded-3xl shadow-2xl overflow-hidden relative z-10 border border-gray-100">
      <div class="hidden lg:flex lg:w-1/2 bg-gradient-to-br from-blue-500 via-indigo-500 to-purple-500 p-12 flex-col justify-between relative overflow-hidden">
        <div class="absolute inset-0">
          <div class="absolute top-0 right-0 w-40 h-40 bg-white/20 rounded-full -mr-20 -mt-20"></div>
          <div class="absolute bottom-0 left-0 w-60 h-60 bg-white/20 rounded-full -ml-30 -mb-30"></div>
        </div>
        <div class="relative z-10">
          <div class="inline-flex items-center justify-center w-20 h-20 rounded-2xl bg-white/30 backdrop-blur mb-6 shadow-lg">
            <svg class="w-10 h-10 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5"/>
            </svg>
          </div>
          <h1 class="text-5xl font-bold text-white mb-4 drop-shadow-lg">在线商城</h1>
          <p class="text-white/95 text-xl leading-relaxed">
            欢迎来到我们的在线商城，这里有精选的商品、优惠的价格和优质的服务。
          </p>
        </div>
        <div class="relative z-10 space-y-6">
          <div class="flex items-center gap-4 text-white">
            <div class="w-14 h-14 rounded-2xl bg-white/30 flex items-center justify-center backdrop-blur shadow-lg">
              <svg class="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
              </svg>
            </div>
            <span class="text-xl font-medium">正品保障</span>
          </div>
          <div class="flex items-center gap-4 text-white">
            <div class="w-14 h-14 rounded-2xl bg-white/30 flex items-center justify-center backdrop-blur shadow-lg">
              <svg class="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
              </svg>
            </div>
            <span class="text-xl font-medium">极速发货</span>
          </div>
          <div class="flex items-center gap-4 text-white">
            <div class="w-14 h-14 rounded-2xl bg-white/30 flex items-center justify-center backdrop-blur shadow-lg">
              <svg class="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
              </svg>
            </div>
            <span class="text-xl font-medium">7天无理由退换</span>
          </div>
        </div>
      </div>

      <div class="w-full lg:w-1/2 p-12 lg:p-16">
        <div class="max-w-md mx-auto">
          <div class="lg:hidden text-center mb-8">
            <div class="inline-flex items-center justify-center w-12 h-12 rounded-xl bg-gradient-to-r from-blue-500 to-purple-500 mb-4">
              <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5"/>
              </svg>
            </div>
            <h1 class="text-2xl font-bold text-gray-800">在线商城</h1>
          </div>

          <div class="lg:block">
            <h2 class="text-2xl font-bold text-gray-800 mb-2">
              {{ activeTab === 'login' ? '欢迎回来' : '创建账号' }}
            </h2>
            <p class="text-gray-500 mb-8">
              {{ activeTab === 'login' ? '登录您的账号继续购物' : '注册新账号，开启购物之旅' }}
            </p>
          </div>

          <div class="flex bg-gradient-to-r from-blue-50 to-purple-50 rounded-xl p-1.5 mb-8">
            <button
              @click="activeTab = 'login'"
              :class="[
                'flex-1 py-3.5 px-4 font-semibold rounded-lg transition-all duration-300',
                activeTab === 'login'
                  ? 'bg-gradient-to-r from-blue-500 to-purple-500 text-white shadow-lg'
                  : 'text-gray-600 hover:text-gray-800 hover:bg-white/50'
              ]"
            >
              登录
            </button>
            <button
              @click="activeTab = 'register'"
              :class="[
                'flex-1 py-3.5 px-4 font-semibold rounded-lg transition-all duration-300',
                activeTab === 'register'
                  ? 'bg-gradient-to-r from-blue-500 to-purple-500 text-white shadow-lg'
                  : 'text-gray-600 hover:text-gray-800 hover:bg-white/50'
              ]"
            >
              注册
            </button>
          </div>

          <div v-if="activeTab === 'login'">
            <form @submit.prevent="handleLogin">
              <div class="space-y-5">
                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-2">用户名</label>
                  <div class="relative">
                    <svg class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"/>
                    </svg>
                    <input
                      v-model="loginForm.username"
                      type="text"
                      placeholder="请输入用户名"
                      class="w-full pl-12 pr-4 py-3.5 bg-gray-50 border border-gray-200 rounded-lg text-gray-700 placeholder-gray-400 focus:outline-none focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-50 transition-all duration-200"
                      required
                    >
                  </div>
                </div>

                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-2">密码</label>
                  <div class="relative">
                    <svg class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"/>
                    </svg>
                    <input
                      v-model="loginForm.password"
                      :type="showLoginPassword ? 'text' : 'password'"
                      placeholder="请输入密码"
                      class="w-full pl-12 pr-12 py-3.5 bg-gray-50 border border-gray-200 rounded-lg text-gray-700 placeholder-gray-400 focus:outline-none focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-50 transition-all duration-200"
                      required
                    >
                    <button
                      type="button"
                      @click="showLoginPassword = !showLoginPassword"
                      class="absolute right-4 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 transition-colors"
                    >
                      <svg v-if="showLoginPassword" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21"/>
                      </svg>
                      <svg v-else class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>
                      </svg>
                    </button>
                  </div>
                </div>

                <div class="flex items-center justify-between">
                  <label class="flex items-center gap-2 cursor-pointer">
                    <input
                      v-model="loginForm.remember"
                      type="checkbox"
                      class="w-4 h-4 text-blue-600 rounded border-gray-300 focus:ring-blue-500"
                    >
                    <span class="text-sm text-gray-600">记住我</span>
                  </label>
                </div>

                <button
                  type="submit"
                  :disabled="userStore.loading"
                  class="w-full px-6 py-4 bg-gradient-to-r from-blue-500 via-indigo-500 to-purple-500 text-white font-bold rounded-xl shadow-2xl hover:shadow-3xl hover:from-blue-600 hover:via-indigo-600 hover:to-purple-600 hover:scale-[1.02] transition-all duration-300 disabled:opacity-50 disabled:hover:scale-100 flex items-center justify-center gap-2"
                >
                  <span v-if="userStore.loading" class="w-6 h-6 border-2 border-white border-t-transparent rounded-full animate-spin"></span>
                  <span class="text-lg">{{ userStore.loading ? '登录中...' : '登录' }}</span>
                </button>
              </div>
            </form>

            <p v-if="loginError" class="text-red-500 text-sm text-center mt-4">{{ loginError }}</p>
          </div>

          <div v-else>
            <form @submit.prevent="handleRegister">
              <div class="space-y-4">
                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-2">用户名</label>
                  <div class="relative">
                    <svg class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"/>
                    </svg>
                    <input
                      v-model="registerForm.username"
                      type="text"
                      placeholder="请输入用户名"
                      class="w-full pl-12 pr-4 py-3.5 bg-gray-50 border border-gray-200 rounded-lg text-gray-700 placeholder-gray-400 focus:outline-none focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-50 transition-all duration-200"
                      required
                    >
                  </div>
                </div>

                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-2">密码</label>
                  <div class="relative">
                    <svg class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"/>
                    </svg>
                    <input
                      v-model="registerForm.password"
                      :type="showRegisterPassword ? 'text' : 'password'"
                      placeholder="请输入密码"
                      class="w-full pl-12 pr-12 py-3.5 bg-gray-50 border border-gray-200 rounded-lg text-gray-700 placeholder-gray-400 focus:outline-none focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-50 transition-all duration-200"
                      required
                    >
                    <button
                      type="button"
                      @click="showRegisterPassword = !showRegisterPassword"
                      class="absolute right-4 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 transition-colors"
                    >
                      <svg v-if="showRegisterPassword" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21"/>
                      </svg>
                      <svg v-else class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>
                      </svg>
                    </button>
                  </div>
                </div>

                <div>
                  <label class="block text-sm font-medium text-gray-700 mb-2">手机号</label>
                  <div class="relative">
                    <svg class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z"/>
                    </svg>
                    <input
                      v-model="registerForm.phone"
                      type="tel"
                      placeholder="请输入手机号"
                      class="w-full pl-12 pr-4 py-3.5 bg-gray-50 border border-gray-200 rounded-lg text-gray-700 placeholder-gray-400 focus:outline-none focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-50 transition-all duration-200"
                      required
                    >
                  </div>
                </div>

                <label class="flex items-start gap-2 cursor-pointer">
                  <input
                    v-model="registerForm.agree"
                    type="checkbox"
                    class="w-4 h-4 text-blue-600 rounded border-gray-300 focus:ring-blue-500 mt-0.5"
                    required
                  >
                  <span class="text-sm text-gray-600">我已阅读并同意用户协议和隐私政策</span>
                </label>

                <button
                  type="submit"
                  :disabled="userStore.loading"
                  class="w-full px-6 py-4 bg-gradient-to-r from-blue-500 via-indigo-500 to-purple-500 text-white font-bold rounded-xl shadow-2xl hover:shadow-3xl hover:from-blue-600 hover:via-indigo-600 hover:to-purple-600 hover:scale-[1.02] transition-all duration-300 disabled:opacity-50 disabled:hover:scale-100 flex items-center justify-center gap-2"
                >
                  <span v-if="userStore.loading" class="w-6 h-6 border-2 border-white border-t-transparent rounded-full animate-spin"></span>
                  <span class="text-lg">{{ userStore.loading ? '注册中...' : '注册' }}</span>
                </button>
              </div>
            </form>

            <p v-if="registerError" class="text-red-500 text-sm text-center mt-4">{{ registerError }}</p>
          </div>

          <div class="text-center mt-8">
            <router-link to="/" class="text-blue-600 hover:text-blue-700 font-medium inline-flex items-center gap-2">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18"/>
              </svg>
              返回首页
            </router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/userStore'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('login')
const showLoginPassword = ref(false)
const showRegisterPassword = ref(false)
const loginError = ref('')
const registerError = ref('')

const loginForm = reactive({
  username: '',
  password: '',
  remember: false
})

const registerForm = reactive({
  username: '',
  password: '',
  phone: '',
  agree: false
})

const handleLogin = async () => {
  loginError.value = ''
  try {
    await userStore.login(loginForm.username, loginForm.password)
    router.push('/')
  } catch (error) {
    loginError.value = userStore.error || '登录失败'
  }
}

const handleRegister = async () => {
  registerError.value = ''
  try {
    await userStore.register(
      registerForm.username,
      registerForm.password,
      registerForm.phone
    )
    activeTab.value = 'login'
    loginForm.username = registerForm.username
  } catch (error) {
    registerError.value = userStore.error || '注册失败'
  }
}
</script>
