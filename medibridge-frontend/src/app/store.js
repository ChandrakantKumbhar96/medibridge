import { configureStore } from '@reduxjs/toolkit'
import authReducer from '../features/auth/authSlice'
import doctorsReducer from '../features/doctors/doctorsSlice'
import appointmentsReducer from '../features/appointments/appointmentsSlice'
import recordsReducer from '../features/records/recordsSlice'
import familyReducer from '../features/family/familySlice'
import adminReducer from '../features/admin/adminSlice'
import chatReducer from '../features/chat/chatSlice'

export const store = configureStore({
  reducer: {
    auth: authReducer,
    doctors: doctorsReducer,
    appointments: appointmentsReducer,
    records: recordsReducer,
    family: familyReducer,
    admin: adminReducer,
    chat: chatReducer,
  },
})
