import { useState, useEffect } from 'react'
import {
  Bell, Plus, TrendingUp, BookOpen, MapPin, Zap,
  Upload, ChevronRight, ArrowUpRight, ArrowDownRight,
  DollarSign, CreditCard, PiggyBank,
} from 'lucide-react'
import BottomNav from '../components/layout/BottomNav'
import Sidebar from '../components/layout/Sidebar'
import InstallBanner from '../components/common/InstallBanner'
import { useAuth } from '../hooks/useAuth'

function Sk({ className }) {
  return <div className={`skeleton ${className}`} />
}

// ─── Financial Health Gauge ───────────────────────────────────────────────────
function HealthGauge({ percentage, loading }) {
  if (loading) {
    return (
      <div className="glass-card rounded-3xl p-5">
        <div className="flex items-center justify-between mb-4">
          <Sk className="h-3.5 w-32" />
          <Sk className="h-6 w-20 rounded-full" />
        </div>
        <div className="flex items-center gap-6">
          <Sk className="w-28 h-28 rounded-full flex-shrink-0" />
          <div className="flex-1 space-y-3">
            <Sk className="h-7 w-28" />
            <Sk className="h-4 w-44" />
            <Sk className="h-4 w-36" />
          </div>
        </div>
      </div>
    )
  }

  const radius        = 44
  const circumference = 2 * Math.PI * radius
  const offset        = circumference - (percentage / 100) * circumference

  const { gaugeColor, badgeColor, label } =
    percentage >= 75
      ? { gaugeColor: '#22C55E', badgeColor: 'bg-green-500/20 text-green-400',   label: 'Good Job!'    }
      : percentage >= 50
      ? { gaugeColor: '#F59E0B', badgeColor: 'bg-yellow-500/20 text-yellow-400', label: 'Stay on track' }
      : { gaugeColor: '#EF4444', badgeColor: 'bg-red-500/20 text-red-400',       label: 'Be careful!'  }

  return (
    <div className="glass-card rounded-3xl p-5 hover:border-purple-500/30">
      <div className="flex items-center justify-between mb-4">
        <p className="text-slate-400 text-sm font-semibold">Financial Health</p>
        <span className={`text-xs font-bold px-2.5 py-1 rounded-full ${badgeColor}`}>
          This week
        </span>
      </div>
      <div className="flex items-center gap-6">
        <div className="relative w-28 h-28 flex-shrink-0">
          <svg
            width="112"
            height="112"
            viewBox="0 0 112 112"
            style={{ transform: 'rotate(-90deg)' }}
          >
            <circle
              cx="56" cy="56" r={radius}
              fill="none"
              stroke="rgba(255,255,255,0.07)"
              strokeWidth="10"
            />
            <circle
              cx="56" cy="56" r={radius}
              fill="none"
              stroke={gaugeColor}
              strokeWidth="10"
              strokeLinecap="round"
              strokeDasharray={circumference}
              strokeDashoffset={offset}
              style={{ transition: 'stroke-dashoffset 1.4s cubic-bezier(0.4,0,0.2,1)' }}
            />
          </svg>
          <div className="absolute inset-0 flex flex-col items-center justify-center">
            <span className="text-2xl font-black text-slate-100 leading-none">{percentage}%</span>
          </div>
        </div>

        <div className="flex-1 min-w-0">
          <p className="text-xl font-black text-slate-100 leading-tight">{label}</p>
          <p className="text-slate-400 text-sm mt-1.5 leading-relaxed">
            You have{' '}
            <span className="text-green-400 font-bold">RON 250</span> left this week.
          </p>
          <div className="flex items-center gap-1.5 mt-3">
            <TrendingUp size={13} className="text-green-400" />
            <span className="text-xs text-green-400 font-semibold">+12% vs last week</span>
          </div>
        </div>
      </div>
    </div>
  )
}

