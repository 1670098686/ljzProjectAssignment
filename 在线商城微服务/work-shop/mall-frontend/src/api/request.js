import axios from 'axios'

const extractBusinessCode = (errorLike) => {
  const codeFromData = Number(errorLike?.response?.data?.code)
  if (Number.isFinite(codeFromData) && codeFromData > 0) {
    return codeFromData
  }

  const messageCandidates = [
    errorLike?.response?.data?.message,
    errorLike?.message
  ].filter(Boolean)

  for (const text of messageCandidates) {
    if (typeof text !== 'string') continue

    const runtimeCodeMatch = text.match(/code\s*=\s*(\d{4})/i)
    if (runtimeCodeMatch) {
      const value = Number(runtimeCodeMatch[1])
      if (Number.isFinite(value)) return value
    }

    const jsonCodeMatch = text.match(/"code"\s*:\s*(\d{3,5})/i)
    if (jsonCodeMatch) {
      const value = Number(jsonCodeMatch[1])
      if (Number.isFinite(value)) return value
    }
  }

  return null
}

const api = axios.create({
  baseURL: '',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

api.interceptors.request.use(
  (config) => {
    // 确保请求 URL 以 /api 开头
    if (config.url && !config.url.startsWith('/api')) {
      config.url = `/api${config.url.startsWith('/') ? '' : '/'}${config.url}`
    }
    
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    // 始终设置 X-User-Id，后端所有接口都需要
    const userId = localStorage.getItem('userId') || '1'
    config.headers['X-User-Id'] = userId
    
    console.log('发送请求:', config.method?.toUpperCase(), config.url)
    if (config.data) {
      console.log('请求数据:', JSON.stringify(config.data))
    }
    
    return config
  },
  (error) => {
    console.error('请求拦截器错误:', error)
    return Promise.reject(error)
  }
)

api.interceptors.response.use(
  (response) => {
    console.log('收到响应:', response.config.url, JSON.stringify(response.data))
    
    // 检查业务状态码
    if (response.data && response.data.code !== undefined && response.data.code !== 200) {
      console.error('业务错误:', response.config.url, response.data.code, response.data.message)
      // 创建一个错误对象
      const businessCode = extractBusinessCode({ response: { data: response.data } })
      const error = {
        businessCode,
        response: {
          status: response.data.code,
          data: response.data
        },
        message: response.data.message || '请求失败'
      }
      return Promise.reject(error)
    }
    
    return response
  },
  (error) => {
    const timeoutLike = error?.code === 'ECONNABORTED' || String(error?.message || '').toLowerCase().includes('timeout')
    if (timeoutLike && !error?.businessCode) {
      error.businessCode = 5402
    }
    const businessCode = extractBusinessCode(error)
    if (businessCode !== null) {
      error.businessCode = businessCode
    }
    if (error.response) {
      console.error('请求错误:', error.config?.url, error.response.status, JSON.stringify(error.response.data))
    } else {
      console.error('请求错误:', error.message)
    }
    if (error.response?.status === 401 && error.config?.url !== '/api/user/login' && error.config?.url !== '/api/user/register') {
      localStorage.removeItem('token')
      window.location.href = '/auth'
    }
    return Promise.reject(error)
  }
)

export default api
