import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useDispatch, useSelector } from 'react-redux'
import { Shield, Mail, Lock } from 'lucide-react'
import Input, { Field } from '../../components/common/Input'
import { login } from '../../features/auth/authSlice'

export default function AdminLoginPage() {
  const [form, setForm] = useState({ email: '', password: '' })
  const dispatch = useDispatch()
  const navigate = useNavigate()
  const { status, error } = useSelector((s) => s.auth)

  const submit = async (e) => {
    e.preventDefault()
    const res = await dispatch(login({ ...form, role: 'admin' }))
    if (login.fulfilled.match(res)) navigate('/admin')
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-slate-900 to-slate-800 p-6">
      <div className="w-full max-w-md rounded-2xl bg-white p-8 shadow-xl">
        <div className="flex flex-col items-center text-center">
          <div className="flex h-14 w-14 items-center justify-center rounded-full bg-red-500 text-white">
            <Shield size={26} />
          </div>
          <h1 className="mt-4 text-2xl font-extrabold text-slate-900">Admin Portal</h1>
          <p className="mt-1 text-sm text-slate-500">Restricted access — administrators only</p>
        </div>

        {error && <div className="mt-6 rounded-lg bg-red-50 px-4 py-2.5 text-sm text-red-600">{error}</div>}

        <form onSubmit={submit} className="mt-6 space-y-4">
          <Field label="Admin Email">
            <Input icon={Mail} type="email" required placeholder="admin@medibridge.com"
              value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
          </Field>
          <Field label="Password">
            <Input icon={Lock} type="password" required placeholder="Enter your password"
              value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
          </Field>
          <button type="submit" disabled={status === 'loading'}
            className="w-full rounded-lg bg-red-500 py-3 text-sm font-semibold text-white hover:bg-red-600 disabled:opacity-60">
            {status === 'loading' ? 'Signing in...' : 'Sign in to Admin Panel'}
          </button>
        </form>

        <div className="mt-6 text-center">
          <Link to="/login" className="text-sm font-medium text-slate-500 hover:text-primary-600">← Back to user login</Link>
        </div>
      </div>
    </div>
  )
}
