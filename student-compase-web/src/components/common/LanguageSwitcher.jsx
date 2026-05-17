import { useTranslation } from 'react-i18next'

const LANGS = [
  { code: 'en', flag: '🇬🇧', label: 'EN' },
  { code: 'ro', flag: '🇷🇴', label: 'RO' },
]

/**
 * @param {'pill'|'row'} variant
 *   pill  – compact toggle button used in the desktop Sidebar footer
 *   row   – full-width row item used inside the MobileNav drawer
 */
export default function LanguageSwitcher({ variant = 'pill' }) {
  const { i18n } = useTranslation()
  const current  = i18n.language?.slice(0, 2) ?? 'en'

  if (variant === 'row') {
    return (
      <div className="flex items-center gap-1 px-4 py-2.5 rounded-2xl">
        <span className="text-xs font-black text-slate-600 uppercase tracking-widest flex-1">
          Language
        </span>
        <div className="flex items-center gap-1 p-0.5 rounded-xl bg-slate-800/60 border border-white/8">
          {LANGS.map(({ code, flag, label }) => {
            const active = current === code
            return (
              <button
                key={code}
                onClick={() => i18n.changeLanguage(code)}
                aria-label={`Switch to ${label}`}
                className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-bold
                            transition-all duration-200 ${
                              active
                                ? 'bg-purple-500 text-white shadow-[0_0_12px_rgba(168,85,247,0.4)]'
                                : 'text-slate-400 hover:text-slate-100 hover:bg-white/5'
                            }`}
              >
                <span className="text-base leading-none">{flag}</span>
                <span>{label}</span>
              </button>
            )
          })}
        </div>
      </div>
    )
  }

  /* ── variant === 'pill' (default) ─────────────────────────── */
  return (
    <div
      className="flex items-center gap-1 p-0.5 rounded-xl bg-slate-800/60 border border-white/8"
      role="group"
      aria-label="Language switcher"
    >
      {LANGS.map(({ code, flag, label }) => {
        const active = current === code
        return (
          <button
            key={code}
            onClick={() => i18n.changeLanguage(code)}
            aria-label={`Switch to ${label}`}
            className={`flex items-center gap-1 px-2.5 py-1.5 rounded-lg text-xs font-bold
                        transition-all duration-200 ${
                          active
                            ? 'bg-purple-500 text-white shadow-[0_0_10px_rgba(168,85,247,0.35)]'
                            : 'text-slate-400 hover:text-slate-100 hover:bg-white/5'
                        }`}
          >
            <span className="text-sm leading-none">{flag}</span>
            <span>{label}</span>
          </button>
        )
      })}
    </div>
  )
}
