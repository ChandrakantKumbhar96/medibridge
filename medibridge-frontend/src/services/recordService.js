import axiosClient, { USE_MOCK } from '../api/axiosClient'
import { mockResolve } from './_mock'
import { medicalRecords } from '../api/mock/mockData'

export const recordService = {
  async getRecords() {
    if (USE_MOCK) return mockResolve(medicalRecords)
    const { data } = await axiosClient.get('/records')
    return data
  },
  async uploadRecord({ file, report_name, report_type }) {
    if (USE_MOCK) {
      return mockResolve({ report_id: 'r-new', report_name, report_type, size: '1.0 MB' })
    }
    const form = new FormData()
    form.append('file', file)
    const { data } = await axiosClient.post('/records', form, {
      params: { report_name, report_type },
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    return data
  },

  /**
   * Downloads through axios rather than a plain link, because the JWT lives in
   * localStorage and only the axios interceptor attaches it - a bare <a href>
   * would arrive unauthenticated and 401.
   */
  async download(reportId, filename) {
    const { data } = await axiosClient.get(`/records/${reportId}/download`, {
      responseType: 'blob',
    })
    triggerBrowserDownload(data, filename || `report-${reportId}`)
  },

  async downloadMedicalHistoryPdf() {
    const { data } = await axiosClient.get('/records/medical-history/pdf', {
      responseType: 'blob',
    })
    triggerBrowserDownload(data, 'medical-history.pdf')
  },

  async deleteRecord(reportId) {
    if (USE_MOCK) return mockResolve({})
    await axiosClient.delete(`/records/${reportId}`)
  },
}

/** Saves a Blob to disk and cleans up the temporary object URL. */
export function triggerBrowserDownload(blob, filename) {
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.URL.revokeObjectURL(url)
}
