import { createSlice, createAsyncThunk } from '@reduxjs/toolkit'
import { recordService } from '../../services/recordService'

export const fetchRecords = createAsyncThunk('records/fetch', () => recordService.getRecords())

const recordsSlice = createSlice({
  name: 'records',
  initialState: { list: [], status: 'idle', error: null },
  reducers: {},
  extraReducers: (b) => {
    b.addCase(fetchRecords.pending, (s) => { s.status = 'loading' })
     .addCase(fetchRecords.fulfilled, (s, { payload }) => { s.status = 'succeeded'; s.list = payload })
     .addCase(fetchRecords.rejected, (s, { error }) => { s.status = 'failed'; s.error = error.message })
  },
})
export default recordsSlice.reducer
