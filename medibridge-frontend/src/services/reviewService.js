import axiosClient, { USE_MOCK } from '../api/axiosClient'
import { mockResolve } from './_mock'

// Sample reviews for mock mode so a doctor profile does not look empty when the
// app runs standalone. Live mode pulls the real seeded reviews from the backend.
const mockDoctorReviews = [
  { rating_id: 1, patient_name: 'Priya Sharma', stars: 5, overall_experience: 'Excellent',
    what_stood_out: ['Bedside Manner', 'Clear Explanation'],
    review_text: 'Very patient and explained my condition in simple terms. Highly recommend.',
    created_at: '2026-06-18' },
  { rating_id: 2, patient_name: 'Rahul Verma', stars: 5, overall_experience: 'Excellent',
    what_stood_out: ['Punctual', 'Knowledgeable'],
    review_text: 'The video consultation started right on time and my prescription came within minutes.',
    created_at: '2026-06-02' },
  { rating_id: 3, patient_name: 'Sneha Iyer', stars: 4, overall_experience: 'Good',
    what_stood_out: ['Clear Explanation'],
    review_text: 'Good consultation, addressed all my concerns about the follow-up.',
    created_at: '2026-05-21' },
]

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
    if (USE_MOCK) return mockResolve(mockDoctorReviews)
    const { data } = await axiosClient.get(`/doctors/${doctorId}/reviews`)
    return data
  },
}
