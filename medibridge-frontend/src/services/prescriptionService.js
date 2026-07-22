import axiosClient, { USE_MOCK } from '../api/axiosClient'
import { mockResolve } from './_mock'
import { triggerBrowserDownload } from './recordService'

export const prescriptionService = {
  async getMyPrescriptions() {
    if (USE_MOCK) return mockResolve([])
    const { data } = await axiosClient.get('/prescriptions')
    return data
  },

  /** Doctor issues a consultation record + prescription in one call. */
  async create(payload) {
    if (USE_MOCK) return mockResolve({ prescription_id: 1, ...payload })
    const { data } = await axiosClient.post('/prescriptions', payload)
    return data
  },

  async downloadPdf(prescriptionId) {
    const { data } = await axiosClient.get(`/prescriptions/${prescriptionId}/pdf`, {
      responseType: 'blob',
    })
    triggerBrowserDownload(data, `prescription-${prescriptionId}.pdf`)
  },
}
