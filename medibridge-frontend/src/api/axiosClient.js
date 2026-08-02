import axios from 'axios'

// Routed through the gateway (medibridge-gateway), not straight to Spring -
// the gateway is JWT-gated and proxies everything under /api to Spring
// untouched (see proxy.routes.js), so this is a drop-in swap.
const baseURL = import.meta.env.VITE_GATEWAY_API_URL || 'http://localhost:4000/api'

const axiosClient = axios.create({
  baseURL,
  headers: { 'Content-Type': 'application/json' },
})

// Attach JWT token (stored by the auth slice) to every request.
axiosClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('mb_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

/**
 * Access tokens live 15 minutes. Without this, a user reading a page for longer
 * than that starts getting 401s with no explanation.
 *
 * On a 401 we exchange the refresh token for a new pair and replay the failed
 * request once. `isRefreshing` plus `waiters` matter: a dashboard fires several
 * requests at once, and without the queue each one would kick off its own
 * refresh - the first would rotate the token and the rest would fail against a
 * refresh token the server had already burned.
 */
let isRefreshing = false
let waiters = []

const notifyWaiters = (token, error) => {
  waiters.forEach(({ resolve, reject }) => (error ? reject(error) : resolve(token)))
  waiters = []
}

const forceLogout = () => {
  localStorage.removeItem('mb_token')
  localStorage.removeItem('mb_user')
  localStorage.removeItem('mb_refresh_token')
  // Hard redirect rather than router navigation: this can fire from anywhere,
  // including outside React's tree.
  if (!window.location.pathname.startsWith('/login')) {
    window.location.href = '/login'
  }
}

axiosClient.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config
    const status = error?.response?.status

    if (status !== 401 || !original || original._retried) {
      return Promise.reject(error)
    }

    // A 401 from the auth endpoints themselves means the credentials or the
    // refresh token are bad - refreshing again would loop forever.
    if (original.url?.includes('/auth/login')
        || original.url?.includes('/auth/refresh')
        || original.url?.includes('/auth/google')
        || original.url?.includes('/auth/otp/')) {
      return Promise.reject(error)
    }

    const refreshToken = localStorage.getItem('mb_refresh_token')
    if (!refreshToken) {
      forceLogout()
      return Promise.reject(error)
    }

    original._retried = true

    if (isRefreshing) {
      // Another request is already refreshing - wait for its result.
      return new Promise((resolve, reject) => {
        waiters.push({
          resolve: (token) => {
            original.headers.Authorization = `Bearer ${token}`
            resolve(axiosClient(original))
          },
          reject,
        })
      })
    }

    isRefreshing = true
    try {
      // Bare axios, not axiosClient: going through this same instance would
      // re-enter these interceptors.
      const { data } = await axios.post(`${baseURL}/auth/refresh`, {
        refresh_token: refreshToken,
      })

      localStorage.setItem('mb_token', data.token)
      if (data.refresh_token) {
        localStorage.setItem('mb_refresh_token', data.refresh_token)
      }
      if (data.user) {
        localStorage.setItem('mb_user', JSON.stringify(data.user))
      }

      notifyWaiters(data.token, null)
      original.headers.Authorization = `Bearer ${data.token}`
      return axiosClient(original)

    } catch (refreshError) {
      notifyWaiters(null, refreshError)
      forceLogout()
      return Promise.reject(refreshError)
    } finally {
      isRefreshing = false
    }
  }
)

// Global flag: when true, services return mock data instead of calling the API.
export const USE_MOCK = String(import.meta.env.VITE_USE_MOCK) !== 'false'

export default axiosClient
