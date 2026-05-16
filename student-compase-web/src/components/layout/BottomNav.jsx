import { Home, MessageSquare, ShoppingBag, MapPin } from 'lucide-react'
import { NavLink } from 'react-router-dom'

const NAV_ITEMS = [
  { to: '/dashboard', icon: Home,          label: 'Home'   },
  { to: '/chat',      icon: MessageSquare, label: 'Chat'   },
  { to: '/market',    icon: ShoppingBag,   label: 'Market' },
  { to: '/radar',     icon: MapPin,        label: 'Radar'  },
]

export default function BottomNav() {
  return (
    <nav className="fixed bottom-0 left-0 right-0 z-40 md:hidden safe-bottom">
      <div
        className="glass-card border-t border-white/10"
        style={{ paddingTop: '0.75rem', paddingBottom: 'max(env(safe-area-inset-bottom, 0px), 0.5rem)' }}
      >
        <div className="flex items-center justify-around px-2">
          {NAV_ITEMS.map(({ to, icon: Icon, label }) => (
            <NavLink
              key={to}
              to={to}
              className={({ isActive }) =>
                `flex flex-col items-center gap-1 px-5 py-1 rounded-2xl font-medium text-xs transition-all ${
                  isActive
                    ? 'text-purple-400'
                    : 'text-slate-500 hover:text-slate-300'
                }`
              }
            >
              {({ isActive }) => (
                <>
                  <div
                    className={`p-1.5 rounded-xl transition-all ${
                      isActive ? 'bg-purple-500/20' : ''
                    }`}
                  >
                    <Icon size={20} />
                  </div>
                  <span>{label}</span>
                </>
              )}
            </NavLink>
          ))}
        </div>
      </div>
    </nav>
  )
}
