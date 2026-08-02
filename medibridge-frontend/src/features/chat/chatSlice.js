import { createSlice, createAsyncThunk } from '@reduxjs/toolkit'
import { chatService } from '../../services/chatService'
import { doctorService } from '../../services/doctorService'

export const sendChatMessage = createAsyncThunk(
  'chat/sendMessage',
  async ({ sessionId, message }, { rejectWithValue }) => {
    try {
      const result = await chatService.sendMessage(sessionId, message)
      return { result }
    } catch (e) {
      return rejectWithValue(e?.response?.data?.error || 'The assistant is unavailable right now.')
    }
  }
)

export const sendTriageMessage = createAsyncThunk(
  'chat/sendTriageMessage',
  async ({ sessionId, message }, { rejectWithValue }) => {
    try {
      const result = await chatService.sendTriageAnswer(sessionId, message)
      let doctors = result.doctors
      if (result.specialty && !result.needs_more_info && !doctors) {
        // Live gateway responses already embed `doctors` (triage.routes.js).
        // The mock service doesn't, so fetch the same way SymptomChecker does.
        doctors = await doctorService.getDoctors({ specialization: result.specialty })
      }
      return { result: { ...result, doctors: doctors ?? [] } }
    } catch (e) {
      return rejectWithValue(e?.response?.data?.error || 'The assistant is unavailable right now.')
    }
  }
)

const newSessionId = () => crypto.randomUUID()
const emptyTriage = () => ({ specialty: null, urgency: 'routine', doctors: [], needsMoreInfo: false, resolved: false })

const initialState = {
  isOpen: false,
  mode: 'faq', // 'faq' | 'triage'
  sessionId: newSessionId(),
  messages: [], // { role: 'user' | 'assistant', text, chunks?, emergency? }
  triage: emptyTriage(),
  status: 'idle',
  error: null,
}

const chatSlice = createSlice({
  name: 'chat',
  initialState,
  reducers: {
    widgetOpened(state) { state.isOpen = true },
    widgetClosed(state) { state.isOpen = false },
    // Switching FAQ <-> triage starts a fresh conversation - mixing the two
    // in one session_id would confuse the backend's triage history.
    modeChanged(state, { payload }) {
      state.mode = payload
      state.messages = []
      state.triage = emptyTriage()
      state.sessionId = newSessionId()
      state.error = null
    },
    conversationReset(state) {
      state.messages = []
      state.triage = emptyTriage()
      state.sessionId = newSessionId()
      state.error = null
    },
  },
  extraReducers: (b) => {
    b.addCase(sendChatMessage.pending, (s, { meta }) => {
      s.status = 'loading'
      s.error = null
      s.messages.push({ role: 'user', text: meta.arg.message })
    })
    .addCase(sendChatMessage.fulfilled, (s, { payload }) => {
      s.status = 'succeeded'
      s.messages.push({
        role: 'assistant',
        text: payload.result.answer ?? "I don't have an answer for that yet - try rephrasing, or reach our support team.",
        chunks: payload.result.chunks,
      })
    })
    .addCase(sendChatMessage.rejected, (s, { payload }) => {
      s.status = 'failed'
      s.error = payload
    })
    .addCase(sendTriageMessage.pending, (s, { meta }) => {
      s.status = 'loading'
      s.error = null
      s.messages.push({ role: 'user', text: meta.arg.message })
    })
    .addCase(sendTriageMessage.fulfilled, (s, { payload }) => {
      s.status = 'succeeded'
      const { result } = payload
      s.messages.push({
        role: 'assistant',
        text: result.needs_more_info ? result.next_question : result.summary,
        emergency: result.urgency === 'emergency',
      })
      s.triage = {
        specialty: result.specialty,
        urgency: result.urgency,
        doctors: result.doctors,
        needsMoreInfo: result.needs_more_info,
        resolved: !result.needs_more_info,
      }
    })
    .addCase(sendTriageMessage.rejected, (s, { payload }) => {
      s.status = 'failed'
      s.error = payload
    })
  },
})

export const { widgetOpened, widgetClosed, modeChanged, conversationReset } = chatSlice.actions
export default chatSlice.reducer
