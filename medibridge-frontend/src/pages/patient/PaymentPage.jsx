import { useEffect, useState } from 'react'
import { useLocation, useNavigate, Navigate } from 'react-router-dom'
import { CreditCard, Lock, ShieldCheck, Calendar, Clock } from 'lucide-react'
import DashboardTopbar from '../../components/layout/DashboardTopbar'
import Card from '../../components/common/Card'
import Input, { Field } from '../../components/common/Input'
import Button from '../../components/common/Button'
import Avatar from '../../components/common/Avatar'
import { appointmentService } from '../../services/appointmentService'
import { paymentService } from '../../services/paymentService'
import { openCheckout } from '../../services/razorpay'

const money = (n) => `₹${Number(n ?? 0).toLocaleString('en-IN')}`
const METHOD_LABELS = { card: 'Card', upi: 'UPI', netbanking: 'Net Banking' }
const METHOD_API = { card: 'CARD', upi: 'UPI', netbanking: 'NETBANKING' }

export default function PaymentPage() {
  const { state } = useLocation()
  const navigate = useNavigate()

  const [method, setMethod] = useState('card')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState(null)
  // Decided by the server: real gateway when keys are present, simulated form
  // otherwise. The UI adapts rather than the developer flipping a flag.
  const [gatewayEnabled, setGatewayEnabled] = useState(null)
  // Platform fee comes from the server (system_settings), never hardcoded, so
  // the total shown always matches what the gateway is asked to charge.
  const [platformFee, setPlatformFee] = useState(5)
  // The appointment is booked once and reused on retry — re-booking would
  // collide with the slot hold this checkout already holds.
  const [appointmentId, setAppointmentId] = useState(null)

  useEffect(() => {
    paymentService.getConfig()
      .then((c) => {
        setGatewayEnabled(!!c.gatewayEnabled)
        if (c.platformFee != null) setPlatformFee(Number(c.platformFee))
      })
      .catch(() => setGatewayEnabled(false))
  }, [])

  // Reached directly rather than through the booking wizard - there is nothing
  // to pay for, so send them back rather than charging for a guess.
  if (!state?.doctor || !state?.slot) {
    return <Navigate to="/patient/book" replace />
  }

  const { doctor, date, slot, reason, consultType } = state
  const fee = Number(doctor.consultation_fee) || 0
  const total = fee + Number(platformFee)

  /**
   * Creates the appointment, then pays for it.
   *
   * The appointment is created here rather than at the end of the wizard so a
   * patient who abandons checkout does not leave an orphaned booking holding a
   * slot. The backend creates it as PENDING_PAYMENT and only promotes it to a
   * real request once the payment succeeds.
   */
  const describeError = (err, fallback) => {
    const message = err?.response?.data?.message
    // 409 means someone else took the slot between selection and checkout.
    if (err?.response?.status === 409 && message) {
      return `${message} Please go back and choose another time.`
    }
    return message || fallback
  }

  const pay = async (e) => {
    e.preventDefault()
    setError(null)
    setSubmitting(true)

    // Book once, then reuse. A retry (patient reopened checkout after closing
    // it) must not re-book — the slot is already held by the first attempt, so
    // re-booking would fail with a misleading "slot taken" against their own hold.
    let apptId = appointmentId
    if (!apptId) {
      try {
        const appointment = await appointmentService.bookAppointment({
          doctor_id: doctor.doctor_id,
          schedule_id: slot.schedule_id,
          consult_type: consultType || 'Consultation',
          reason: reason || null,
        })
        apptId = appointment.appointment_id
        setAppointmentId(apptId)
      } catch (err) {
        setError(describeError(err, 'Could not create the appointment.'))
        setSubmitting(false)
        return
      }
    }

    if (gatewayEnabled) {
      await payViaGateway(apptId)
    } else {
      await paySimulated(apptId)
    }
  }

  /** Real money: order created server-side, signature verified server-side. */
  const payViaGateway = async (appointmentId) => {
    try {
      const order = await paymentService.createOrder(appointmentId)

      await openCheckout(order, {
        onSuccess: async (response) => {
          try {
            // Until this verify call succeeds the payment does not count,
            // however convincing the gateway's success screen looked.
            await paymentService.verify({
              razorpay_order_id: response.razorpay_order_id,
              razorpay_payment_id: response.razorpay_payment_id,
              razorpay_signature: response.razorpay_signature,
            })
            navigate('/patient/appointments', { replace: true })
          } catch (err) {
            setError(describeError(err, 'We could not verify your payment. '
              + 'If you were charged, please contact support.'))
            setSubmitting(false)
          }
        },
        onDismiss: () => {
          paymentService.markFailed(order.order_id, 'Checkout closed by user')
          setError('Payment was cancelled. Your appointment is not confirmed yet.')
          setSubmitting(false)
        },
        onFailure: (reasonText) => {
          paymentService.markFailed(order.order_id, reasonText)
          setError(reasonText || 'Payment failed. Please try another method.')
          setSubmitting(false)
        },
      })
    } catch (err) {
      setError(describeError(err, 'Could not start the payment. Please try again.'))
      setSubmitting(false)
    }
  }

  /** No gateway keys configured - record the transaction without charging. */
  const paySimulated = async (appointmentId) => {
    try {
      await paymentService.pay({
        appointment_id: appointmentId,
        payment_method: METHOD_API[method],
        amount: total,
      })
      navigate('/patient/appointments', { replace: true })
    } catch (err) {
      setError(describeError(err, 'Payment could not be completed. Please try again.'))
      setSubmitting(false)
    }
  }

  return (
    <div className="min-h-screen bg-sand-50">
      <DashboardTopbar />
      <div className="mx-auto max-w-5xl px-6 py-8">
        <h1 className="text-3xl font-extrabold text-sand-900">Checkout</h1>
        <p className="mt-1 text-sand-500">Complete payment to confirm your appointment</p>

        {error && (
          <div className="mt-5 rounded-xl bg-danger-50 px-4 py-3 text-sm text-danger-600">{error}</div>
        )}

        <div className="mt-6 grid gap-6 lg:grid-cols-5">
          {/* Summary card */}
          <div className="lg:col-span-2">
            <div className="overflow-hidden rounded-4xl bg-gradient-to-br from-primary-700 to-primary-900 bg-mesh-teal p-6 text-white shadow-lift">
              <div className="text-[11px] font-bold uppercase tracking-[0.16em] text-white/70">
                MediBridge booking
              </div>
              <div className="mt-4 flex items-center gap-3">
                <Avatar name={doctor.full_name} size={48} />
                <div className="min-w-0">
                  <div className="truncate text-lg font-bold">{doctor.full_name}</div>
                  <div className="text-sm text-white/80">{doctor.specialization}</div>
                </div>
              </div>
              <div className="mt-5 space-y-2 text-sm text-white/90">
                <div className="flex items-center gap-2"><Calendar size={15} /> {date}</div>
                <div className="flex items-center gap-2"><Clock size={15} /> {slot.label}</div>
              </div>
              <div className="mt-6 border-t border-white/20 pt-4">
                <div className="text-[11px] uppercase tracking-wide text-white/60">Total payable</div>
                <div className="text-3xl font-extrabold">{money(total)}</div>
              </div>
            </div>

            <div className="surface mt-4 p-4">
              <div className="flex items-center gap-2 text-sm text-sand-600">
                <ShieldCheck size={18} className="text-success-600" />
                Payments are encrypted and processed securely.
              </div>
            </div>
          </div>

          {/* Payment form */}
          <div className="lg:col-span-3">
            <Card>
              <h2 className="text-lg font-bold text-sand-900">Payment Method</h2>

              {/* With a real gateway, card details are collected by Razorpay's
                  own hosted window - never by this form. That is deliberate:
                  card numbers must never touch our page or our server. */}
              {gatewayEnabled ? (
                <form onSubmit={pay} className="mt-4 space-y-4">
                  <div className="rounded-xl border border-sand-200 p-4">
                    <div className="flex items-center gap-3">
                      <ShieldCheck size={20} className="text-success-600" />
                      <div>
                        <div className="text-sm font-semibold text-sand-800">
                          Secure checkout by Razorpay
                        </div>
                        <div className="text-xs text-sand-500">
                          Pay by UPI, card, net banking or wallet. Your card details
                          are entered on Razorpay&apos;s secure window, never here.
                        </div>
                      </div>
                    </div>
                  </div>

                  <div className="rounded-xl bg-sand-50 p-4 text-sm">
                    <div className="flex justify-between text-sand-600"><span>Consultation Fee</span><span>{money(fee)}</span></div>
                    <div className="mt-2 flex justify-between text-sand-600"><span>Platform Fee</span><span>{money(platformFee)}</span></div>
                    <div className="mt-3 flex justify-between border-t border-sand-200 pt-3 font-bold text-sand-900"><span>Total</span><span>{money(total)}</span></div>
                  </div>

                  <Button type="submit" disabled={submitting}
                    variant={submitting ? 'disabled' : 'primary'} className="w-full py-3">
                    <Lock size={16} /> {submitting ? 'Opening secure checkout…' : `Pay ${money(total)} Securely`}
                  </Button>
                </form>
              ) : (
                <>
                  <div className="mt-4 grid grid-cols-3 gap-3">
                    {Object.keys(METHOD_LABELS).map((m) => (
                      <button key={m} type="button" onClick={() => setMethod(m)}
                        className={`rounded-xl border py-2.5 text-sm font-semibold transition ${
                          method === m ? 'border-primary-600 bg-primary-50 text-primary-600' : 'border-sand-200 text-sand-600'}`}>
                        {METHOD_LABELS[m]}
                      </button>
                    ))}
                  </div>

                  <form onSubmit={pay} className="mt-6 space-y-4">
                    {method === 'card' && (
                      <>
                        <Field label="Cardholder Name"><Input placeholder="John Doe" /></Field>
                        <Field label="Card Number"><Input icon={CreditCard} placeholder="1234 5678 9012 3456" /></Field>
                        <div className="grid grid-cols-2 gap-4">
                          <Field label="Expiry"><Input placeholder="MM/YY" /></Field>
                          <Field label="CVV"><Input icon={Lock} placeholder="123" /></Field>
                        </div>
                      </>
                    )}
                    {method === 'upi' && <Field label="UPI ID"><Input placeholder="name@bank" /></Field>}
                    {method === 'netbanking' && (
                      <Field label="Select Bank">
                        <select className="w-full rounded-xl border border-sand-200 bg-sand-50/60 px-4 py-3 text-sm font-medium text-sand-900 outline-none transition-all hover:border-sand-300 hover:bg-white focus:border-primary-400 focus:bg-white focus:ring-4 focus:ring-primary-500/10">
                          <option>State Bank</option><option>HDFC</option><option>ICICI</option><option>Axis</option>
                        </select>
                      </Field>
                    )}

                    <div className="rounded-xl bg-warning-50 px-4 py-2.5 text-xs text-warning-700">
                      Demo mode — no gateway keys configured, so no money is charged
                      and these fields are not sent anywhere.
                    </div>

                    <div className="rounded-xl bg-sand-50 p-4 text-sm">
                      <div className="flex justify-between text-sand-600"><span>Consultation Fee</span><span>{money(fee)}</span></div>
                      <div className="mt-2 flex justify-between text-sand-600"><span>Platform Fee</span><span>{money(platformFee)}</span></div>
                      <div className="mt-3 flex justify-between border-t border-sand-200 pt-3 font-bold text-sand-900"><span>Total</span><span>{money(total)}</span></div>
                    </div>

                    <Button type="submit" disabled={submitting}
                      variant={submitting ? 'disabled' : 'primary'} className="w-full py-3">
                      <Lock size={16} /> {submitting ? 'Processing…' : 'Pay & Confirm Booking'}
                    </Button>
                  </form>
                </>
              )}
            </Card>
          </div>
        </div>
      </div>
    </div>
  )
}
