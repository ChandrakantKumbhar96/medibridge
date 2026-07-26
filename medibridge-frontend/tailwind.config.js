/** @type {import('tailwindcss').Config} */

/**
 * MediBridge design tokens.
 *
 * A clinical blue primary (like Practo, Apollo 24|7, 1mg, Zocdoc, Halodoc),
 * a warm coral accent, and sand-toned neutrals instead of cold slate — the
 * palette real telemedicine products use to read as trustworthy, kept off the
 * bare Tailwind defaults by the custom scale + sand neutrals.
 */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        /* Primary — clinical blue, matching how real telemedicine apps
           (Practo, Apollo 24|7, 1mg, Zocdoc, Halodoc) present trust. */
        primary: {
          50:  '#EFF6FF',
          100: '#DBEAFE',
          200: '#BFDBFE',
          300: '#93C5FD',
          400: '#60A5FA',
          500: '#3B82F6',
          600: '#2563EB',
          700: '#1D4ED8',
          800: '#1E40AF',
          900: '#1E3A8A',
        },

        /* Accent — warm coral. Used sparingly for emphasis and human warmth. */
        accent: {
          50:  '#FFF3F0',
          100: '#FFE2DA',
          200: '#FFC5B5',
          300: '#FF9F86',
          400: '#FF7A5C',
          500: '#F65F3E',
          600: '#E04A2B',
          700: '#BC3A1F',
          800: '#992F19',
          900: '#7A2614',
        },

        /* Neutrals — warm sand, not cold slate. This single swap does more to
           break the "AI template" look than any other change. */
        sand: {
          25:  '#FDFCFA',
          50:  '#F8F6F3',
          100: '#F1EEE9',
          200: '#E5E0D8',
          300: '#CFC8BD',
          400: '#A8A096',
          500: '#7D766C',
          600: '#5C564E',
          700: '#443F39',
          800: '#2C2925',
          900: '#1A1815',
        },

        /* Semantic — warmed to sit beside the sand neutrals.
           Full 50–900 scales so any shade referenced in a page resolves; a
           missing shade fails silently in Tailwind and leaves an unstyled element. */
        success: {
          50: '#ECFDF3', 100: '#D1FADF', 200: '#A6F4C5', 300: '#6CE9A6', 400: '#32D583',
          500: '#12B76A', 600: '#039855', 700: '#027A48', 800: '#05603A', 900: '#054F31',
        },
        warning: {
          50: '#FFFAEB', 100: '#FEF0C7', 200: '#FEDF89', 300: '#FEC84B', 400: '#FDB022',
          500: '#F79009', 600: '#DC6803', 700: '#B54708', 800: '#93370D', 900: '#7A2E0E',
        },
        danger: {
          50: '#FEF3F2', 100: '#FEE4E2', 200: '#FECDCA', 300: '#FDA29B', 400: '#F97066',
          500: '#F04438', 600: '#D92D20', 700: '#B42318', 800: '#912018', 900: '#7A271A',
        },
        info: {
          50: '#EFF8FF', 100: '#D1E9FF', 200: '#B2DDFF', 300: '#84CAFF', 400: '#53B1FD',
          500: '#2E90FA', 600: '#1570EF', 700: '#175CD3', 800: '#1849A9', 900: '#194185',
        },
      },

      fontFamily: {
        sans: ['"Plus Jakarta Sans"', 'ui-sans-serif', 'system-ui', 'sans-serif'],
        display: ['"Plus Jakarta Sans"', 'ui-sans-serif', 'system-ui', 'sans-serif'],
      },

      fontSize: {
        /* Display sizes with tight tracking — editorial, not default. */
        'display-sm': ['2.25rem', { lineHeight: '1.15', letterSpacing: '-0.02em', fontWeight: '800' }],
        'display':    ['3rem',    { lineHeight: '1.08', letterSpacing: '-0.03em', fontWeight: '800' }],
        'display-lg': ['3.75rem', { lineHeight: '1.05', letterSpacing: '-0.035em', fontWeight: '800' }],
      },

      borderRadius: {
        xl2: '1.125rem',
        '4xl': '2rem',
      },

      boxShadow: {
        /* Layered and brand-tinted. Flat uniform shadow-sm is a tell. */
        soft:  '0 1px 2px rgba(26,24,21,.04), 0 4px 12px -2px rgba(26,24,21,.06)',
        card:  '0 1px 3px rgba(26,24,21,.05), 0 12px 32px -12px rgba(37,99,235,.18)',
        lift:  '0 2px 8px rgba(26,24,21,.06), 0 20px 48px -16px rgba(37,99,235,.28)',
        glow:  '0 0 0 4px rgba(59,130,246,.12)',
        'glow-accent': '0 0 0 4px rgba(246,95,62,.12)',
        inner_soft: 'inset 0 1px 2px rgba(26,24,21,.05)',
      },

      backgroundImage: {
        'mesh-teal':
          'radial-gradient(at 12% 18%, rgba(59,130,246,.16) 0px, transparent 55%),' +
          'radial-gradient(at 88% 12%, rgba(246,95,62,.12) 0px, transparent 50%),' +
          'radial-gradient(at 70% 88%, rgba(96,165,250,.14) 0px, transparent 55%)',
        'grid-sand':
          'linear-gradient(rgba(207,200,189,.35) 1px, transparent 1px),' +
          'linear-gradient(90deg, rgba(207,200,189,.35) 1px, transparent 1px)',
      },

      backgroundSize: { 'grid-sand': '32px 32px' },

      keyframes: {
        'fade-up': {
          '0%':   { opacity: '0', transform: 'translateY(12px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        float: {
          '0%,100%': { transform: 'translateY(0)' },
          '50%':     { transform: 'translateY(-8px)' },
        },
        shimmer: {
          '100%': { transform: 'translateX(100%)' },
        },
      },
      animation: {
        'fade-up': 'fade-up .5s cubic-bezier(.16,1,.3,1) both',
        float: 'float 6s ease-in-out infinite',
        shimmer: 'shimmer 1.6s infinite',
      },
    },
  },
  plugins: [],
}
