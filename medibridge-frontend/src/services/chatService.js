import axiosClient, { USE_MOCK } from '../api/axiosClient'
import { mockResolve } from './_mock'
import { chatEmergencyKeywords, chatEmergencyMessage, chatFaqTopics, chatTriageRules } from '../api/mock/mockData'

// axiosClient's default baseURL is now the gateway, which is the only thing
// that serves /chat and /triage - Spring has no such routes.

// Mock-only: no backend to hold multi-turn triage state, so it lives here
// per session_id, mirroring app/session/redis_store.py.
const mockTriageHistory = new Map()

function matchTriageRule(text) {
  const lowered = text.toLowerCase()
  return chatTriageRules.find((r) => r.keywords.some((k) => lowered.includes(k)))
}

export const chatService = {
  async sendMessage(sessionId, message) {
    if (USE_MOCK) {
      const lowered = message.toLowerCase()
      if (chatEmergencyKeywords.some((k) => lowered.includes(k))) {
        return mockResolve({ answer: chatEmergencyMessage, chunks: [] })
      }
      const topic = chatFaqTopics.find((t) => t.keywords.some((k) => lowered.includes(k)))
      return mockResolve(
        topic
          ? { answer: topic.answer, chunks: [{ source: 'faq', text: topic.answer, score: 0.9 }] }
          : { answer: null, chunks: [] }
      )
    }
    const { data } = await axiosClient.post('/chat/message', { session_id: sessionId, message })
    return data
  },

  async sendTriageAnswer(sessionId, message) {
    if (USE_MOCK) {
      const lowered = message.toLowerCase()
      if (chatEmergencyKeywords.some((k) => lowered.includes(k))) {
        mockTriageHistory.delete(sessionId)
        return mockResolve({
          specialty: null, urgency: 'emergency', summary: chatEmergencyMessage,
          needs_more_info: false, next_question: null,
        })
      }

      const soFar = `${mockTriageHistory.get(sessionId) ?? ''} ${message}`.trim()
      const matched = matchTriageRule(soFar)
      if (matched) {
        mockTriageHistory.delete(sessionId)
        return mockResolve({
          specialty: matched.specialty, urgency: matched.urgency,
          summary: `Based on what you've described, we recommend booking ${matched.specialty}.`,
          needs_more_info: false, next_question: null,
        })
      }

      if (!mockTriageHistory.has(sessionId)) {
        // First turn, no keyword match yet - ask once before falling back.
        mockTriageHistory.set(sessionId, soFar)
        return mockResolve({
          specialty: null, urgency: 'routine', summary: null, needs_more_info: true,
          next_question: 'Can you tell me a bit more about your symptoms?',
        })
      }

      // Second turn still with no match - the real service would let Groq
      // decide; the mock just falls back rather than asking forever.
      mockTriageHistory.delete(sessionId)
      return mockResolve({
        specialty: 'General Physician', urgency: 'routine', needs_more_info: false, next_question: null,
        summary: "Based on what you've described, we recommend booking a General Physician visit.",
      })
    }
    const { data } = await axiosClient.post('/triage/answer', { session_id: sessionId, message })
    return data
  },
}