// ─── Quick stats strip ────────────────────────────────────────────────────────
function QuickStats({ loading }) {
  const stats = [
    { label: 'Budget', value: 'RON 800', Icon: DollarSign, delta: null, iconColor: 'text-slate-400',  valueColor: 'text-slate-100'   },
    { label: 'Spent',  value: 'RON 550', Icon: CreditCard, delta: -3,   iconColor: 'text-orange-400', valueColor: 'text-orange-400'  },
    { label: 'Saved',  value: 'RON 120', Icon: PiggyBank,  delta: +18,  iconColor: 'text-green-400',  valueColor: 'text-green-400'   },
  ]

  if (loading) {
    return (
      <div className="grid grid-cols-3 gap-3">
        {[1, 2, 3].map((i) => (
          <div key={i} className="glass-card rounded-2xl p-3">
            <Sk className="h-7 w-7 rounded-xl mx-auto mb-2" />
            <Sk className="h-5 w-16 mx-auto mb-1" />
            <Sk className="h-3 w-12 mx-auto" />
          </div>
        ))}
      </div>
    )
  }

  return (
    <div className="grid grid-cols-3 gap-3">
      {stats.map(({ label, value, Icon, delta, iconColor, valueColor }) => (
        <div
          key={label}
          className="glass-card rounded-2xl p-3 text-center hover:border-purple-500/30"
        >
          <div className="flex justify-center mb-1.5">
            <Icon size={18} className={iconColor} />
          </div>
          <p className={`text-sm font-black ${valueColor}`}>{value}</p>
          <p className="text-xs text-slate-500 font-medium mt-0.5">{label}</p>
          {delta !== null && (
            <div className={`flex items-center justify-center gap-0.5 mt-1 text-xs font-bold ${delta >= 0 ? 'text-green-400' : 'text-red-400'}`}>
              {delta >= 0 ? <ArrowUpRight size={11} /> : <ArrowDownRight size={11} />}
              {Math.abs(delta)}%
            </div>
          )}
        </div>
      ))}
    </div>
  )
}

// ─── AI Input Widget ──────────────────────────────────────────────────────────
function AIWidget() {
  const [message, setMessage] = useState('')
  return (
    <div className="glass-card rounded-3xl p-4 hover:border-purple-500/30">
      <div className="flex items-center gap-3 mb-3">
        <div className="w-8 h-8 rounded-xl bg-purple-500/20 flex items-center justify-center flex-shrink-0">
          <Zap size={15} className="text-purple-400" />
        </div>
        <p className="text-sm font-bold text-slate-300">Ask StuFi</p>
        <span className="ml-auto text-xs text-purple-400 font-semibold px-2 py-0.5 rounded-full bg-purple-500/15">AI</span>
      </div>
      <div className="flex items-center gap-3 bg-slate-700/50 rounded-2xl px-4 py-3">
        <input
          type="text"
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          placeholder="Ask StuFi about your budget..."
          className="flex-1 bg-transparent text-slate-100 placeholder-slate-500 text-sm outline-none"
        />
        <button
          className="w-8 h-8 rounded-xl bg-purple-500 flex items-center justify-center hover:bg-purple-400 flex-shrink-0 active:scale-90"
          aria-label="Send"
        >
          <Plus size={16} className="text-white" />
        </button>
      </div>
      <div className="flex items-center gap-2 mt-3">
        <button className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-700/50 text-slate-400 text-xs font-semibold hover:bg-slate-700">
          <Upload size={11} />
          Bank Statement
        </button>
        <button className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-700/50 text-slate-400 text-xs font-semibold hover:bg-slate-700">
          <Upload size={11} />
          Pachet acasa
        </button>
      </div>
    </div>
  )
}

// ─── Marketplace Widget ───────────────────────────────────────────────────────
const MOCK_MARKET = [
  { id: 1, title: 'Calculus Vol. 1', price: 'RON 35', condition: 'Good',     seller: 'Mihai D.' },
  { id: 2, title: 'Fizica Cuantica',  price: 'RON 28', condition: 'Like New', seller: 'Ana P.'   },
]

function MarketWidget({ loading }) {
  if (loading) {
    return (
      <div className="glass-card rounded-3xl p-5">
        <div className="flex items-center justify-between mb-4">
          <Sk className="h-4 w-28" />
          <Sk className="h-4 w-14" />
        </div>
        <div className="space-y-2.5">
          <Sk className="h-16 w-full rounded-2xl" />
          <Sk className="h-16 w-full rounded-2xl" />
        </div>
      </div>
    )
  }
  return (
    <div className="glass-card rounded-3xl p-5 hover:border-purple-500/30">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <BookOpen size={16} className="text-purple-400" />
          <p className="text-slate-100 font-bold text-sm">Marketplace</p>
        </div>
        <button className="flex items-center gap-1 text-xs text-purple-400 font-semibold hover:text-purple-300">
          See all <ChevronRight size={12} />
        </button>
      </div>
      <div className="space-y-2">
        {MOCK_MARKET.map((item) => (
          <div
            key={item.id}
            className="flex items-center justify-between p-3 rounded-2xl bg-slate-700/50 hover:bg-slate-700 cursor-pointer active:scale-[0.98]"
          >
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-purple-500/20 flex items-center justify-center flex-shrink-0">
                <BookOpen size={16} className="text-purple-400" />
              </div>
              <div>
                <p className="text-sm font-bold text-slate-100 leading-tight">{item.title}</p>
                <p className="text-xs text-slate-500 mt-0.5">{item.condition} · {item.seller}</p>
              </div>
            </div>
            <span className="text-sm font-black text-purple-400 flex-shrink-0 ml-3">{item.price}</span>
          </div>
        ))}
      </div>
    </div>
  )
}

