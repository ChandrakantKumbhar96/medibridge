import axiosClient, { USE_MOCK } from '../api/axiosClient'
import { mockResolve } from './_mock'
import { patientProfile, doctorProfile } from '../api/mock/mockData'

// Returns { token, user } where user = { id, name, email, role }
export const authService = {
  async login({ email, password, role }) {
    if (USE_MOCK) {
      const base =
        role === 'doctor'
          ? { id: 'd-1', name: doctorProfile.full_name, email: doctorProfile.email, role: 'doctor', specialization: doctorProfile.specialization }
          : role === 'admin'
          ? { id: 'adm-1', name: 'Admin User', email, role: 'admin', title: 'System Administrator' }
          : { id: patientProfile.patient_id, name: patientProfile.full_name, email, role: 'patient' }
      return mockResolve({ token: 'mock-jwt-token', user: base })
    }
    const { data } = await axiosClient.post('/auth/login', { email, password, role })
    return data
  },

  /**
   * Exchanges a Google ID token for a MediBridge session.
   * Only the credential is sent - the backend reads the email from the verified
   * token, so nothing here can claim an identity it does not own.
   */
  async loginWithGoogle(credential) {
    if (USE_MOCK) {
      return mockResolve({
        token: 'mock-jwt-token',
        user: { id: 'g-1', name: 'Google User', email: 'google.user@gmail.com', role: 'patient' },
      })
    }
    const { data } = await axiosClient.post('/auth/google', { credential, role: 'patient' })
    return data
  },

  /** Which federated providers the server actually has configured. */
  async getProviders() {
    if (USE_MOCK) return mockResolve({ google: false })
    const { data } = await axiosClient.get('/auth/providers')
    return data
  },

  async registerPatient(payload) {
    if (USE_MOCK) return mockResolve({ token: 'mock-jwt-token', user: { id: 999, name: payload.full_name, email: payload.email, role: 'patient' } })
    const { data } = await axiosClient.post('/auth/register/patient', payload)
    return data
  },

  async registerDoctor(payload) {
    if (USE_MOCK) return mockResolve({ token: 'mock-jwt-token', user: { id: 'd-999', name: payload.full_name, email: payload.email, role: 'doctor' } })
    const { data } = await axiosClient.post('/auth/register/doctor', payload)
    return data
  },
}
