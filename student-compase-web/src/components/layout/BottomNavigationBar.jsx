import { Home, MapPin, Plus, ShoppingBag, User } from 'lucide-react'
import { NavLink } from 'react-router-dom'

const leftItems = [
  { to: '/dashboard', icon: Home, label: 'Home' },
  { to: '/market', icon: ShoppingBag, label: 'Market' },
]

const rightItems = [
  { to: '/radar', icon: MapPin, label: 'Radar' },
  { to: '/profile', icon: User, label: 'Profile' },
]

function NavItem({ to, icon: Icon, label }) {
  return (
    <NavLink
      to={to}
      className={({ isActive }) =>
        `flex min-w-0 flex-1 flex-col items-center gap-1 rounded-2xl px-2 py-1.5 text-xs font-bold ${
          isActive ? 'text-purple-300' : 'text-slate-500 hover:text-slate-300'
        }`
      }
    >
      {({ isActive }) => (
        <>
          <span className={`flex h-8 w-8 items-center justify-center rounded-xl ${isActive ? 'bg-purple-500/20' : ''}`}>
            <Icon size={20} />
          </span>
          <span className="truncate">{label}</span>
        </>
      )}
    </NavLink>
  )
}

export default function BottomNavigationBar() {
  return (
    <nav className="fixed bottom-0 left-0 right-0 z-40 md:hidden safe-bottom">
      <div
        className="glass-card border-t border-white/10 px-2"
        style={{ paddingTop: '0.65rem', paddingBottom: 'max(env(safe-area-inset-bottom, 0px), 0.55rem)' }}
      >
        <div className="grid grid-cols-[1fr_1fr_4.25rem_1fr_1fr] items-center gap-1">
          {leftItems.map((item) => (
            <NavItem key={item.to} {...item} />
          ))}

          <NavLink
            to="/marketplace/sell"
            className={({ isActive }) =>
              `mx-auto -mt-8 flex h-16 w-16 items-center justify-center rounded-full border-4 border-slate-900 bg-purple-500 text-white shadow-[0_0_34px_rgba(168,85,247,0.55)] active:scale-95 ${
                isActive ? 'ring-2 ring-purple-200/60' : 'hover:bg-purple-400'
              }`
            }
            aria-label="Sell item"
          >
            <Plus size={28} strokeWidth={3} />
          </NavLink>

          {rightItems.map((item) => (
            <NavItem key={item.to} {...item} />
          ))}
        </div>
      </div>
    </nav>
  )
}
