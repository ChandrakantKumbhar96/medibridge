import axiosClient, { USE_MOCK } from '../api/axiosClient'
import { mockResolve } from './_mock'
import { medicalRecords } from '../api/mock/mockData'

export const recordService = {
  /**
   * @param subject undefined for everyone on the account, 'self' for the account
   *   holder's own documents, or a family_member_id.
   */
  async getRecords(subject) {
    if (USE_MOCK) return mockResolve(filterBySubject(medicalRecords, subject))
    const { data } = await axiosClient.get('/records', {
      params: subject ? { subject } : undefined,
    })
    return data
  },
  /**
   * A patient's documents, read by the treating doctor.
   *
   * The server allows this only where a real appointment links the two — the
   * doctor id comes from the JWT, never from the request.
   */
  async getPatientRecords(patientId) {
    if (USE_MOCK) return mockResolve(medicalRecords)
    const { data } = await axiosClient.get(`/doctor/patients/${patientId}/records`)
    return data
  },
  /** @param family_member_id omit to file the document under the account holder. */
  async uploadRecord({ file, report_name, report_type, family_member_id }) {
    if (USE_MOCK) {
      return mockResolve({ report_id: 'r-new', report_name, report_type, size: '1.0 MB', family_member_id })
    }
    const form = new FormData()
    form.append('file', file)
    const { data } = await axiosClient.post('/records', form, {
      params: { report_name, report_type, family_member_id },
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

/**
 * Mirrors the server's `subject` rule so mock mode behaves the same way: the
 * account holder's own documents carry no family_member_id at all, because the
 * server omits null fields.
 */
function filterBySubject(records, subject) {
  if (!subject) return records
  if (subject === 'self') return records.filter((r) => !r.family_member_id)
  return records.filter((r) => String(r.family_member_id) === String(subject))
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
