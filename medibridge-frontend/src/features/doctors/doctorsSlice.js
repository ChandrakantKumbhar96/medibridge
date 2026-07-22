import { createSlice, createAsyncThunk } from '@reduxjs/toolkit'
import { doctorService } from '../../services/doctorService'

export const fetchDoctors = createAsyncThunk('doctors/fetch', () => doctorService.getDoctors())
export const fetchSpecialties = createAsyncThunk('doctors/specialties', () => doctorService.getSpecialties())

const doctorsSlice = createSlice({
  name: 'doctors',
  initialState: { list: [], specialties: [], status: 'idle', error: null },
  reducers: {},
  extraReducers: (b) => {
    b.addCase(fetchDoctors.pending, (s) => { s.status = 'loading' })
     .addCase(fetchDoctors.fulfilled, (s, { payload }) => { s.status = 'succeeded'; s.list = payload })
     .addCase(fetchDoctors.rejected, (s, { error }) => { s.status = 'failed'; s.error = error.message })
     .addCase(fetchSpecialties.fulfilled, (s, { payload }) => { s.specialties = payload })
  },
})
export default doctorsSlice.reducer
