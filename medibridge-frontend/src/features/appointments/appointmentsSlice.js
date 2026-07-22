import { createSlice, createAsyncThunk } from '@reduxjs/toolkit'
import { appointmentService } from '../../services/appointmentService'

export const fetchPatientAppointments = createAsyncThunk('appointments/patient', () => appointmentService.getPatientAppointments())
export const fetchDoctorDashboard = createAsyncThunk('appointments/doctorDashboard', () => appointmentService.getDoctorDashboard())
export const cancelAppointment = createAsyncThunk('appointments/cancel', (id) => appointmentService.cancelAppointment(id))

const appointmentsSlice = createSlice({
  name: 'appointments',
  initialState: {
    patient: { upcoming: [], past: [] },
    doctor: { today: [], pending: [], completed: [] },
    status: 'idle',
    error: null,
  },
  reducers: {},
  extraReducers: (b) => {
    b.addCase(fetchPatientAppointments.pending, (s) => { s.status = 'loading' })
     .addCase(fetchPatientAppointments.fulfilled, (s, { payload }) => { s.status = 'succeeded'; s.patient = payload })
     .addCase(fetchDoctorDashboard.fulfilled, (s, { payload }) => { s.status = 'succeeded'; s.doctor = payload })
     .addCase(cancelAppointment.fulfilled, (s, { payload }) => {
       s.patient.upcoming = s.patient.upcoming.filter((a) => a.appointment_id !== payload.appointment_id)
     })
  },
})
export default appointmentsSlice.reducer
