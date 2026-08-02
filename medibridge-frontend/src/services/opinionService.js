import axiosClient, { USE_MOCK } from '../api/axiosClient'
import { mockResolve } from './_mock'
import { medicalOpinions } from '../api/mock/mockData'
import { triggerBrowserDownload } from './recordService'

export const opinionService = {
  /** The reviewing specialist issues their written opinion. */
  async create(payload) {
    if (USE_MOCK) {
      return mockResolve({
        opinion_id: 'o-new',
        verdict: payload.agrees_with_original
          ? 'Agrees with the original diagnosis'
          : 'Differs from the original diagnosis',
        ...payload,
      })
    }
    const { data } = await axiosClient.post('/opinions', payload)
    return data
  },

  async getMyOpinions() {
    if (USE_MOCK) return mockResolve(medicalOpinions)
    const { data } = await axiosClient.get('/opinions')
    return data
  },

  /**
   * Downloads through axios rather than a plain link: the JWT lives in
   * localStorage and only the interceptor attaches it, so a bare <a href> would
   * arrive unauthenticated and 401.
   */
  async downloadPdf(opinionId) {
    const { data } = await axiosClient.get(`/opinions/${opinionId}/pdf`, {
      responseType: 'blob',
    })
    triggerBrowserDownload(data, `second-opinion-${opinionId}.pdf`)
  },
}
