import { createSlice, createAsyncThunk } from '@reduxjs/toolkit'
import { authService } from '../../services/authService'

const savedUser = (() => {
  try { return JSON.parse(localStorage.getItem('mb_user')) } catch { return null }
})()

export const login = createAsyncThunk('auth/login', async (creds, { rejectWithValue }) => {
  try { return await authService.login(creds) } catch (e) { return rejectWithValue(e?.response?.data?.message || 'Login failed') }
})

export const loginWithGoogle = createAsyncThunk('auth/loginWithGoogle', async (credential, { rejectWithValue }) => {
  try { return await authService.loginWithGoogle(credential) } catch (e) { return rejectWithValue(e?.response?.data?.message || 'Google sign-in failed') }
})

export const registerPatient = createAsyncThunk('auth/registerPatient', async (payload, { rejectWithValue }) => {
  try { return await authService.registerPatient(payload) } catch (e) { return rejectWithValue(e?.response?.data?.message || 'Registration failed') }
})

export const registerDoctor = createAsyncThunk('auth/registerDoctor', async (payload, { rejectWithValue }) => {
  try { return await authService.registerDoctor(payload) } catch (e) { return rejectWithValue(e?.response?.data?.message || 'Registration failed') }
})

const persist = (token, user) => {
  localStorage.setItem('mb_token', token)
  localStorage.setItem('mb_user', JSON.stringify(user))
}

const initialState = {
  user: savedUser || null,
  token: localStorage.getItem('mb_token') || null,
  isAuthenticated: !!savedUser,
  status: 'idle',
  error: null,
}

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    logout(state) {
      state.user = null
      state.token = null
      state.isAuthenticated = false
      localStorage.removeItem('mb_token')
      localStorage.removeItem('mb_user')
    },
    clearError(state) { state.error = null },
  },
  extraReducers: (builder) => {
    const fulfilled = (state, { payload }) => {
      state.status = 'succeeded'
      state.user = payload.user
      state.token = payload.token
      state.isAuthenticated = true
      persist(payload.token, payload.user)
    }
    const pending = (state) => { state.status = 'loading'; state.error = null }
    const rejected = (state, { payload }) => { state.status = 'failed'; state.error = payload }
    builder
      .addCase(login.pending, pending).addCase(login.fulfilled, fulfilled).addCase(login.rejected, rejected)
      .addCase(loginWithGoogle.pending, pending).addCase(loginWithGoogle.fulfilled, fulfilled).addCase(loginWithGoogle.rejected, rejected)
      .addCase(registerPatient.pending, pending).addCase(registerPatient.fulfilled, fulfilled).addCase(registerPatient.rejected, rejected)
      .addCase(registerDoctor.pending, pending).addCase(registerDoctor.fulfilled, fulfilled).addCase(registerDoctor.rejected, rejected)
  },
})

export const { logout, clearError } = authSlice.actions
export default authSlice.reducer
