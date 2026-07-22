import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useDispatch, useSelector } from 'react-redux'
import { Search, Star } from 'lucide-react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import Card from '../../components/common/Card'
import Badge from '../../components/common/Badge'
import Avatar from '../../components/common/Avatar'
import Button from '../../components/common/Button'
import Input from '../../components/common/Input'
import { patientNav } from './patientNav'
import { fetchDoctors } from '../../features/doctors/doctorsSlice'
import { doctorService } from '../../services/doctorService'

export default function FindDoctors() {
  const dispatch = useDispatch()
  const navigate = useNavigate()
  const doctors = useSelector((s) => s.doctors.list)
  const [specializations, setSpecializations] = useState([])
  const [q, setQ] = useState('')
  const [spec, setSpec] = useState('All Specializations')

  useEffect(() => { dispatch(fetchDoctors()) }, [dispatch])

  useEffect(() => {
    doctorService.getSpecializations().then(setSpecializations).catch(() => setSpecializations([]))
  }, [])

  const filtered = doctors.filter((d) => {
    const matchQ = `${d.full_name} ${d.specialization}`.toLowerCase().includes(q.toLowerCase())
    // Prefix match, not equality: seeded rows say 'Cardiologist' while the
    // specialization list says 'Cardiology'.
    const matchS = spec === 'All Specializations'
      || d.specialization.toLowerCase().startsWith(spec.toLowerCase().slice(0, 5))
    return matchQ && matchS
  })

  return (
    <DashboardLayout navItems={patientNav}>
      <h1 className="text-3xl font-extrabold text-slate-900">Find Doctors</h1>
      <p className="mt-1 text-slate-500">Search for doctors by specialization</p>

      <Card className="mt-6 flex flex-col gap-3 sm:flex-row">
        <div className="flex-1">
          <Input icon={Search} placeholder="Search by doctor name or specialization..." value={q} onChange={(e) => setQ(e.target.value)} />
        </div>
        <select className="rounded-lg border border-slate-300 px-4 py-2.5 text-sm text-slate-700" value={spec} onChange={(e) => setSpec(e.target.value)}>
          <option>All Specializations</option>
          {specializations.map((s) => <option key={s}>{s}</option>)}
        </select>
      </Card>

      <div className="mt-5 space-y-4">
        {filtered.map((d) => (
          <Card key={d.doctor_id} className="flex flex-wrap items-center justify-between gap-4">
            <div className="flex items-center gap-4">
              <Avatar color="solid" size={52} />
              <div>
                <div className="font-bold text-slate-900">{d.full_name}</div>
                <div className="text-sm text-slate-500">{d.specialization}</div>
                <div className="mt-1 flex items-center gap-2 text-sm">
                  <span className="flex items-center gap-1 text-amber-500"><Star size={14} fill="currentColor" /> {d.rating}</span>
                  <span className="text-slate-400">• {d.experience_years} years experience</span>
                  <Badge status={d.available ? 'Available' : 'Unavailable'} className="text-[11px]" />
                </div>
              </div>
            </div>
            <div className="flex items-center gap-2">
              <Button variant="outline" className="px-4 py-2">View Profile</Button>
              <Button variant={d.available ? 'primary' : 'disabled'} disabled={!d.available} className="px-4 py-2"
                onClick={() => navigate('/patient/book', { state: { doctorId: d.doctor_id } })}>
                Book Appointment
              </Button>
            </div>
          </Card>
        ))}
      </div>
    </DashboardLayout>
  )
}
