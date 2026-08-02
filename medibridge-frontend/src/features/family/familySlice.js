import { createSlice, createAsyncThunk } from '@reduxjs/toolkit'
import { familyService } from '../../services/familyService'

export const fetchFamily = createAsyncThunk('family/fetch', () => familyService.getFamily())

export const addFamilyMember = createAsyncThunk('family/add', (member) =>
  familyService.addMember(member))

export const updateFamilyMember = createAsyncThunk('family/update', ({ id, member }) =>
  familyService.updateMember(id, member))

export const removeFamilyMember = createAsyncThunk('family/remove', (id) =>
  familyService.removeMember(id))

const familySlice = createSlice({
  name: 'family',
  initialState: { list: [], status: 'idle', error: null },
  reducers: {},
  extraReducers: (b) => {
    b.addCase(fetchFamily.pending, (s) => { s.status = 'loading' })
     .addCase(fetchFamily.fulfilled, (s, { payload }) => { s.status = 'succeeded'; s.list = payload })
     .addCase(fetchFamily.rejected, (s, { error }) => { s.status = 'failed'; s.error = error.message })
     .addCase(addFamilyMember.fulfilled, (s, { payload }) => { s.list.push(payload) })
     .addCase(updateFamilyMember.fulfilled, (s, { payload }) => {
       const i = s.list.findIndex((m) => m.family_member_id === payload.family_member_id)
       if (i !== -1) s.list[i] = payload
     })
     // Archived, not deleted, on the server - but it is gone from every list the
     // patient can act on, so dropping it here matches what they will see on a
     // reload.
     .addCase(removeFamilyMember.fulfilled, (s, { payload }) => {
       s.list = s.list.filter((m) => m.family_member_id !== payload.family_member_id)
     })
  },
})
export default familySlice.reducer
