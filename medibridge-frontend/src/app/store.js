import { configureStore } from '@reduxjs/toolkit'
import authReducer from '../features/auth/authSlice'
import doctorsReducer from '../features/doctors/doctorsSlice'
import appointmentsReducer from '../features/appointments/appointmentsSlice'
import recordsReducer from '../features/records/recordsSlice'
import adminReducer from '../features/admin/adminSlice'

export const store = configureStore({
  reducer: {
    auth: authReducer,
    doctors: doctorsReducer,
    appointments: appointmentsReducer,
    records: recordsReducer,
    admin: adminReducer,
  },
})
