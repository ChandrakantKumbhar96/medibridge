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

  /** Approve a pending doctor, or suspend an active one. */
  async setDoctorStatus(doctorId, status) {
    if (USE_MOCK) return mockResolve({ doctor_id: doctorId, status })
    const { data } = await axiosClient.patch(`/admin/doctors/${doctorId}/status`, { status })
    return data
  },

  async setPatientStatus(patientId, status) {
    if (USE_MOCK) return mockResolve({ patient_id: patientId, status })
    const { data } = await axiosClient.patch(`/admin/patients/${patientId}/status`, { status })
    return data
  },

  async getActivity(limit = 20) {
    if (USE_MOCK) return mockResolve(adminRecentActivity)
    const { data } = await axiosClient.get('/admin/activity', { params: { limit } })
    return data
  },

  async refundPayment(transactionId, reason) {
    const { data } = await axiosClient.post(`/payments/${transactionId}/refund`, { reason })
    return data
  },
}
