import { useEffect, useRef, useState } from 'react'

/**
 * Plays the existing fade-up animation the first time this element scrolls
 * into view, instead of on mount — animate-fade-up alone is invisible for
 * anything below the fold since it has already fired before you scroll to it.
 */
export default function Reveal({ children, className = '', delay = 0, as: Tag = 'div' }) {
  const ref = useRef(null)
  const [visible, setVisible] = useState(false)

  useEffect(() => {
    const el = ref.current
    if (!el) return
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setVisible(true)
          observer.disconnect()
        }
      },
      { threshold: 0.15 }
    )
    observer.observe(el)
    return () => observer.disconnect()
  }, [])

  return (
    <Tag
      ref={ref}
      style={delay ? { animationDelay: `${delay}ms` } : undefined}
      className={`${visible ? 'animate-fade-up' : 'opacity-0'} ${className}`}
    >
      {children}
    </Tag>
  )
}
