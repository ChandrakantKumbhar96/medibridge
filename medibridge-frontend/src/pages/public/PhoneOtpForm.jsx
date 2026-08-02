import { useEffect, useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { useNavigate } from 'react-router-dom'
import { ArrowLeft, KeyRound, Smartphone } from 'lucide-react'
import Input, { Field } from '../../components/common/Input'
import { requestOtp, verifyOtp } from '../../features/auth/authSlice'

/**
 * Phone-first sign-in: number, code, done.
 *
 * <p>Patients only. A doctor account means a licence an admin verified and an
 * admin account is seeded — neither is something holding a SIM should get you.
 *
 * <p>There is no "register" branch. The server creates the account on a
 * successful verification, so an unknown number and a known one look exactly
 * the same from here — which is also why the first step's response says nothing
 * about whether the number is registered.
 */
export default function PhoneOtpForm({ onUsePassword }) {
  const [step, setStep] = useState('phone')
  const [phone, setPhone] = useState('')
  const [code, setCode] = useState('')

  // Both come from the server: an admin can retune the code length and the
  // cooldown, and a hardcoded 6 / 60 here would quietly start lying.
  const [codeLength, setCodeLength] = useState(6)
  const [cooldown, setCooldown] = useState(0)

  const dispatch = useDispatch()
  const navigate = useNavigate()
  const { status, error } = useSelector((s) => s.auth)
  const busy = status === 'loading'

  useEffect(() => {
    if (cooldown <= 0) return undefined
    const tick = setTimeout(() => setCooldown((s) => s - 1), 1000)
    return () => clearTimeout(tick)
  }, [cooldown])

  const send = async (e) => {
    e?.preventDefault()
    const res = await dispatch(requestOtp(phone))
    if (requestOtp.fulfilled.match(res)) {
      setCodeLength(res.payload.code_length || 6)
      setCooldown(res.payload.resend_in_seconds || 60)
      setCode('')
      setStep('code')
    }
  }

  const submit = async (e) => {
    e.preventDefault()
    const res = await dispatch(verifyOtp({ phone, code }))
    if (verifyOtp.fulfilled.match(res)) {
      // A number that just registered has a placeholder name and no email. The
      // dashboard would render a page of blanks; settings is where they fix it.
      navigate(res.payload.user?.profile_complete === false
        ? '/patient/settings'
        : '/patient')
    }
  }

  const Banner = () => error && (
    <div className="mt-5 rounded-xl border border-danger-100 bg-danger-50 px-4 py-3 text-[13px] font-medium text-danger-700">
      {error}
    </div>
  )

  const submitButton = (label, disabled) => (
    <button
      type="submit"
      disabled={disabled}
      className="mt-6 w-full rounded-full bg-gradient-to-b from-primary-500 to-primary-600 py-3.5
                 text-sm font-bold text-white shadow-[0_8px_24px_-8px_rgba(37,99,235,.6)]
                 transition-all hover:from-primary-600 hover:to-primary-700
                 disabled:cursor-not-allowed disabled:opacity-60"
    >
      {label}
    </button>
  )

  if (step === 'phone') {
    return (
      <form onSubmit={send} className="mt-7">
        <h2 className="text-[26px] font-extrabold tracking-[-0.03em] text-sand-900">
          Sign in with your mobile
        </h2>
        <p className="mt-1 text-[14px] text-sand-500">
          We’ll text you a {codeLength}-digit code. No password needed.
        </p>

        <Banner />

        <div className="mt-6">
          <Field label="Mobile number" hint="Indian numbers can skip the +91.">
            <Input
              icon={Smartphone}
              type="tel"
              inputMode="tel"
              autoComplete="tel"
              required
              placeholder="+91 90000 11111"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
            />
          </Field>
        </div>

        {submitButton(busy ? 'Sending…' : 'Send code', busy || phone.trim().length < 8)}

        <button
          type="button"
          onClick={onUsePassword}
          className="mt-4 w-full text-[13px] font-bold text-primary-600 hover:text-primary-700"
        >
          Use email and password instead
        </button>
      </form>
    )
  }

  return (
    <form onSubmit={submit} className="mt-7">
      <h2 className="text-[26px] font-extrabold tracking-[-0.03em] text-sand-900">
        Enter your code
      </h2>
      <p className="mt-1 text-[14px] text-sand-500">
        Sent to <span className="font-bold text-sand-700">{phone}</span>
      </p>

      <Banner />

      <div className="mt-6">
        <Field label="Verification code">
          <Input
            icon={KeyRound}
            inputMode="numeric"
            autoComplete="one-time-code"
            required
            maxLength={codeLength}
            placeholder={'0'.repeat(codeLength)}
            className="text-center text-lg tracking-[0.5em]"
            value={code}
            /* Digits only: the server rejects anything else anyway, and a
               pasted "code: 123456" should not become a failed attempt. */
            onChange={(e) => setCode(e.target.value.replace(/\D/g, ''))}
          />
        </Field>
      </div>

      {submitButton(busy ? 'Verifying…' : 'Verify & sign in',
        busy || code.length !== codeLength)}

      <div className="mt-4 flex items-center justify-between text-[13px]">
        <button
          type="button"
          onClick={() => setStep('phone')}
          className="inline-flex items-center gap-1.5 font-bold text-sand-500 hover:text-sand-700"
        >
          <ArrowLeft size={14} strokeWidth={2.4} /> Change number
        </button>

        {cooldown > 0 ? (
          // Driven by the server's own cooldown, so the button re-enables when
          // a resend would actually be accepted rather than 429'd.
          <span className="font-medium text-sand-400">Resend in {cooldown}s</span>
        ) : (
          <button
            type="button"
            onClick={send}
            disabled={busy}
            className="font-bold text-primary-600 hover:text-primary-700 disabled:opacity-60"
          >
            Resend code
          </button>
        )}
      </div>
    </form>
  )
}
