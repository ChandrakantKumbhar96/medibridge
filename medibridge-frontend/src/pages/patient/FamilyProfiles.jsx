import { useEffect, useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { Users, Plus, Pencil, Trash2, UserRound } from 'lucide-react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import Button from '../../components/common/Button'
import Input, { Field } from '../../components/common/Input'
import { patientNav } from './patientNav'
import {
  fetchFamily, addFamilyMember, updateFamilyMember, removeFamilyMember,
} from '../../features/family/familySlice'

const RELATIONS = ['Child', 'Spouse', 'Parent', 'Sibling', 'Other']
const GENDERS = ['Male', 'Female', 'Other']

const EMPTY = {
  full_name: '', date_of_birth: '', gender: 'Male',
  relation: 'Child', blood_group: '', phone: '',
}

/**
 * The people this account books for.
 *
 * There is no password field and no invite: a dependent is a profile, not a
 * login. Everything here is scoped to the signed-in patient by the server, so
 * nothing on this page ever sends an owner id.
 */
export default function FamilyProfiles() {
  const dispatch = useDispatch()
  const family = useSelector((s) => s.family.list)
  const status = useSelector((s) => s.family.status)

  const [form, setForm] = useState(null)      // null = form closed
  const [editingId, setEditingId] = useState(null)
  const [busy, setBusy] = useState(false)
  const [msg, setMsg] = useState(null)

  useEffect(() => { dispatch(fetchFamily()) }, [dispatch])

  const notify = (text, error = false) => {
    setMsg({ text, error })
    setTimeout(() => setMsg(null), 4000)
  }

  const set = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }))

  const openAdd = () => { setEditingId(null); setForm({ ...EMPTY }) }

  const openEdit = (m) => {
    setEditingId(m.family_member_id)
    setForm({
      full_name: m.full_name,
      date_of_birth: m.date_of_birth || '',
      gender: m.gender || 'Male',
      relation: m.relation || 'Child',
      blood_group: m.blood_group || '',
      phone: m.phone || '',
    })
  }

  const save = async (e) => {
    e.preventDefault()
    setBusy(true)
    try {
      const action = editingId
        ? updateFamilyMember({ id: editingId, member: form })
        : addFamilyMember(form)
      await dispatch(action).unwrap()
      setForm(null)
      setEditingId(null)
      notify(editingId ? 'Profile updated.' : 'Family profile added.')
    } catch (err) {
      notify(err?.response?.data?.message || err?.message || 'Could not save this profile.', true)
    } finally {
      setBusy(false)
    }
  }

  const remove = async (m) => {
    setBusy(true)
    try {
      await dispatch(removeFamilyMember(m.family_member_id)).unwrap()
      notify(`${m.full_name} removed. Their past records stay on your account.`)
    } catch (err) {
      notify(err?.response?.data?.message || 'Could not remove this profile.', true)
    } finally {
      setBusy(false)
    }
  }

  return (
    <DashboardLayout navItems={patientNav}>
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <span className="eyebrow">Your account</span>
          <h1 className="mt-1 text-display-sm text-sand-900">Family profiles</h1>
          <p className="mt-1 text-sand-500">
            Book appointments and keep records for the people you care for, all from this login.
          </p>
        </div>
        <Button onClick={openAdd} disabled={busy}>
          <Plus size={16} /> Add family member
        </Button>
      </div>

      {msg && (
        <div className={`mt-5 rounded-xl px-4 py-2.5 text-sm ${
          msg.error ? 'bg-danger-50 text-danger-600' : 'bg-success-50 text-success-700'}`}>{msg.text}</div>
      )}

      {form && (
        <form onSubmit={save} className="surface mt-6 p-6">
          <h2 className="text-lg font-extrabold text-sand-900">
            {editingId ? 'Edit profile' : 'New family member'}
          </h2>
          <div className="mt-4 grid gap-4 sm:grid-cols-2">
            <Field label="Full name">
              <Input value={form.full_name} onChange={set('full_name')} required maxLength={100}
                placeholder="e.g. Ananya Gupta" />
            </Field>
            <Field label="Date of birth"
              hint="Required — a dose is calculated from it, so it cannot be guessed.">
              <Input type="date" value={form.date_of_birth} onChange={set('date_of_birth')}
                required max={new Date().toISOString().slice(0, 10)} />
            </Field>
            <Field label="Relationship">
              <select value={form.relation} onChange={set('relation')} className={SELECT}>
                {RELATIONS.map((r) => <option key={r} value={r}>{r}</option>)}
              </select>
            </Field>
            <Field label="Gender">
              <select value={form.gender} onChange={set('gender')} className={SELECT}>
                {GENDERS.map((g) => <option key={g} value={g}>{g}</option>)}
              </select>
            </Field>
            <Field label="Blood group">
              <Input value={form.blood_group} onChange={set('blood_group')} maxLength={5}
                placeholder="O+" />
            </Field>
            <Field label="Phone" hint="Optional — yours is used if they have none.">
              <Input value={form.phone} onChange={set('phone')} maxLength={20}
                placeholder="+91 90000 00000" />
            </Field>
          </div>
          <div className="mt-5 flex gap-3">
            <Button type="submit" disabled={busy}>{busy ? 'Saving…' : 'Save profile'}</Button>
            <Button type="button" variant="outline" onClick={() => { setForm(null); setEditingId(null) }}>
              Cancel
            </Button>
          </div>
        </form>
      )}

      <div className="surface mt-6 p-6">
        <h2 className="text-lg font-extrabold text-sand-900">People on this account</h2>

        {status === 'loading' && family.length === 0 && (
          <p className="mt-4 text-sm text-sand-500">Loading…</p>
        )}

        {status !== 'loading' && family.length === 0 && (
          <div className="mt-4 flex flex-col items-center gap-2 rounded-2xl border border-dashed border-sand-200 py-10 text-center">
            <Users size={26} className="text-sand-300" />
            <p className="text-sm text-sand-500">
              No family profiles yet — add one to book on their behalf.
            </p>
          </div>
        )}

        <div className="mt-4 grid gap-3 sm:grid-cols-2">
          {family.map((m) => (
            <div key={m.family_member_id}
              className="flex items-center justify-between gap-3 rounded-2xl border border-sand-100 p-4 transition hover:border-primary-200">
              <div className="flex min-w-0 items-center gap-3">
                <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-primary-50 text-primary-600">
                  <UserRound size={20} />
                </div>
                <div className="min-w-0">
                  <div className="truncate font-bold text-sand-800">{m.full_name}</div>
                  <div className="text-xs text-sand-500">
                    {m.relation}
                    {m.age != null && ` • ${m.age} yrs`}
                    {m.gender && ` • ${m.gender}`}
                    {m.blood_group && ` • ${m.blood_group}`}
                  </div>
                </div>
              </div>
              <div className="flex shrink-0 items-center gap-1">
                <button onClick={() => openEdit(m)} title="Edit"
                  className="flex h-9 w-9 items-center justify-center rounded-xl text-primary-600 transition hover:bg-primary-50">
                  <Pencil size={17} />
                </button>
                <button onClick={() => remove(m)} disabled={busy} title="Remove"
                  className="flex h-9 w-9 items-center justify-center rounded-xl text-sand-400 transition hover:bg-danger-50 hover:text-danger-600 disabled:opacity-50">
                  <Trash2 size={17} />
                </button>
              </div>
            </div>
          ))}
        </div>

        {family.length > 0 && (
          <p className="mt-4 text-xs text-sand-500">
            Removing a profile hides it from booking. Their appointments, reports and
            prescriptions stay on your account.
          </p>
        )}
      </div>
    </DashboardLayout>
  )
}

// Matches Input's shell so the two sit level in the same grid row.
const SELECT = `w-full rounded-xl border border-sand-200 bg-sand-50/60 px-4 py-3 text-sm
                font-medium text-sand-900 outline-none transition-all duration-200
                hover:border-sand-300 hover:bg-white
                focus:border-primary-400 focus:bg-white focus:ring-4 focus:ring-primary-500/10`
