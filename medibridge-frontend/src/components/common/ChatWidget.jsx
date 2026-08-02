import { useEffect, useRef, useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { useNavigate } from 'react-router-dom'
import { MessageCircle, X, Send, Stethoscope, RotateCcw, Star, ArrowRight } from 'lucide-react'
import {
  widgetOpened, widgetClosed, modeChanged, conversationReset, sendChatMessage, sendTriageMessage,
} from '../../features/chat/chatSlice'
import Button from './Button'
import Avatar from './Avatar'
import Badge from './Badge'

const money = (n) => `₹${Number(n ?? 0).toLocaleString('en-IN')}`

/**
 * Floating assistant: app-help FAQ and symptom triage in one widget, talking
 * to medibridge-gateway's /chat and /triage routes (chatService.js). Public
 * on the landing page and for patients; doctors/admins have no use for
 * either mode once signed in.
 */
export default function ChatWidget() {
  const dispatch = useDispatch()
  const navigate = useNavigate()
  const { isAuthenticated, user } = useSelector((s) => s.auth)
  const { isOpen, mode, sessionId, messages, triage, status, error } = useSelector((s) => s.chat)
  const [draft, setDraft] = useState('')
  const scrollRef = useRef(null)

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: 'smooth' })
  }, [messages, status])

  if (isAuthenticated && user?.role !== 'patient') return null

  const send = (e) => {
    e.preventDefault()
    const message = draft.trim()
    if (!message || status === 'loading') return
    setDraft('')
    dispatch(mode === 'triage' ? sendTriageMessage({ sessionId, message }) : sendChatMessage({ sessionId, message }))
  }

  if (!isOpen) {
    return (
      <button
        onClick={() => dispatch(widgetOpened())}
        aria-label="Open chat"
        className="fixed bottom-24 right-5 z-50 flex h-14 w-14 items-center justify-center rounded-full
                   bg-gradient-to-b from-primary-500 to-primary-600 text-white
                   shadow-[0_8px_24px_-6px_rgba(37,99,235,.6)] transition-transform hover:scale-105 lg:bottom-6"
      >
        <MessageCircle size={24} />
      </button>
    )
  }

  return (
    <div className="fixed bottom-24 right-5 z-50 flex h-[min(600px,75vh)] w-[min(380px,90vw)] flex-col
                     overflow-hidden rounded-3xl border border-sand-200 bg-white shadow-2xl lg:bottom-6">
      {/* Header */}
      <div className="flex items-center justify-between gap-2 border-b border-sand-100 bg-gradient-to-b
                       from-primary-500 to-primary-600 px-4 py-3.5 text-white">
        <div className="flex items-center gap-2 font-extrabold">
          <MessageCircle size={18} /> MediBridge Assistant
        </div>
        <div className="flex items-center gap-1">
          <button onClick={() => dispatch(conversationReset())} title="Restart conversation"
            className="rounded-full p-1.5 hover:bg-white/15">
            <RotateCcw size={16} />
          </button>
          <button onClick={() => dispatch(widgetClosed())} title="Close"
            className="rounded-full p-1.5 hover:bg-white/15">
            <X size={18} />
          </button>
        </div>
      </div>

      {/* Mode tabs */}
      <div className="flex gap-1.5 border-b border-sand-100 p-2">
        <button onClick={() => dispatch(modeChanged('faq'))}
          className={`flex-1 rounded-full px-3 py-1.5 text-xs font-bold transition ${
            mode === 'faq' ? 'bg-primary-600 text-white' : 'text-sand-500 hover:bg-sand-100'}`}>
          App help
        </button>
        <button onClick={() => dispatch(modeChanged('triage'))}
          className={`flex-1 rounded-full px-3 py-1.5 text-xs font-bold transition ${
            mode === 'triage' ? 'bg-primary-600 text-white' : 'text-sand-500 hover:bg-sand-100'}`}>
          <Stethoscope size={13} className="mr-1 inline" /> Symptom triage
        </button>
      </div>

      {/* Messages */}
      <div ref={scrollRef} className="flex-1 space-y-3 overflow-y-auto px-4 py-4">
        {messages.length === 0 && (
          <p className="text-center text-xs text-sand-400">
            {mode === 'triage'
              ? "Tell me what's bothering you and I'll suggest which specialist to book. This is guidance, not a diagnosis."
              : 'Ask me about booking, rescheduling, refunds or video consults.'}
          </p>
        )}
        {messages.map((m, i) => (
          <div key={i} className={`flex flex-col gap-1 ${m.role === 'user' ? 'items-end' : 'items-start'}`}>
            <div className={`max-w-[85%] rounded-2xl px-3.5 py-2.5 text-sm ${
              m.role === 'user'
                ? 'bg-primary-600 text-white'
                : m.emergency
                  ? 'border border-danger-200 bg-danger-50 text-danger-700'
                  : 'bg-sand-100 text-sand-800'
            }`}>
              {m.text}
            </div>
            {m.chunks?.length > 0 && (
              <div className="flex max-w-[85%] flex-wrap items-center gap-1 px-1">
                <span className="text-[10px] font-semibold text-sand-400">Sources:</span>
                {[...new Set(m.chunks.map((c) => c.source))].map((source) => (
                  <span key={source} title={m.chunks.find((c) => c.source === source).text}
                    className="rounded-full bg-sand-100 px-2 py-0.5 text-[10px] font-semibold text-sand-500">
                    {source.replace(/_/g, ' ')}
                  </span>
                ))}
              </div>
            )}
          </div>
        ))}
        {status === 'loading' && (
          <div className="flex justify-start">
            <div className="rounded-2xl bg-sand-100 px-3.5 py-2.5 text-sm text-sand-400">Typing…</div>
          </div>
        )}
        {error && <p className="text-center text-xs text-danger-600">{error}</p>}

        {/* Triage result: urgency + matched doctors, once resolved */}
        {mode === 'triage' && triage.resolved && triage.specialty && (
          <div className="space-y-2 rounded-2xl border border-sand-200 p-3">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-sand-500">Recommended specialty</span>
              <Badge status={triage.urgency}>{triage.urgency}</Badge>
            </div>
            <div className="font-extrabold text-sand-900">{triage.specialty}</div>
            {triage.doctors.length === 0 && (
              <p className="text-xs text-sand-500">No {triage.specialty} specialists available right now.</p>
            )}
            {triage.doctors.slice(0, 3).map((d) => (
              <div key={d.doctor_id} className="flex items-center justify-between gap-2 rounded-xl bg-sand-50 p-2.5">
                <div className="flex min-w-0 items-center gap-2.5">
                  <Avatar name={d.full_name} size={36} color="solid" />
                  <div className="min-w-0">
                    <div className="truncate text-xs font-bold text-sand-900">{d.full_name}</div>
                    <div className="flex items-center gap-1.5 text-[11px] text-sand-500">
                      <Star size={11} className="text-warning-500" fill="currentColor" /> {Number(d.rating) > 0 ? d.rating : 'New'}
                      <span>·</span> {money(d.consultation_fee)}
                    </div>
                  </div>
                </div>
                <Button variant="primary" className="px-3 py-1.5 text-xs"
                  onClick={() => navigate('/patient/book', { state: { doctorId: d.doctor_id } })}>
                  Book <ArrowRight size={13} />
                </Button>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Input */}
      <form onSubmit={send} className="flex items-center gap-2 border-t border-sand-100 p-3">
        <input
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          placeholder={mode === 'triage' ? 'Describe your symptoms…' : 'Type your question…'}
          className="flex-1 rounded-full border border-sand-200 bg-sand-50 px-4 py-2.5 text-sm outline-none
                     focus:border-primary-400"
        />
        <button type="submit" disabled={!draft.trim() || status === 'loading'}
          className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-primary-600 text-white
                     disabled:cursor-not-allowed disabled:bg-sand-200 disabled:text-sand-400">
          <Send size={16} />
        </button>
      </form>
    </div>
  )
}
