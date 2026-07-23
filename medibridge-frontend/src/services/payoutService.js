import axiosClient, { USE_MOCK } from '../api/axiosClient'
import { mockResolve } from './_mock'

const EMPTY_SUMMARY = {
  pendingAmount: 0, paidAmount: 0, lifetimeEarnings: 0,
  pendingConsultations: 0, commissionRate: 20, payoutCycleDays: 15,
}

/** A doctor's own earnings ledger. */
export const earningsService = {
  async getSummary() {
    if (USE_MOCK) return mockResolve(EMPTY_SUMMARY)
    const { data } = await axiosClient.get('/doctor/earnings/summary')
    return data
  },
  async getEarnings() {
    if (USE_MOCK) return mockResolve([])
    const { data } = await axiosClient.get('/doctor/earnings')
    return data
  },
  async getPayouts() {
    if (USE_MOCK) return mockResolve([])
    const { data } = await axiosClient.get('/doctor/payouts')
    return data
  },
}

/** Platform-side settlement. Admin only — the server enforces it too. */
export const payoutAdminService = {
  async getSummary() {
    if (USE_MOCK) return mockResolve({ totalCommission: 0, pendingPayouts: 0, paidPayouts: 0 })
    const { data } = await axiosClient.get('/admin/payouts/summary')
    return data
  },
  async getPayouts() {
    if (USE_MOCK) return mockResolve([])
    const { data } = await axiosClient.get('/admin/payouts')
    return data
  },
  /** Creates batches for every doctor with unsettled earnings. Safe to re-run. */
  async runSettlement(from, to) {
    const { data } = await axiosClient.post('/admin/payouts/run', null, { params: { from, to } })
    return data
  },
  /** Records that the bank transfer actually happened. */
  async markPaid(payoutId, reference, notes) {
    const { data } = await axiosClient.patch(`/admin/payouts/${payoutId}/paid`,
      { reference, notes })
    return data
  },
}
