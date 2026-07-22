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
  /** Cancelling triggers an automatic refund on the server, per policy. */
  async cancelAppointment(id, reason) {
    if (USE_MOCK) return mockResolve({ appointment_id: id, status: 'cancelled' })
    const { data } = await axiosClient.patch(`/appointments/${id}/cancel`, { reason })
    return data
  },
  async getDoctorDashboard() {
    if (USE_MOCK) return mockResolve({ today: doctorTodaySchedule, pending: doctorPendingRequests, completed: doctorCompletedConsults })
    const { data } = await axiosClient.get('/appointments/doctor/dashboard')
    return data
  },
  /**
   * Marks a consultation done.
   *
   * There is no accept/reject any more: paying for a published slot confirms
   * the appointment outright. A doctor who cannot attend cancels instead,
   * which refunds the patient in full.
   */
  async completeAppointment(id) {
    if (USE_MOCK) return mockResolve({ appointment_id: id, status: 'confirmed' })
    const { data } = await axiosClient.patch(`/appointments/${id}/complete`)
    return data
  },

  async getDoctorAppointments() {
    if (USE_MOCK) return mockResolve([])
    const { data } = await axiosClient.get('/appointments/doctor')
    return data
  },
}
