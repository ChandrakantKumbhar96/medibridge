import { useEffect, useRef, useState } from 'react'

/**
 * Counts up from 0 to `value` once it scrolls into view, instead of
 * rendering the number statically — the "climbing number" pattern real
 * marketing sites use to make trust stats feel alive.
 */
export default function StatCounter({ value, suffix = '', decimals = 0, duration = 1400, className = '' }) {
  const ref = useRef(null)
  const started = useRef(false)
  const [display, setDisplay] = useState(decimals ? (0).toFixed(decimals) : '0')

  useEffect(() => {
    const el = ref.current
    if (!el) return
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting && !started.current) {
          started.current = true
          const start = performance.now()
          const tick = (now) => {
            const progress = Math.min((now - start) / duration, 1)
            const eased = 1 - Math.pow(1 - progress, 3)
            const current = eased * value
            setDisplay(decimals ? current.toFixed(decimals) : Math.round(current).toLocaleString())
            if (progress < 1) requestAnimationFrame(tick)
          }
          requestAnimationFrame(tick)
          observer.disconnect()
        }
      },
      { threshold: 0.4 }
    )
    observer.observe(el)
    return () => observer.disconnect()
  }, [value, duration, decimals])

  return (
    <span ref={ref} className={className}>
      {display}{suffix}
    </span>
  )
}
