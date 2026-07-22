import axiosClient, { USE_MOCK } from '../api/axiosClient'
import { mockResolve } from './_mock'
import { adminStats, adminRecentActivity, adminPatients, adminDoctors, adminAppointments, adminAnalytics, adminSystemSettings } from '../api/mock/mockData'

export const adminService = {
  async getDashboard() {
    if (USE_MOCK) return mockResolve({ stats: adminStats, activity: adminRecentActivity })
    const { data } = await axiosClient.get('/admin/dashboard')
    return data
  },
  async getPatients() {
    if (USE_MOCK) return mockResolve(adminPatients)
    const { data } = await axiosClient.get('/admin/patients')
    return data
  },
  async getDoctors() {
    if (USE_MOCK) return mockResolve(adminDoctors)
    const { data } = await axiosClient.get('/admin/doctors')
    return data
  },
  async getAppointments() {
    if (USE_MOCK) return mockResolve(adminAppointments)
    const { data } = await axiosClient.get('/admin/appointments')
    return data
  },
  async getAnalytics() {
    if (USE_MOCK) return mockResolve(adminAnalytics)
    const { data } = await axiosClient.get('/admin/analytics')
    return data
  },
  async getSystemSettings() {
    if (USE_MOCK) return mockResolve(adminSystemSettings)
    const { data } = await axiosClient.get('/admin/settings')
    return data
  },
}
