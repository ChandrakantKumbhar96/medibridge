import { useEffect, useState } from 'react'
import { useLocation, useNavigate, Navigate } from 'react-router-dom'
import { Star, CheckCircle2 } from 'lucide-react'
import DashboardTopbar from '../../components/layout/DashboardTopbar'
import Card from '../../components/common/Card'
import Avatar from '../../components/common/Avatar'
import Button from '../../components/common/Button'
import { reviewService } from '../../services/reviewService'

const experiences = ['Excellent', 'Good', 'Okay', 'Poor']
const highlights = ['Bedside Manner', 'Clear Explanations', 'Follow-up Care',
  'Accurate Diagnosis', 'Friendly Staff']

export default function RateExperience() {
  const { state } = useLocation()
  const navigate = useNavigate()

  const [stars, setStars] = useState(0)
  const [hover, setHover] = useState(0)
  const [overall, setOverall] = useState('Excellent')
  const [tags, setTags] = useState([])
  const [review, setReview] = useState('')

  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState(null)
  const [done, setDone] = useState(false)
  const [alreadyReviewed, setAlreadyReviewed] = useState(false)

  const appointment = state?.appointment

  // Reached directly rather than from an appointment - there is nothing to review.
  useEffect(() => {
    if (!appointment?.appointment_id) return
    reviewService.getForAppointment(appointment.appointment_id)
      .then((existing) => { if (existing) setAlreadyReviewed(true) })
      .catch(() => { /* 404 means no review yet, which is the normal case */ })
  }, [appointment])

  if (!appointment) {
    return <Navigate to="/patient/appointments" replace />
  }

  const toggleTag = (t) =>
    setTags((p) => (p.includes(t) ? p.filter((x) => x !== t) : [...p, t]))

  const submit = async () => {
    setError(null)

    if (stars === 0) {
      setError('Please select a star rating.')
      return
    }

    setSubmitting(true)
    try {
      await reviewService.submit({
        appointment_id: appointment.appointment_id,
        stars,
        overall_experience: overall,
        // Sent as an array - all selected tags persist, one row each.
        what_stood_out: tags,
        review_text: review || null,
      })
      setDone(true)
    } catch (err) {
      setError(err?.response?.data?.message || 'Could not submit your review.')
      setSubmitting(false)
    }
  }

  if (done) {
    return (
      <div className="min-h-screen bg-sand-50">
        <DashboardTopbar />
        <div className="mx-auto max-w-lg px-6 py-16">
          <Card className="text-center">
            <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-success-100 text-success-600">
              <CheckCircle2 size={28} />
            </div>
            <h1 className="mt-4 text-2xl font-extrabold text-sand-900">Thank you</h1>
            <p className="mt-2 text-sm text-sand-500">
              Your feedback helps other patients choose the right doctor.
            </p>
            <Button onClick={() => navigate('/patient/appointments')} className="mt-6 px-6">
              Back to appointments
            </Button>
          </Card>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-sand-50">
      <DashboardTopbar />
      <div className="mx-auto max-w-2xl px-6 py-8">
        <Card>
          <div className="flex items-center gap-4 border-b border-sand-100 pb-5">
            <Avatar name={appointment.doctor} size={52} />
            <div>
              <div className="font-bold text-sand-900">{appointment.doctor}</div>
              <div className="text-sm text-sand-500">
                {appointment.specialization} • Consultation on {appointment.appointment_date}
              </div>
            </div>
          </div>

          {alreadyReviewed && (
            <div className="mt-5 rounded-xl bg-warning-50 px-4 py-2.5 text-sm text-warning-700">
              You have already reviewed this consultation.
            </div>
          )}

          <h1 className="mt-6 text-center text-2xl font-extrabold text-sand-900">
            Rate Your Experience
          </h1>

          {error && (
            <div className="mt-4 rounded-xl bg-danger-50 px-4 py-2.5 text-sm text-danger-600">
              {error}
            </div>
          )}

          <div className="mt-4 flex justify-center gap-2">
            {[1, 2, 3, 4, 5].map((n) => (
              <button key={n} type="button"
                onMouseEnter={() => setHover(n)} onMouseLeave={() => setHover(0)}
                onClick={() => setStars(n)} aria-label={`${n} star${n > 1 ? 's' : ''}`}>
                <Star size={40}
                  className={(hover || stars) >= n ? 'text-warning-400' : 'text-sand-300'}
                  fill={(hover || stars) >= n ? 'currentColor' : 'none'} />
              </button>
            ))}
          </div>

          <div className="mt-8">
            <div className="text-sm font-semibold text-sand-700">
              How was your overall experience?
            </div>
            <div className="mt-3 grid grid-cols-4 gap-3">
              {experiences.map((e) => (
                <button key={e} type="button" onClick={() => setOverall(e)}
                  className={`rounded-xl border py-3 text-sm font-medium transition ${
                    overall === e ? 'border-primary-600 bg-primary-50 text-primary-600'
                                  : 'border-sand-200 text-sand-600'}`}>
                  {e}
                </button>
              ))}
            </div>
          </div>

          <div className="mt-6">
            <div className="text-sm font-semibold text-sand-700">
              What stood out? <span className="font-normal text-sand-400">(select any)</span>
            </div>
            <div className="mt-3 flex flex-wrap gap-2">
              {highlights.map((h) => (
                <button key={h} type="button" onClick={() => toggleTag(h)}
                  className={`rounded-full border px-4 py-2 text-sm font-medium transition ${
                    tags.includes(h) ? 'border-primary-600 bg-primary-50 text-primary-600'
                                     : 'border-sand-200 text-sand-600'}`}>
                  {h}
                </button>
              ))}
            </div>
          </div>

          <div className="mt-6">
            <div className="text-sm font-semibold text-sand-700">Write a review</div>
            <textarea rows={4} value={review} onChange={(e) => setReview(e.target.value)}
              placeholder="Share details of your experience with this doctor..."
              className="mt-2 w-full rounded-xl border border-sand-200 bg-sand-50/60 px-4 py-3 text-sm font-medium text-sand-900 outline-none transition-all placeholder:font-normal placeholder:text-sand-400 hover:border-sand-300 hover:bg-white focus:border-primary-400 focus:bg-white focus:ring-4 focus:ring-primary-500/10" />
          </div>

          <Button onClick={submit} disabled={submitting || alreadyReviewed}
            variant={submitting || alreadyReviewed ? 'disabled' : 'primary'}
            className="mt-6 w-full py-3">
            {submitting ? 'Submitting…' : 'Submit Review'}
          </Button>
        </Card>
      </div>
    </div>
  )
}
