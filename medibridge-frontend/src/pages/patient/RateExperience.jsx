import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Star } from 'lucide-react'
import DashboardTopbar from '../../components/layout/DashboardTopbar'
import Card from '../../components/common/Card'
import Avatar from '../../components/common/Avatar'
import Button from '../../components/common/Button'

const experiences = ['Excellent', 'Good', 'Okay', 'Poor']
const highlights = ['Bedside Manner', 'Clear Explanations', 'Follow-up Care', 'Accurate Diagnosis', 'Friendly Staff']

export default function RateExperience() {
  const navigate = useNavigate()
  const [stars, setStars] = useState(0)
  const [hover, setHover] = useState(0)
  const [overall, setOverall] = useState('Excellent')
  const [tags, setTags] = useState([])
  const [review, setReview] = useState('')

  const toggleTag = (t) => setTags((p) => (p.includes(t) ? p.filter((x) => x !== t) : [...p, t]))

  return (
    <div className="min-h-screen bg-slate-50">
      <DashboardTopbar />
      <div className="mx-auto max-w-2xl px-6 py-8">
        <Card>
          <div className="flex items-center gap-4 border-b border-slate-100 pb-5">
            <Avatar color="solid" size={52} />
            <div>
              <div className="font-bold text-slate-900">Dr. Sarah Johnson</div>
              <div className="text-sm text-slate-500">Cardiologist • Consultation on 2026-05-05</div>
            </div>
          </div>

          <h1 className="mt-6 text-center text-2xl font-extrabold text-slate-900">Rate Your Experience</h1>

          <div className="mt-4 flex justify-center gap-2">
            {[1, 2, 3, 4, 5].map((n) => (
              <button key={n} onMouseEnter={() => setHover(n)} onMouseLeave={() => setHover(0)} onClick={() => setStars(n)}>
                <Star size={40} className={(hover || stars) >= n ? 'text-amber-400' : 'text-slate-300'} fill={(hover || stars) >= n ? 'currentColor' : 'none'} />
              </button>
            ))}
          </div>

          <div className="mt-8">
            <div className="text-sm font-semibold text-slate-700">How was your overall experience?</div>
            <div className="mt-3 grid grid-cols-4 gap-3">
              {experiences.map((e) => (
                <button key={e} onClick={() => setOverall(e)}
                  className={`rounded-lg border py-3 text-sm font-medium transition ${
                    overall === e ? 'border-primary-600 bg-primary-50 text-primary-600' : 'border-slate-200 text-slate-600'}`}>
                  {e}
                </button>
              ))}
            </div>
          </div>

          <div className="mt-6">
            <div className="text-sm font-semibold text-slate-700">What stood out?</div>
            <div className="mt-3 flex flex-wrap gap-2">
              {highlights.map((h) => (
                <button key={h} onClick={() => toggleTag(h)}
                  className={`rounded-full border px-4 py-2 text-sm font-medium transition ${
                    tags.includes(h) ? 'border-primary-600 bg-primary-50 text-primary-600' : 'border-slate-200 text-slate-600'}`}>
                  {h}
                </button>
              ))}
            </div>
          </div>

          <div className="mt-6">
            <div className="text-sm font-semibold text-slate-700">Write a review</div>
            <textarea rows={4} value={review} onChange={(e) => setReview(e.target.value)}
              placeholder="Share details of your experience with this doctor..."
              className="mt-2 w-full rounded-lg border border-slate-300 px-3.5 py-2.5 text-sm" />
          </div>

          <Button onClick={() => navigate('/patient/appointments')} className="mt-6 w-full py-3">Submit Review</Button>
        </Card>
      </div>
    </div>
  )
}
