import axiosClient, { USE_MOCK } from '../api/axiosClient'
import { mockResolve } from './_mock'

export const paymentService = {
  /** Whether the server has real gateway keys, or we fall back to the simulated form. */
  async getConfig() {
    if (USE_MOCK) return mockResolve({ gatewayEnabled: false })
    const { data } = await axiosClient.get('/payments/config')
    return data
  },

  /** Step 1: server creates the order and fixes the amount. */
  async createOrder(appointmentId) {
    const { data } = await axiosClient.post('/payments/order', { appointment_id: appointmentId })
    return data
  },

  /**
   * Step 2: hand the gateway's response back for server-side signature checks.
   * The payment does not count until this call succeeds - the browser saying
   * "it worked" proves nothing.
   */
  async verify(payload) {
    const { data } = await axiosClient.post('/payments/verify', payload)
    return data
  },

  async markFailed(orderId, reason) {
    try {
      await axiosClient.post('/payments/failed', { order_id: orderId, reason })
    } catch {
      // Best-effort bookkeeping; never block the user on it.
    }
  },

  async pay(payload) {
    if (USE_MOCK) {
      return mockResolve({
        transaction_id: 1,
        transaction_ref: 'MB-MOCKTRANSACTION',
        appointment_id: payload.appointment_id,
        amount: payload.amount ?? 0,
        payment_method: payload.payment_method,
        status: 'Paid',
      })
    }
    const { data } = await axiosClient.post('/payments', payload)
    return data
  },

  async getMyPayments() {
    if (USE_MOCK) return mockResolve([])
    const { data } = await axiosClient.get('/payments')
    return data
  },
}
