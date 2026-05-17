import { useState, useEffect } from 'react'
import { NavLink, useNavigate, useLocation } from 'react-router-dom'
import {
  Home, Wallet, MapPin, Sparkles, ShoppingBag,
  ListOrdered, Plus, Zap, User, LogOut, X, Menu,
} from 'lucide-react'
import { useAuth } from '../../hooks/useAuth'

const NAV_GROUPS = [
  {
    label: 'Main',
    items: [
      { to: '/dashboard',       icon: Home,        label: 'Home'         },
      { to: '/budget',          icon: Wallet,      label: 'Budget'       },
      { to: '/radar',           icon: MapPin,      label: 'Radar Deals'  },
      { to: '/recommendations', icon: Sparkles,    label: 'AI Picks'     },
    ],
  },
  {
    label: 'Marketplace',
    items: [
      { to: '/market',            icon: ShoppingBag, label: 'Browse'       },
      { to: '/marketplace/me',    icon: ListOrdered, label: 'My Listings'  },
      { to: '/marketplace/sell',  icon: Plus,        label: 'Sell Item', featured: true },
    ],
  },
  {
    label: 'Tools',
    items: [
      { to: '/chat',    icon: Zap,  label: 'AI Chat' },
      { to: '/profile', icon: User, label: 'Profile' },
    ],
  },
]

function DrawerLink({ to, icon: Icon, label, featured, onClose }) {
  return (
    <NavLink
      to={to}
      onClick={onClose}
      className={({ isActive }) =>
        `flex items-center gap-3 px-4 py-3 rounded-2xl font-semibold text-sm transition-all ${
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
  )
}

export default function MobileNav() {
  const [open, setOpen]   = useState(false)
  const { user, logout }  = useAuth()
  const navigate          = useNavigate()
  const location          = useLocation()

  // Close drawer on navigation
  useEffect(() => { setOpen(false) }, [location.pathname])

  // Lock body scroll while drawer is open
  useEffect(() => {
    if (open) document.body.style.overflow = 'hidden'
    else       document.body.style.overflow = ''
    return () => { document.body.style.overflow = '' }
  }, [open])

  const handleLogout = async () => {
    setOpen(false)
    await logout()
    navigate('/')
  }

  const initials = [user?.firstName?.[0], user?.lastName?.[0]]
    .filter(Boolean).join('').toUpperCase() || '?'

  return (
    <>
      {/* ── Fixed top header (mobile only) ──────────────────── */}
      <header
        className="fixed top-0 left-0 right-0 h-14 md:hidden flex items-center justify-between px-4 glass-card border-b border-white/10"
        style={{ zIndex: 2000 }}
      >
        {/* Logo */}
        <div className="flex items-center gap-2.5">
          <div
            className="w-8 h-8 rounded-xl bg-purple-500 flex items-center justify-center flex-shrink-0"
            style={{ boxShadow: '0 0 18px rgba(168,85,247,0.5)' }}
          >
            <span className="text-white font-black text-sm leading-none">S</span>
          </div>
          <span className="text-lg font-black text-slate-100 tracking-tight">StuFi</span>
        </div>

        {/* Hamburger */}
        <button
          onClick={() => setOpen(true)}
          aria-label="Open navigation menu"
          className="w-10 h-10 rounded-2xl flex items-center justify-center text-slate-300
                     hover:bg-white/8 hover:text-slate-100 active:scale-90 transition-all"
        >
          <Menu size={22} />
        </button>
      </header>

      {/* ── Backdrop ─────────────────────────────────────────── */}
      <div
        onClick={() => setOpen(false)}
        aria-hidden="true"
        className={`fixed inset-0 md:hidden transition-opacity duration-300 ${
          open ? 'opacity-100 pointer-events-auto' : 'opacity-0 pointer-events-none'
        }`}
        style={{
          zIndex: 1999,
          background: 'rgba(2, 6, 23, 0.75)',
          backdropFilter: 'blur(4px)',
          WebkitBackdropFilter: 'blur(4px)',
        }}
      />

      {/* ── Slide-in drawer ───────────────────────────────────── */}
      <aside
        className={`fixed top-0 left-0 h-full w-72 md:hidden flex flex-col glass-card border-r border-white/10
                    transition-transform duration-300 ease-in-out ${
                      open ? 'translate-x-0' : '-translate-x-full'
                    }`}
        style={{ zIndex: 2000 }}
        aria-label="Navigation drawer"
      >
        {/* Drawer header */}
        <div className="flex items-center justify-between px-5 h-14 flex-shrink-0 border-b border-white/8">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-xl bg-purple-500 flex items-center justify-center"
              style={{ boxShadow: '0 0 18px rgba(168,85,247,0.5)' }}
            >
              <span className="text-white font-black text-sm leading-none">S</span>
            </div>
            <span className="text-lg font-black text-slate-100 tracking-tight">StuFi</span>
          </div>
          <button
            onClick={() => setOpen(false)}
            aria-label="Close menu"
            className="w-9 h-9 rounded-xl flex items-center justify-center text-slate-400
                       hover:bg-white/8 hover:text-slate-100 active:scale-90 transition-all"
          >
            <X size={18} />
          </button>
        </div>

        {/* Nav groups */}
        <nav className="flex-1 overflow-y-auto py-4 px-3 space-y-5">
          {NAV_GROUPS.map((group) => (
            <div key={group.label}>
              <p className="px-4 mb-1.5 text-[0.62rem] font-black text-slate-600 uppercase tracking-widest">
                {group.label}
              </p>
              <div className="space-y-0.5">
                {group.items.map((item) => (
                  <DrawerLink key={item.to} {...item} onClose={() => setOpen(false)} />
                ))}
              </div>
            </div>
          ))}
        </nav>

        {/* User card + logout */}
        <div className="flex-shrink-0 border-t border-white/8 px-3 py-4 space-y-2">
          <div className="flex items-center gap-3 px-4 py-3 rounded-2xl bg-slate-800/70">
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
            Sign Out
          </button>
        </div>
      </aside>
    </>
  )
}
