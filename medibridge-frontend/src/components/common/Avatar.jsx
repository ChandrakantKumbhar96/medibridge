import { useState } from 'react'
import { User } from 'lucide-react'

/**
 * Avatar with three levels of graceful degradation:
 *
 *   1. an explicit `src` image, or an illustrated avatar generated from `name`
 *   2. the person's initials on a brand-tinted background, if the image fails
 *   3. a generic icon, when there is no name at all
 *
 * Illustrated avatars are used rather than stock portrait photography on
 * purpose: attaching a real person's face to a fictional doctor with a
 * fabricated registration number is not something a healthcare product should
 * ship, even in a demo.
 */

const tones = {
  blue:   'bg-primary-100 text-primary-700 ring-primary-200',
  gray:   'bg-sand-100 text-sand-500 ring-sand-200',
  yellow: 'bg-warning-100 text-warning-700 ring-warning-100',
  green:  'bg-success-100 text-success-700 ring-success-100',
  solid:  'bg-gradient-to-br from-primary-500 to-primary-700 text-white ring-primary-200',
  red:    'bg-gradient-to-br from-accent-500 to-accent-700 text-white ring-accent-200',
}

function initialsOf(name) {
  return name
    .replace(/^(Dr\.?|Mr\.?|Mrs\.?|Ms\.?)\s+/i, '')
    .split(/\s+/)
    .slice(0, 2)
    .map((w) => w[0])
    .join('')
    .toUpperCase()
}

export default function Avatar({
  color = 'blue',
  size = 40,
  icon: Icon = User,
  name,
  src,
  ring = true,
  className = '',
}) {
  const [failed, setFailed] = useState(false)

  const imageUrl =
    src ||
    (name
      ? `https://api.dicebear.com/9.x/notionists/svg?seed=${encodeURIComponent(name)}` +
        `&backgroundColor=D6F5EF,FFE2DA,F1EEE9&radius=50`
      : null)

  const base = `relative flex flex-shrink-0 items-center justify-center overflow-hidden rounded-full ${
    ring ? 'ring-2 ring-offset-2 ring-offset-white' : ''
  } ${tones[color] || tones.blue} ${className}`

  if (imageUrl && !failed) {
    return (
      <img
        src={imageUrl}
        alt={name || 'Avatar'}
        onError={() => setFailed(true)}
        loading="lazy"
        className={`${base} bg-sand-100 object-cover`}
        style={{ width: size, height: size }}
      />
    )
  }

  return (
    <div className={base} style={{ width: size, height: size }} aria-label={name}>
      {name ? (
        <span className="font-bold leading-none" style={{ fontSize: size * 0.36 }}>
          {initialsOf(name)}
        </span>
      ) : (
        <Icon size={size * 0.46} strokeWidth={2} />
      )}
    </div>
  )
}
