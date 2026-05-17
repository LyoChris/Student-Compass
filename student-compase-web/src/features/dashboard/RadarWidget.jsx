import { useState, useEffect } from 'react'
import { NavLink } from 'react-router-dom'
import { MapPin, ChevronRight, Zap, AlertCircle, ThumbsUp, Clock } from 'lucide-react'
import { radarApi } from '../../api/radarApi'
import DealFullDetailsModal from '../../pages/radar/DealFullDetailsModal'

// ── Helpers ────────────────────────────────────────────────────────────────────
const CATEGORY_META = {
  FOOD:   { color: '#F59E0B', label: 'Food'   },
  HOME:   { color: '#60A5FA', label: 'Home'   },
  SOCIAL: { color: '#F472B6', label: 'Social' },
  TECH:   { color: '#22D3EE', label: 'Tech'   },
  OTHER:  { color: '#A855F7', label: 'Other'  },
}

function timeLeft(expiresAt) {
  if (!expiresAt) return ''
  const diff = new Date(expiresAt) - Date.now()
  if (diff <= 0) return 'Expired'
  const h = Math.floor(diff / 3_600_000)
  const m = Math.floor((diff % 3_600_000) / 60_000)
  if (h >= 24) return `${Math.floor(h / 24)}d left`
  return h > 0 ? `${h}h left` : `${m}m left`
}

// ── Skeleton ───────────────────────────────────────────────────────────────────
function Sk({ className }) {
  return <div className={`skeleton ${className}`} />
}

function SkeletonWidget() {
  return (
    <div className="glass-card rounded-3xl p-5">
      <div className="flex items-center justify-between mb-4">
        <Sk className="h-4 w-28" />
        <Sk className="h-4 w-14" />
      </div>
      <div className="space-y-2.5">
        <Sk className="h-16 w-full rounded-2xl" />
        <Sk className="h-16 w-full rounded-2xl" />
        <Sk className="h-16 w-full rounded-2xl" />
      </div>
    </div>
  )
}

// ── Deal row ───────────────────────────────────────────────────────────────────
function DealRow({ deal, onClick }) {
  const meta    = CATEGORY_META[deal.category] ?? CATEGORY_META.OTHER
  const ttl     = timeLeft(deal.expiresAt)
  const expired = ttl === 'Expired'
  const score   = Number(deal.upvotes ?? 0) - Number(deal.downvotes ?? 0)

  return (
    <button
      onClick={onClick}
      className="flex items-center gap-3 p-3 rounded-2xl w-full text-left
                 bg-slate-800/50 hover:bg-slate-700/60 active:scale-[0.98]
                 border border-white/5 hover:border-white/15 transition-all"
    >
      {/* Category dot */}
      <div
        className="w-2.5 h-2.5 rounded-full flex-shrink-0"
        style={{ background: meta.color, boxShadow: `0 0 6px ${meta.color}80` }}
      />

      {/* Title + meta */}
      <div className="flex-1 min-w-0">
        <p className="text-sm font-bold text-slate-100 truncate leading-tight">{deal.title}</p>
        <div className="flex items-center gap-2 mt-0.5">
          <span className="text-[0.65rem] font-semibold" style={{ color: meta.color }}>
            {meta.label}
          </span>
          {ttl && (
            <span className={`flex items-center gap-0.5 text-[0.65rem] font-semibold ${
              expired ? 'text-rose-400' : 'text-emerald-400'
            }`}>
              <Clock size={9} />
              {ttl}
            </span>
          )}
        </div>
      </div>

      {/* Score */}
      <div className={`flex items-center gap-1 flex-shrink-0 ${
        score >= 0 ? 'text-emerald-400' : 'text-rose-400'
      }`}>
        <ThumbsUp size={11} />
        <span className="text-xs font-black">{score > 0 ? `+${score}` : score}</span>
      </div>
    </button>
  )
}

// ── Main widget ────────────────────────────────────────────────────────────────
export default function RadarWidget() {
  const [status,     setStatus]     = useState('loading')
  const [deals,      setDeals]      = useState([])
  const [activeDeal, setActiveDeal] = useState(null)

  useEffect(() => {
    radarApi.getDeals({ limit: 3 })
      .then(res => {
        const list = Array.isArray(res.data)
          ? res.data
          : res.data?.content ?? res.data?.deals ?? []
        setDeals(list.slice(0, 3))
        setStatus('success')
      })
      .catch(() => setStatus('error'))
  }, [])

  if (status === 'loading') return <SkeletonWidget />

  return (
    <>
      <div className="glass-card rounded-3xl p-5 hover:border-emerald-500/20 transition-colors">
        {/* Header */}
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <div className="w-7 h-7 rounded-xl bg-emerald-500/15 flex items-center justify-center">
              <MapPin size={14} className="text-emerald-400" />
            </div>
            <p className="text-slate-100 font-bold text-sm">Radar Deals</p>
          </div>
          <NavLink
            to="/radar"
            className="flex items-center gap-1 text-xs text-emerald-400 font-semibold hover:text-emerald-300 transition-colors"
          >
            See all <ChevronRight size={12} />
          </NavLink>
        </div>

        {/* Error */}
        {status === 'error' && (
          <div className="flex items-center gap-3 py-4 px-3 rounded-2xl bg-slate-800/50">
            <AlertCircle size={16} className="text-rose-400 flex-shrink-0" />
            <p className="text-sm text-slate-500">Couldn't load deals right now.</p>
          </div>
        )}

        {/* Empty state */}
        {status === 'success' && deals.length === 0 && (
          <div className="flex flex-col items-center py-7 text-center gap-2">
            <div className="w-12 h-12 rounded-2xl bg-emerald-500/10 flex items-center justify-center mb-1">
              <Zap size={22} className="text-emerald-400/50" />
            </div>
            <p className="text-slate-400 text-sm font-semibold">No deals nearby yet</p>
            <p className="text-slate-600 text-xs max-w-[18rem]">
              Be the first to drop a student deal on the Radar map!
            </p>
            <NavLink
              to="/radar"
              className="mt-2 flex items-center gap-1.5 rounded-2xl border border-emerald-500/30 bg-emerald-500/10
                         px-4 py-2 text-xs font-black text-emerald-300 hover:bg-emerald-500/20 transition-all"
            >
              <MapPin size={12} />
              Open Radar
            </NavLink>
          </div>
        )}

        {/* Deal list */}
        {status === 'success' && deals.length > 0 && (
          <div className="space-y-2">
            {deals.map((deal) => (
              <DealRow
                key={deal.id}
                deal={deal}
                onClick={() => setActiveDeal(deal)}
              />
            ))}
          </div>
        )}
      </div>

      {/* Deal detail modal — opens inline from dashboard */}
      {activeDeal && (
        <DealFullDetailsModal
          deal={activeDeal}
          onClose={() => setActiveDeal(null)}
        />
      )}
    </>
  )
}
