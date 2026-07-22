import axiosClient, { USE_MOCK } from '../api/axiosClient'
import { mockResolve } from './_mock'
import {
  patientProfile, doctorProfile, doctorSchedule,
  doctorPatientRecords, adminSystemSettings,
} from '../api/mock/mockData'

/** Patient's own profile + password. */
export const patientProfileService = {
  async get() {
    if (USE_MOCK) return mockResolve(patientProfile)
    const { data } = await axiosClient.get('/patient/profile')
    return data
  },
  async update(payload) {
    if (USE_MOCK) return mockResolve({ ...patientProfile, ...payload })
    const { data } = await axiosClient.put('/patient/profile', payload)
    return data
  },
  async changePassword(payload) {
    if (USE_MOCK) return mockResolve({ message: 'Password updated' })
    const { data } = await axiosClient.post('/patient/change-password', payload)
    return data
  },
  async stats() {
    if (USE_MOCK) {
      return mockResolve({
        upcomingAppointments: 3, medicalRecords: 12, completedConsultations: 8,
      })
    }
    const { data } = await axiosClient.get('/patient/stats')
    return data
  },
}

/** Doctor's own profile, weekly schedule and treated patients. */
export const doctorProfileService = {
  async get() {
    if (USE_MOCK) return mockResolve(doctorProfile)
    const { data } = await axiosClient.get('/doctor/profile')
    return data
  },
  async update(payload) {
    if (USE_MOCK) return mockResolve({ ...doctorProfile, ...payload })
    const { data } = await axiosClient.put('/doctor/profile', payload)
    return data
  },
  async getSchedule() {
    if (USE_MOCK) return mockResolve(doctorSchedule)
    const { data } = await axiosClient.get('/doctor/schedule')
    return data
  },
  async updateSchedule(days) {
    if (USE_MOCK) return mockResolve(days)
    const { data } = await axiosClient.put('/doctor/schedule', days)
    return data
  },
  async getPatients() {
    if (USE_MOCK) return mockResolve(doctorPatientRecords)
    const { data } = await axiosClient.get('/doctor/patients')
    return data
  },
}

/** Admin system settings. */
export const settingsService = {
  async get() {
    if (USE_MOCK) return mockResolve(adminSystemSettings)
    const { data } = await axiosClient.get('/admin/settings')
    return data
  },
  async update(payload) {
    if (USE_MOCK) return mockResolve({ ...adminSystemSettings, ...payload })
    const { data } = await axiosClient.put('/admin/settings', payload)
    return data
  },
}
