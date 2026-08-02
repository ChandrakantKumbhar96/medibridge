import axiosClient, { USE_MOCK } from '../api/axiosClient'
import { mockResolve } from './_mock'
import { patientAppointments, doctorTodaySchedule, doctorPendingRequests, doctorCompletedConsults, nextAvailableSlot } from '../api/mock/mockData'

export const appointmentService = {
  /**
   * Upcoming appointments carry their live queue standing - `queue_position`,
   * `eta_minutes` and, when the clinic is behind, `delay_minutes`. The server
   * sends them only for today's confirmed bookings, so their absence is what
   * tells the UI there is no queue to show.
   */
  async getPatientAppointments() {
    if (USE_MOCK) return mockResolve(patientAppointments)
    const { data } = await axiosClient.get('/appointments/patient')
    return data
  },
  /**
   * The soonest open slot in the server-configured window, optionally
   * narrowed to one specialization (a name, e.g. "Cardiologist" - the same
   * value the specialty chips on FindDoctors hold, not an id). Returns null
   * when nothing qualifies (server sends 204) - the caller decides whether to
   * hide the banner or show "nothing soon".
   */
  async getNextAvailable(specialization) {
    if (USE_MOCK) return mockResolve(nextAvailableSlot)
    const { data, status } = await axiosClient.get('/appointments/next-available', {
      params: specialization && specialization !== 'All' ? { specialization } : undefined,
      validateStatus: (s) => s === 200 || s === 204,
    })
    return status === 204 ? null : data
  },
  async bookAppointment(payload) {
    if (USE_MOCK) return mockResolve({ appointment_id: 'a-new', status: 'Requested', ...payload })
    const { data } = await axiosClient.post('/appointments', payload)
    return data
  },
  /**
   * Claims the one free revisit a completed consultation earns.
   *
   * Takes only the parent appointment and a slot: the doctor, the fee and the
   * consult type are all inherited server-side, so there is nothing here that
   * could be tampered with into a free slot with a doctor you never saw.
   *
   * 409 means the revisit is already used or the slot was just taken - both are
   * worth showing verbatim, they say different things.
   */
  async bookFollowUp(parentAppointmentId, scheduleId) {
    if (USE_MOCK) {
      return mockResolve({
        appointment_id: 'a-followup', status: 'confirmed',
        consultation_fee: 0, parent_appointment_id: parentAppointmentId,
        schedule_id: scheduleId,
      })
    }
    const { data } = await axiosClient.post(
      `/appointments/${parentAppointmentId}/follow-up`, { schedule_id: scheduleId })
    return data
  },

  /** Cancelling triggers an automatic refund on the server, per policy. */
  async cancelAppointment(id, reason) {
    if (USE_MOCK) return mockResolve({ appointment_id: id, status: 'cancelled' })
    const { data } = await axiosClient.patch(`/appointments/${id}/cancel`, { reason })
    return data
  },
  /** `today` carries the same queue fields the patient sees, from the same source. */
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

  /**
   * Moves an appointment to another slot with the same doctor.
   * The appointment id survives, so the payment carries across - no refund
   * and recharge.
   */
  async reschedule(id, scheduleId) {
    if (USE_MOCK) return mockResolve({ appointment_id: id, schedule_id: scheduleId })
    const { data } = await axiosClient.patch(`/appointments/${id}/reschedule`,
      { schedule_id: scheduleId })
    return data
  },

  async getDoctorAppointments() {
    if (USE_MOCK) return mockResolve([])
    const { data } = await axiosClient.get('/appointments/doctor')
    return data
  },

  /**
   * Fetches the video room URL on demand. The link is never in the appointment
   * list — the server issues it here only inside the join window, so a failure
   * carries the reason (e.g. "opens at 09:45 AM") in the error message.
   */
  async getJoinLink(id) {
    const { data } = await axiosClient.get(`/appointments/${id}/join`)
    return data.meeting_link
  },

  /**
   * Who is currently in the consultation room — polled by the waiting room.
   *
   * Read-only by design: unlike getJoinLink it records nothing, so leaving the
   * waiting room open does not mark the patient as having attended.
   *
   * The mock walks the doctor in after a few polls so the waiting → joined
   * transition is visible without a backend.
   */
  async getRoomStatus(id) {
    if (USE_MOCK) {
      mockDoctorJoinPolls += 1
      return mockResolve({
        appointment_id: id,
        doctor_joined: mockDoctorJoinPolls > 2,
        patient_joined: true,
        can_join: true,
        status: 'confirmed',
      })
    }
    const { data } = await axiosClient.get(`/appointments/${id}/room-status`)
    return data
  },
}

// Mock-only: counts polls so the fake doctor "arrives" on the third one.
let mockDoctorJoinPolls = 0
