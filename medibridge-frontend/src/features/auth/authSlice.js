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

export const requestOtp = createAsyncThunk('auth/requestOtp', async (phone, { rejectWithValue }) => {
  try { return await authService.otpRequest(phone) } catch (e) { return rejectWithValue(e?.response?.data?.message || 'Could not send the code') }
})

export const verifyOtp = createAsyncThunk('auth/verifyOtp', async (payload, { rejectWithValue }) => {
  try { return await authService.otpVerify(payload) } catch (e) { return rejectWithValue(e?.response?.data?.message || 'Sign-in failed') }
})

export const registerPatient = createAsyncThunk('auth/registerPatient', async (payload, { rejectWithValue }) => {
  try { return await authService.registerPatient(payload) } catch (e) { return rejectWithValue(e?.response?.data?.message || 'Registration failed') }
})

export const registerDoctor = createAsyncThunk('auth/registerDoctor', async (payload, { rejectWithValue }) => {
  try { return await authService.registerDoctor(payload) } catch (e) { return rejectWithValue(e?.response?.data?.message || 'Registration failed') }
})

const persist = (token, user, refreshToken) => {
  localStorage.setItem('mb_token', token)
  localStorage.setItem('mb_user', JSON.stringify(user))
  // Needed by the 401 interceptor in axiosClient to silently renew the
  // 15-minute access token. Without it the session dies mid-use.
  if (refreshToken) localStorage.setItem('mb_refresh_token', refreshToken)
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
      localStorage.removeItem('mb_refresh_token')
    },

    clearError(state) { state.error = null },

    /**
     * Merge server-confirmed profile fields into the cached session user.
     *
     * The topbar, the dashboard greeting and the post-login redirect all read
     * `auth.user`, which is written once at login and rehydrated from
     * `mb_user`. Saving your profile changes the same fields on the server but
     * used to leave that copy alone, so a phone signup stayed "New Patient"
     * with `profile_complete: false` until the next sign-in - a reload did not
     * help, because the reload is what restores the stale copy.
     */
    userUpdated(state, { payload }) {
      if (!state.user) return
      state.user = { ...state.user, ...payload }
      localStorage.setItem('mb_user', JSON.stringify(state.user))
    },
  },
  extraReducers: (builder) => {
    const fulfilled = (state, { payload }) => {
      // Two callers land here with no token and neither is a failure: doctor
      // registration (pending admin approval) and an OTP request, which only
      // says a code was sent. The component reads the payload it needs off the
      // thunk result; there is no session to establish here.
      if (!payload.token) {
        state.status = 'succeeded'
        return
      }
      state.status = 'succeeded'
      state.user = payload.user
      state.token = payload.token
      state.isAuthenticated = true
      persist(payload.token, payload.user, payload.refresh_token)
    }
    const pending = (state) => { state.status = 'loading'; state.error = null }
    const rejected = (state, { payload }) => { state.status = 'failed'; state.error = payload }
    builder
      .addCase(login.pending, pending).addCase(login.fulfilled, fulfilled).addCase(login.rejected, rejected)
      .addCase(loginWithGoogle.pending, pending).addCase(loginWithGoogle.fulfilled, fulfilled).addCase(loginWithGoogle.rejected, rejected)
      .addCase(requestOtp.pending, pending).addCase(requestOtp.fulfilled, fulfilled).addCase(requestOtp.rejected, rejected)
      .addCase(verifyOtp.pending, pending).addCase(verifyOtp.fulfilled, fulfilled).addCase(verifyOtp.rejected, rejected)
      .addCase(registerPatient.pending, pending).addCase(registerPatient.fulfilled, fulfilled).addCase(registerPatient.rejected, rejected)
      .addCase(registerDoctor.pending, pending).addCase(registerDoctor.fulfilled, fulfilled).addCase(registerDoctor.rejected, rejected)
  },
})

export const { logout, clearError, userUpdated } = authSlice.actions
export default authSlice.reducer
