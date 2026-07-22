import { createSlice, createAsyncThunk } from '@reduxjs/toolkit'
import { adminService } from '../../services/adminService'

export const fetchAdminDashboard = createAsyncThunk('admin/dashboard', () => adminService.getDashboard())
export const fetchAdminPatients = createAsyncThunk('admin/patients', () => adminService.getPatients())
export const fetchAdminDoctors = createAsyncThunk('admin/doctors', () => adminService.getDoctors())
export const fetchAdminAppointments = createAsyncThunk('admin/appointments', () => adminService.getAppointments())
export const fetchAdminAnalytics = createAsyncThunk('admin/analytics', () => adminService.getAnalytics())

const adminSlice = createSlice({
  name: 'admin',
  initialState: {
    stats: null, activity: [], patients: [], doctors: [], appointments: [], analytics: null,
    status: 'idle',
  },
  reducers: {},
  extraReducers: (b) => {
    b.addCase(fetchAdminDashboard.fulfilled, (s, { payload }) => { s.stats = payload.stats; s.activity = payload.activity })
     .addCase(fetchAdminPatients.fulfilled, (s, { payload }) => { s.patients = payload })
     .addCase(fetchAdminDoctors.fulfilled, (s, { payload }) => { s.doctors = payload })
     .addCase(fetchAdminAppointments.fulfilled, (s, { payload }) => { s.appointments = payload })
     .addCase(fetchAdminAnalytics.fulfilled, (s, { payload }) => { s.analytics = payload })
  },
})
export default adminSlice.reducer
