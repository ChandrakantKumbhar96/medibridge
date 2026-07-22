import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Plus, Trash2, FileText, Download, ArrowLeft } from 'lucide-react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import Card from '../../components/common/Card'
import Input, { Field } from '../../components/common/Input'
import Button from '../../components/common/Button'
import Avatar from '../../components/common/Avatar'
import { doctorNav } from './doctorNav'
import { appointmentService } from '../../services/appointmentService'
import { prescriptionService } from '../../services/prescriptionService'

const BLANK_MEDICINE = {
  medicine_name: '', dosage: '', frequency: '1-0-1', duration: '', instructions: '',
}

// The Indian convention: morning-afternoon-night.
const FREQUENCIES = ['1-0-0', '0-1-0', '0-0-1', '1-0-1', '1-1-1', '0-0-0 (SOS)']

export default function WritePrescription() {
  const { appointmentId } = useParams()
  const navigate = useNavigate()

  const [appointment, setAppointment] = useState(null)
  const [form, setForm] = useState({
    diagnosis: '', notes: '', follow_up_date: '', advice: '',
  })
  const [medicines, setMedicines] = useState([{ ...BLANK_MEDICINE }])
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState(null)
  const [issued, setIssued] = useState(null)

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  useEffect(() => {
    appointmentService.getDoctorAppointments()
      .then((list) => {
        const match = list.find((a) => String(a.appointment_id) === String(appointmentId))
        if (!match) setError('Appointment not found, or it is not one of yours.')
        setAppointment(match || null)
      })
      .catch(() => setError('Could not load the appointment.'))
  }, [appointmentId])

  const updateMedicine = (index, key, value) =>
    setMedicines((prev) => prev.map((m, i) => (i === index ? { ...m, [key]: value } : m)))

  const addMedicine = () => setMedicines((prev) => [...prev, { ...BLANK_MEDICINE }])

  const removeMedicine = (index) =>
    setMedicines((prev) => (prev.length === 1 ? prev : prev.filter((_, i) => i !== index)))

  const submit = async (e) => {
    e.preventDefault()
    setError(null)

    const filled = medicines.filter((m) => m.medicine_name.trim())
    if (filled.length === 0) {
      setError('Add at least one medicine.')
      return
    }

    setSaving(true)
    try {
      const result = await prescriptionService.create({
        appointment_id: Number(appointmentId),
        diagnosis: form.diagnosis,
        notes: form.notes || null,
        follow_up_date: form.follow_up_date || null,
        advice: form.advice || null,
        medicines: filled,
      })
      // Issuing a prescription completes the consultation, so there is nothing
      // left to edit - show the result instead of the form.
      setIssued(result)
    } catch (err) {
      setError(err?.response?.data?.message || 'Could not save the prescription.')
    } finally {
      setSaving(false)
    }
  }

  if (issued) {
    return (
      <DashboardLayout badge="Doctor" navItems={doctorNav}>
        <Card className="mx-auto mt-10 max-w-lg text-center">
          <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-green-100 text-green-600">
            <FileText size={26} />
          </div>
          <h1 className="mt-4 text-2xl font-extrabold text-slate-900">Prescription issued</h1>
          <p className="mt-2 text-sm text-slate-500">
            {issued.patient_name} has been notified and can download it from their
            Medical Records.
          </p>
          <div className="mt-6 flex justify-center gap-3">
            <Button onClick={() => prescriptionService.downloadPdf(issued.prescription_id)}
              className="px-5">
              <Download size={16} /> Download PDF
            </Button>
            <button onClick={() => navigate('/doctor/appointments')}
              className="rounded-lg border border-slate-300 px-5 text-sm font-semibold text-slate-700 hover:bg-slate-50">
              Back to appointments
            </button>
          </div>
        </Card>
      </DashboardLayout>
    )
  }

  return (
    <DashboardLayout badge="Doctor" navItems={doctorNav}>
      <button onClick={() => navigate('/doctor/appointments')}
        className="flex items-center gap-1.5 text-sm font-medium text-slate-500 hover:text-primary-600">
        <ArrowLeft size={16} /> Back to appointments
      </button>

      <h1 className="mt-3 text-3xl font-extrabold text-slate-900">Write Prescription</h1>
      <p className="mt-1 text-slate-500">Record the consultation and issue medicines</p>

      {error && (
        <div className="mt-5 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-600">{error}</div>
      )}

      {appointment && (
        <Card className="mt-6 flex items-center gap-4">
          <Avatar color="solid" size={48} />
          <div>
            <div className="font-bold text-slate-900">{appointment.patient}</div>
            <div className="text-sm text-slate-500">
              {appointment.age} yrs • {appointment.appointment_date} at {appointment.time}
            </div>
            {appointment.reason && (
              <div className="mt-1 text-sm text-slate-600">
                Reported: {appointment.reason}
              </div>
            )}
          </div>
        </Card>
      )}

      <form onSubmit={submit}>
        <Card className="mt-6">
          <h2 className="text-lg font-bold text-slate-900">Consultation</h2>
          <div className="mt-5 space-y-4">
            <Field label="Diagnosis">
              <Input required value={form.diagnosis} onChange={set('diagnosis')}
                placeholder="e.g. Hypertension, Stage 1" />
            </Field>
            <Field label="Clinical notes">
              <textarea rows={3} value={form.notes} onChange={set('notes')}
                placeholder="Observations, vitals, examination findings"
                className="w-full rounded-lg border border-slate-300 px-3.5 py-2.5 text-sm" />
            </Field>
            <div className="grid gap-4 sm:grid-cols-2">
              <Field label="Follow-up date (optional)">
                <Input type="date" value={form.follow_up_date}
                  min={new Date().toISOString().split('T')[0]}
                  onChange={set('follow_up_date')} />
              </Field>
            </div>
          </div>
        </Card>

        <Card className="mt-6">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-bold text-slate-900">Medicines</h2>
            <button type="button" onClick={addMedicine}
              className="flex items-center gap-1.5 rounded-lg border border-slate-300 px-3 py-1.5 text-sm font-semibold text-slate-700 hover:bg-slate-50">
              <Plus size={15} /> Add medicine
            </button>
          </div>

          <div className="mt-5 space-y-5">
            {medicines.map((m, i) => (
              <div key={i} className="rounded-xl border border-slate-200 p-4">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-semibold uppercase tracking-wide text-slate-400">
                    Medicine {i + 1}
                  </span>
                  {medicines.length > 1 && (
                    <button type="button" onClick={() => removeMedicine(i)}
                      className="text-slate-400 hover:text-red-600">
                      <Trash2 size={16} />
                    </button>
                  )}
                </div>

                <div className="mt-3 grid gap-3 sm:grid-cols-2">
                  <Field label="Medicine name">
                    <Input value={m.medicine_name}
                      onChange={(e) => updateMedicine(i, 'medicine_name', e.target.value)}
                      placeholder="e.g. Amlodipine" />
                  </Field>
                  <Field label="Dosage">
                    <Input value={m.dosage}
                      onChange={(e) => updateMedicine(i, 'dosage', e.target.value)}
                      placeholder="e.g. 5 mg" />
                  </Field>
                  <Field label="Frequency">
                    <select value={m.frequency}
                      onChange={(e) => updateMedicine(i, 'frequency', e.target.value)}
                      className="w-full rounded-lg border border-slate-300 px-3.5 py-2.5 text-sm">
                      {FREQUENCIES.map((f) => <option key={f}>{f}</option>)}
                    </select>
                  </Field>
                  <Field label="Duration">
                    <Input value={m.duration}
                      onChange={(e) => updateMedicine(i, 'duration', e.target.value)}
                      placeholder="e.g. 30 days" />
                  </Field>
                </div>
                <div className="mt-3">
                  <Field label="Instructions (optional)">
                    <Input value={m.instructions}
                      onChange={(e) => updateMedicine(i, 'instructions', e.target.value)}
                      placeholder="e.g. After breakfast" />
                  </Field>
                </div>
              </div>
            ))}
          </div>
        </Card>

        <Card className="mt-6">
          <h2 className="text-lg font-bold text-slate-900">General advice</h2>
          <textarea rows={3} value={form.advice} onChange={set('advice')}
            placeholder="Diet, activity, warning signs to watch for"
            className="mt-4 w-full rounded-lg border border-slate-300 px-3.5 py-2.5 text-sm" />

          <div className="mt-5 rounded-lg bg-blue-50 px-4 py-2.5 text-xs text-primary-700">
            Issuing this prescription marks the consultation complete and notifies
            the patient. It cannot be edited afterwards.
          </div>

          <Button type="submit" disabled={saving} variant={saving ? 'disabled' : 'primary'}
            className="mt-5 w-full py-3">
            {saving ? 'Issuing…' : 'Issue Prescription'}
          </Button>
        </Card>
      </form>
    </DashboardLayout>
  )
}
