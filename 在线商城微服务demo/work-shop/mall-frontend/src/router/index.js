import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'home',
    component: () => import('../views/HomeView.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/auth',
    name: 'auth',
    component: () => import('../views/AuthView.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/product/:id',
    name: 'product',
    component: () => import('../views/ProductDetailView.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/cart',
    name: 'cart',
    component: () => import('../views/CartView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/order/confirm',
    name: 'orderConfirm',
    component: () => import('../views/OrderConfirmView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/order',
    name: 'order',
    component: () => import('../views/OrderView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/order/:id',
    name: 'orderDetail',
    component: () => import('../views/OrderDetailView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/user/center',
    name: 'userCenter',
    component: () => import('../views/UserCenterView.vue'),
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && !token) {
    next('/auth')
  } else {
    next()
  }
})

export default router