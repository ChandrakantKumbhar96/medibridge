import axiosClient, { USE_MOCK } from '../api/axiosClient'
import { mockResolve } from './_mock'

export const reviewService = {
  /**
   * Submits a rating. `what_stood_out` is an array - the backend stores it in
   * the rating_highlight junction table, so multiple tags all persist.
   */
  async submit(payload) {
    if (USE_MOCK) return mockResolve({ rating_id: 1, ...payload })
    const { data } = await axiosClient.post('/reviews', payload)
    return data
  },

  async getForAppointment(appointmentId) {
    if (USE_MOCK) return mockResolve(null)
    const { data } = await axiosClient.get(`/reviews/appointment/${appointmentId}`)
    return data
  },

  async getForDoctor(doctorId) {
    if (USE_MOCK) return mockResolve([])
    const { data } = await axiosClient.get(`/doctors/${doctorId}/reviews`)
    return data
  },
}
