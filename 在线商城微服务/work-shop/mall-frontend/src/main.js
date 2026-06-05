import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import router from './router'
import pinia from './stores'
import { useUserStore } from './stores/userStore'

const app = createApp(App)

app.use(router)
app.use(pinia)

const userStore = useUserStore()
userStore.initAuth()

app.mount('#app')