// ─── Radar Widget ─────────────────────────────────────────────────────────────
const MOCK_RADAR = [
  { id: 1, name: 'Cantina Universitatii', discount: '-30%', type: 'Food',    distance: '50 m'  },
  { id: 2, name: 'Xerox & Print Hub',     discount: '-15%', type: 'Service', distance: '120 m' },
]

function RadarWidget({ loading }) {
  if (loading) {
    return (
      <div className="glass-card rounded-3xl p-5">
        <div className="flex items-center justify-between mb-4">
          <Sk className="h-4 w-28" />
          <Sk className="h-5 w-16 rounded-full" />
        </div>
        <div className="space-y-2.5">
          <Sk className="h-16 w-full rounded-2xl" />
          <Sk className="h-16 w-full rounded-2xl" />
        </div>
      </div>
    )
  }
  return (
    <div className="glass-card rounded-3xl p-5 hover:border-green-500/30">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <MapPin size={16} className="text-green-400" />
          <p className="text-slate-100 font-bold text-sm">Radar Deals</p>
        </div>
        <span className="text-xs font-bold px-2.5 py-1 rounded-full bg-green-500/20 text-green-400">
          Nearby
        </span>
      </div>
      <div className="space-y-2">
        {MOCK_RADAR.map((deal) => (
          <div
            key={deal.id}
            className="flex items-center justify-between p-3 rounded-2xl bg-slate-700/50 hover:bg-slate-700 cursor-pointer active:scale-[0.98]"
          >
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-green-500/20 flex items-center justify-center flex-shrink-0">
                <MapPin size={16} className="text-green-400" />
              </div>
              <div>
                <p className="text-sm font-bold text-slate-100 leading-tight">{deal.name}</p>
                <p className="text-xs text-slate-500 mt-0.5">{deal.type} · {deal.distance}</p>
              </div>
            </div>
            <span className="text-sm font-black text-green-400 flex-shrink-0 ml-3">{deal.discount}</span>
          </div>
        ))}
      </div>
    </div>
  )
}

// ─── Main Dashboard ───────────────────────────────────────────────────────────
export default function DashboardPage() {
  const { user } = useAuth()
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const t = setTimeout(() => setLoading(false), 1800)
    return () => clearTimeout(t)
  }, [])

  const firstName = user?.firstName ?? 'Student'
  const hour      = new Date().getHours()
  const greeting  =
    hour < 12 ? 'Good morning' : hour < 17 ? 'Good afternoon' : 'Good evening'

  return (
    <div className="flex h-screen bg-slate-900 overflow-hidden">
      <Sidebar />

      <main className="flex-1 md:ml-64 overflow-y-auto">
        <div className="max-w-2xl mx-auto px-4 pt-6 pb-28 md:pb-8 space-y-4">

          {/* Header */}
          <div className="flex items-center justify-between mb-2">
            <div>
              <p className="text-slate-500 text-sm font-medium">{greeting},</p>
              <h1 className="text-2xl font-black text-slate-100 tracking-tight">
                Hey, {firstName}!
              </h1>
            </div>
            <button
              className="relative w-11 h-11 rounded-2xl glass-card flex items-center justify-center hover:border-purple-500/40"
              aria-label="Notifications"
            >
              <Bell size={18} className="text-slate-400" />
              <span className="absolute top-2.5 right-2.5 w-2 h-2 rounded-full bg-purple-500 ring-2 ring-slate-900" />
            </button>
          </div>

          <HealthGauge percentage={85} loading={loading} />
          <QuickStats loading={loading} />
          <AIWidget />
          <MarketWidget loading={loading} />
          <RadarWidget  loading={loading} />

          {!loading && (
            <div className="glass-card rounded-3xl p-5">
              <p className="text-slate-100 font-bold text-sm mb-4">Recent Activity</p>
              <div className="space-y-3">
                {[
                  { label: 'Cantina lunch',        amount: '-RON 12',  time: 'Today, 13:24',     color: 'text-red-400',   dot: 'bg-red-500'    },
                  { label: 'Book sold - Calculus',  amount: '+RON 35',  time: 'Yesterday, 18:01', color: 'text-green-400', dot: 'bg-green-500'  },
                  { label: 'Parents transfer',      amount: '+RON 300', time: '3 days ago',       color: 'text-green-400', dot: 'bg-purple-500' },
                ].map((tx) => (
                  <div key={tx.label} className="flex items-center gap-3">
                    <div className={`w-2 h-2 rounded-full flex-shrink-0 ${tx.dot}`} />
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-semibold text-slate-100 truncate">{tx.label}</p>
                      <p className="text-xs text-slate-500">{tx.time}</p>
                    </div>
                    <span className={`text-sm font-black flex-shrink-0 ${tx.color}`}>{tx.amount}</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </main>

      <BottomNav />
      <InstallBanner />
    </div>
  )
}
