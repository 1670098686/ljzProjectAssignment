<template>
  <div class="fixed top-4 right-4 z-[9999] space-y-3">
    <TransitionGroup name="toast">
      <div
        v-for="toast in toasts"
        :key="toast.id"
        :class="[
          'flex items-center gap-3 px-5 py-3 rounded-xl shadow-lg text-white font-medium min-w-[280px] max-w-[400px]',
          toast.type === 'success' ? 'bg-gradient-to-r from-green-500 to-emerald-500' : '',
          toast.type === 'error' ? 'bg-gradient-to-r from-red-500 to-rose-500' : '',
          toast.type === 'warning' ? 'bg-gradient-to-r from-yellow-500 to-amber-500' : '',
          toast.type === 'info' ? 'bg-gradient-to-r from-blue-500 to-indigo-500' : ''
        ]"
      >
        <svg v-if="toast.type === 'success'" class="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
        </svg>
        <svg v-else-if="toast.type === 'error'" class="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
        </svg>
        <svg v-else-if="toast.type === 'warning'" class="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/>
        </svg>
        <svg v-else class="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
        </svg>
        <span class="text-sm">{{ toast.message }}</span>
        <button @click="removeToast(toast.id)" class="ml-auto flex-shrink-0 hover:opacity-70 transition-opacity">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>
    </TransitionGroup>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const toasts = ref([])
let toastId = 0

const showToast = (message, type = 'info', duration = 3000) => {
  const id = ++toastId
  toasts.value.push({ id, message, type })
  
  if (duration > 0) {
    setTimeout(() => {
      removeToast(id)
    }, duration)
  }
}

const removeToast = (id) => {
  toasts.value = toasts.value.filter(t => t.id !== id)
}

const success = (message, duration) => showToast(message, 'success', duration)
const error = (message, duration) => showToast(message, 'error', duration)
const warning = (message, duration) => showToast(message, 'warning', duration)
const info = (message, duration) => showToast(message, 'info', duration)

defineExpose({ success, error, warning, info })
</script>

<style scoped>
.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s ease;
}

.toast-enter-from {
  opacity: 0;
  transform: translateX(100%);
}

.toast-leave-to {
  opacity: 0;
  transform: translateX(100%);
}

.toast-move {
  transition: transform 0.3s ease;
}
</style>
