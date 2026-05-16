import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  withCredentials: true,          // carry HttpOnly refresh_token cookie
  xsrfCookieName: 'XSRF-TOKEN',  // read from cookie…
  xsrfHeaderName: 'X-XSRF-TOKEN', // …send in this header (CSRF protection)
  headers: { 'Content-Type': 'application/json' },
})

// ─── Token refresh queue ────────────────────────────────────────────────────
let isRefreshing = false
let refreshQueue = []

const processQueue = (error, token = null) => {
  refreshQueue.forEach((cb) => (error ? cb.reject(error) : cb.resolve(token)))
  refreshQueue = []
}

// ─── Request: attach Bearer token ───────────────────────────────────────────
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('stufi_access_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// ─── Response: auto-refresh on 401 ─────────────────────────────────────────
api.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config
    const isRefreshRequest = original?.url?.includes('/api/v1/auth/refresh')

    if (isRefreshRequest) {
      processQueue(error, null)
      localStorage.removeItem('stufi_access_token')
      localStorage.removeItem('stufi_user')
      return Promise.reject(error)
    }

    if (error.response?.status !== 401 || original?._retry) {
      return Promise.reject(error)
    }

    if (isRefreshing) {
      return new Promise((resolve, reject) => {
        refreshQueue.push({ resolve, reject })
      }).then((token) => {
        original.headers.Authorization = `Bearer ${token}`
        return api(original)
      })
    }

    original._retry = true
    isRefreshing = true

    try {
      const { data } = await api.post('/api/v1/auth/refresh')
      const newToken = data.accessToken
      localStorage.setItem('stufi_access_token', newToken)
      processQueue(null, newToken)
      original.headers.Authorization = `Bearer ${newToken}`
      return api(original)
    } catch (refreshError) {
      processQueue(refreshError, null)
      localStorage.removeItem('stufi_access_token')
      localStorage.removeItem('stufi_user')
      window.location.href = '/login'
      return Promise.reject(refreshError)
    } finally {
      isRefreshing = false
    }
  }
)

export default api
