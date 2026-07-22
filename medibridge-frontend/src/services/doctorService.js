import axiosClient, { USE_MOCK } from '../api/axiosClient'
import { mockResolve } from './_mock'
import { doctors, specialtyCards, specializations, timeSlots } from '../api/mock/mockData'

export const doctorService = {
  async getDoctors(params = {}) {
    if (USE_MOCK) {
      const { specialization } = params
      if (!specialization) return mockResolve(doctors)
      // Mock rows use 'Cardiologist' while the specialty list says 'Cardiology',
      // so match on a prefix rather than equality.
      return mockResolve(
        doctors.filter((d) =>
          d.specialization.toLowerCase().startsWith(specialization.toLowerCase().slice(0, 5))
        )
      )
    }
    const { data } = await axiosClient.get('/doctors', { params })
    return data
  },
  async getSpecialties() {
    if (USE_MOCK) return mockResolve(specialtyCards)
    const { data } = await axiosClient.get('/specialties')
    return data
  },
  async getSpecializations() {
    if (USE_MOCK) return mockResolve(specializations)
    const { data } = await axiosClient.get('/specializations')
    return data
  },
  /**
   * Returns [{ schedule_id, start_time, end_time, label }].
   * schedule_id is what the booking endpoint requires - the unique constraint on
   * it is what prevents two patients taking the same slot.
   */
  async getAvailableSlots(doctorId, date) {
    if (USE_MOCK) {
      return mockResolve(
        timeSlots.map((label, i) => ({ schedule_id: i + 1, label, start_time: label }))
      )
    }
    const { data } = await axiosClient.get(`/doctors/${doctorId}/slots`, { params: { date } })
    return data
  },
}
