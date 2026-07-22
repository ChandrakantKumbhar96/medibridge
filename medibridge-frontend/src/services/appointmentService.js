import axiosClient, { USE_MOCK } from '../api/axiosClient'
import { mockResolve } from './_mock'
import { patientAppointments, doctorTodaySchedule, doctorPendingRequests, doctorCompletedConsults } from '../api/mock/mockData'

export const appointmentService = {
  async getPatientAppointments() {
    if (USE_MOCK) return mockResolve(patientAppointments)
    const { data } = await axiosClient.get('/appointments/patient')
    return data
  },
  async bookAppointment(payload) {
    if (USE_MOCK) return mockResolve({ appointment_id: 'a-new', status: 'Requested', ...payload })
    const { data } = await axiosClient.post('/appointments', payload)
    return data
  },
  async cancelAppointment(id) {
    if (USE_MOCK) return mockResolve({ appointment_id: id, status: 'Cancelled' })
    const { data } = await axiosClient.patch(`/appointments/${id}/cancel`)
    return data
  },
  async getDoctorDashboard() {
    if (USE_MOCK) return mockResolve({ today: doctorTodaySchedule, pending: doctorPendingRequests, completed: doctorCompletedConsults })
    const { data } = await axiosClient.get('/appointments/doctor/dashboard')
    return data
  },
  async respondToRequest(id, action) {
    if (USE_MOCK) return mockResolve({ id, action })
    const { data } = await axiosClient.patch(`/appointments/${id}/respond`, { action })
    return data
  },
}
