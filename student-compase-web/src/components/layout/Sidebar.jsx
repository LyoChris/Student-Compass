import {
  Home, Wallet, MapPin, Sparkles,
  ShoppingBag, ListOrdered, Plus,
  Zap, User, LogOut,
} from 'lucide-react'
import { NavLink, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../../hooks/useAuth'
import LanguageSwitcher from '../common/LanguageSwitcher'

export default function Sidebar() {
  const { user, logout } = useAuth()
  const navigate         = useNavigate()
  const { t }            = useTranslation()

  const NAV_GROUPS = [
    {
      label: t('nav.groups.main'),
      items: [
        { to: '/dashboard',       icon: Home,        label: t('nav.home')       },
        { to: '/budget',          icon: Wallet,      label: t('nav.budget')     },
        { to: '/radar',           icon: MapPin,      label: t('nav.radarDeals') },
        { to: '/recommendations', icon: Sparkles,    label: t('nav.aiPicks')    },
      ],
    },
    {
      label: t('nav.groups.marketplace'),
      items: [
        { to: '/market',           icon: ShoppingBag, label: t('nav.browse')      },
        { to: '/marketplace/me',   icon: ListOrdered, label: t('nav.myListings')  },
        { to: '/marketplace/sell', icon: Plus,        label: t('nav.sellItem'), featured: true },
      ],
    },
    {
      label: t('nav.groups.tools'),
      items: [
        { to: '/profile', icon: User, label: t('nav.profile') },
      ],
    },
  ]

  const handleLogout = async () => {
    await logout()
    navigate('/')
  }

  const initials = [user?.firstName?.[0], user?.lastName?.[0]]
    .filter(Boolean).join('').toUpperCase() || '?'

  return (
    <aside className="hidden md:flex flex-col w-64 h-screen fixed left-0 top-0 z-30 glass-card border-r border-white/10">
      {/* Logo */}
      <div className="flex items-center px-4 h-20 flex-shrink-0 border-b border-white/8">
        <img
          src="/logo.png"
          alt="StuFi"
          className="h-16 w-auto object-contain"
          draggable="false"
        />
      </div>

      {/* Navigation */}
      <nav className="flex-1 overflow-y-auto py-4 px-3 space-y-5">
        {NAV_GROUPS.map((group) => (
          <div key={group.label}>
            <p className="px-4 mb-1.5 text-[0.62rem] font-black text-slate-600 uppercase tracking-widest">
              {group.label}
            </p>
            <div className="space-y-0.5">
              {group.items.map(({ to, icon: Icon, label, featured }) => (
                <NavLink
                  key={to}
                  to={to}
                  className={({ isActive }) =>
                    `flex items-center gap-3 px-4 py-2.5 rounded-2xl font-semibold text-sm transition-all ${
                      featured
                        ? 'bg-purple-500 text-white shadow-[0_0_24px_rgba(168,85,247,0.35)] hover:bg-purple-400'
                        : isActive
                        ? 'bg-purple-500/20 text-purple-300 border border-purple-500/30'
                        : 'text-slate-400 hover:text-slate-100 hover:bg-white/5'
                    }`
                  }
                >
                  <Icon size={17} className="flex-shrink-0" />
                  {label}
                </NavLink>
              ))}
            </div>
          </div>
        ))}
      </nav>

      {/* Language switcher */}
      <div className="flex-shrink-0 border-t border-white/8 px-3 pt-3 pb-1 flex items-center justify-between">
        <span className="text-[0.62rem] font-black text-slate-600 uppercase tracking-widest px-1">
          {t('common.language')}
        </span>
        <LanguageSwitcher variant="pill" />
      </div>

      {/* User card + logout */}
      <div className="flex-shrink-0 px-3 py-3 space-y-2">
        <div className="flex items-center gap-3 px-4 py-3 rounded-2xl bg-slate-800/60">
          <div className="w-9 h-9 rounded-full bg-purple-500/20 border border-purple-500/40 flex items-center justify-center flex-shrink-0">
            <span className="text-purple-400 font-bold text-sm">{initials}</span>
          </div>
          <div className="min-w-0">
            <p className="text-sm font-bold text-slate-100 truncate">
              {user?.firstName} {user?.lastName}
            </p>
            <p className="text-xs text-slate-500 truncate">{user?.email}</p>
          </div>
        </div>

        <button
          onClick={handleLogout}
          className="w-full flex items-center gap-3 px-4 py-3 rounded-2xl text-slate-500
                     hover:text-rose-400 hover:bg-rose-500/10 font-semibold text-sm transition-all"
        >
          <LogOut size={17} />
          {t('common.signOut')}
        </button>
      </div>
    </aside>
  )
}
