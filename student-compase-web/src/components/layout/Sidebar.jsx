import { Home, ShoppingBag, MapPin, LogOut, Plus, User, Wallet, Zap, ListOrdered } from 'lucide-react'
import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth'

const NAV_ITEMS = [
  { to: '/dashboard', icon: Home,          label: 'Home'        },
  { to: '/budget',    icon: Wallet,        label: 'Budget'      },
  { to: '/chat',      icon: Zap,           label: 'AI Chat'     },
  { to: '/market',          icon: ShoppingBag,  label: 'Marketplace'  },
  { to: '/marketplace/me',  icon: ListOrdered,  label: 'My Listings'  },
  { to: '/marketplace/sell', icon: Plus,        label: 'Sell Item', featured: true },
  { to: '/radar',     icon: MapPin,        label: 'Radar Deals' },
  { to: '/profile',   icon: User,          label: 'Profile'     },
]

export default function Sidebar() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = async () => {
    await logout()
    navigate('/')
  }

  const initials = [user?.firstName?.[0], user?.lastName?.[0]]
    .filter(Boolean)
    .join('')
    .toUpperCase() || '?'

  return (
    <aside className="hidden md:flex flex-col w-64 h-screen fixed left-0 top-0 z-30 glass-card border-r border-white/10 p-6">
      {/* Logo */}
      <div className="flex items-center gap-3 mb-10">
        <div className="w-9 h-9 rounded-xl bg-purple-500 flex items-center justify-center shadow-lg shadow-purple-500/40">
          <span className="text-white font-black text-base">S</span>
        </div>
        <span className="text-xl font-black text-slate-100 tracking-tight">StuFi</span>
      </div>

      {/* Navigation */}
      <nav className="flex-1 space-y-1">
        {NAV_ITEMS.map(({ to, icon: Icon, label, featured }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) =>
              `flex items-center gap-3 px-4 py-3 rounded-2xl font-semibold text-sm transition-all ${
                featured
                  ? 'bg-purple-500 text-white shadow-[0_0_28px_rgba(168,85,247,0.35)] hover:bg-purple-400'
                  : isActive
                  ? 'bg-purple-500/20 text-purple-400 border border-purple-500/30'
                  : 'text-slate-400 hover:text-slate-100 hover:bg-white/5'
              }`
            }
          >
            <Icon size={18} />
            {label}
          </NavLink>
        ))}
      </nav>

      {/* User card + logout */}
      <div className="space-y-2 pt-4 border-t border-white/10">
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
          className="w-full flex items-center gap-3 px-4 py-3 rounded-2xl text-slate-500 hover:text-red-400 hover:bg-red-500/10 font-semibold text-sm"
        >
          <LogOut size={18} />
          Sign Out
        </button>
      </div>
    </aside>
  )
}
