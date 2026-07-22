import axios from 'axios'

const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

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

axiosClient.interceptors.response.use(
  (res) => res,
  (error) => Promise.reject(error)
)

// Global flag: when true, services return mock data instead of calling the API.
export const USE_MOCK = String(import.meta.env.VITE_USE_MOCK) !== 'false'

export default axiosClient
