const CHECKOUT_SRC = 'https://checkout.razorpay.com/v1/checkout.js'

let scriptPromise = null

/** Loads the Razorpay checkout script once. */
export function loadRazorpay() {
  if (scriptPromise) return scriptPromise

  scriptPromise = new Promise((resolve, reject) => {
    if (window.Razorpay) return resolve(window.Razorpay)

    const existing = document.querySelector(`script[src="${CHECKOUT_SRC}"]`)
    if (existing) {
      existing.addEventListener('load', () => resolve(window.Razorpay))
      existing.addEventListener('error', () => reject(new Error('Could not load Razorpay')))
      return
    }

    const script = document.createElement('script')
    script.src = CHECKOUT_SRC
    script.async = true
    script.onload = () => resolve(window.Razorpay)
    script.onerror = () => reject(new Error('Could not load Razorpay'))
    document.body.appendChild(script)
  })

  return scriptPromise
}

/**
 * Opens Razorpay checkout for a server-created order.
 *
 * <p>Resolves with the gateway's success payload, which the caller must send to
 * the backend for signature verification. Note the amount and key id come from
 * the server's order response - nothing about the price is decided here.
 */
export function openCheckout(order, { onSuccess, onDismiss, onFailure }) {
  return loadRazorpay().then((Razorpay) => {
    const rzp = new Razorpay({
      key: order.razorpay_key_id,
      amount: order.amount_paise,
      currency: order.currency,
      name: 'MediBridge',
      description: `Consultation with ${order.doctor}`,
      order_id: order.order_id,
      prefill: {
        name: order.patient_name,
        email: order.patient_email,
        contact: order.patient_phone || '',
      },
      theme: { color: '#2563eb' },
      handler: (response) => onSuccess(response),
      modal: {
        ondismiss: () => onDismiss && onDismiss(),
      },
    })

    rzp.on('payment.failed', (response) => {
      onFailure && onFailure(response?.error?.description || 'Payment failed')
    })

    rzp.open()
  })
}
