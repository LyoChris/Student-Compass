import { useState, useEffect } from 'react'
import {
  BadgeCheck, Sparkles, ServerCrash, PackageX, RefreshCw,
} from 'lucide-react'
import { recommendationsApi } from '../../api/recommendationsApi'

// ── Helpers ────────────────────────────────────────────────────────────────────
function fmtPrice(n) {
  return Number(n ?? 0).toFixed(2)
}

// ── Skeleton card ──────────────────────────────────────────────────────────────
function SkeletonCard() {
  return (
    <div className="bg-slate-800/40 backdrop-blur-lg border border-slate-700 rounded-2xl p-5 space-y-4 animate-pulse">
      <div className="flex items-center justify-between">
        <div className="h-3 w-28 rounded-full bg-slate-700/70" />
        <div className="h-5 w-16 rounded-full bg-slate-700/70" />
      </div>
      <div className="space-y-2 pt-1">
        <div className="h-4 w-full rounded-full bg-slate-700/70" />
        <div className="h-4 w-3/4 rounded-full bg-slate-700/70" />
      </div>
      <div className="h-8 w-24 rounded-full bg-slate-700/70" />
      <div className="h-16 w-full rounded-xl bg-slate-700/40" />
    </div>
  )
}

// ── Error state ────────────────────────────────────────────────────────────────
function ErrorState({ onRetry }) {
  return (
    <div className="col-span-full flex flex-col items-center justify-center gap-5 py-16 text-center">
      <div
        className="w-20 h-20 rounded-3xl flex items-center justify-center"
        style={{
          background: 'rgba(239,68,68,0.10)',
          border: '1px solid rgba(239,68,68,0.25)',
          boxShadow: '0 0 32px rgba(239,68,68,0.10)',
        }}
      >
        <ServerCrash size={34} className="text-red-400" />
      </div>
      <div>
        <p className="text-slate-100 font-black text-lg">Could not load recommendations</p>
        <p className="text-slate-500 text-sm mt-1 max-w-xs leading-relaxed">
          The AI coach is taking a moment. Check your connection or try again.
        </p>
      </div>
      <button
        onClick={onRetry}
        className="flex items-center gap-2 rounded-2xl border border-red-500/30 bg-red-500/10
                   px-5 py-2.5 text-sm font-black text-red-400
                   hover:bg-red-500/20 hover:border-red-400/50 transition-all"
      >
        <RefreshCw size={14} />
        Try Again
      </button>
    </div>
  )
}

// ── Empty state ────────────────────────────────────────────────────────────────
function EmptyState() {
  return (
    <div className="col-span-full flex flex-col items-center justify-center gap-5 py-16 text-center">
      <div
        className="w-20 h-20 rounded-3xl flex items-center justify-center"
        style={{
          background: 'rgba(168,85,247,0.08)',
          border: '1px solid rgba(168,85,247,0.20)',
          boxShadow: '0 0 32px rgba(168,85,247,0.08)',
        }}
      >
        <PackageX size={34} className="text-purple-400" />
      </div>
      <div>
        <p className="text-slate-100 font-black text-lg">Nothing here yet</p>
        <p className="text-slate-500 text-sm mt-1 max-w-xs leading-relaxed">
          No recommendations found for this budget yet.
        </p>
      </div>
    </div>
  )
}

// ── Partner badge ──────────────────────────────────────────────────────────────
function PartnerBadge() {
  return (
    <span
      className="flex items-center gap-1 flex-shrink-0 rounded-full border border-purple-400/30
                 bg-purple-500/15 px-2 py-0.5 text-[0.60rem] font-black text-purple-300"
      style={{ boxShadow: '0 0 10px rgba(168,85,247,0.20)' }}
    >
      <BadgeCheck size={9} />
      Partner
    </span>
  )
}

// ── Product card ───────────────────────────────────────────────────────────────
function ProductCard({ item }) {
  return (
    <article
      className="bg-slate-800/40 backdrop-blur-lg border border-slate-700 rounded-2xl
                 flex flex-col hover:border-purple-500/50 hover:bg-slate-800/60
                 transition-all duration-300"
    >
      {/* Header — store + partner badge */}
      <div className="flex items-center justify-between gap-2 px-5 pt-5 pb-3 border-b border-white/5">
        <span className="text-slate-400 text-xs font-semibold uppercase tracking-wider truncate">
          {item.storeName}
        </span>
        {item.isPartner && <PartnerBadge />}
      </div>

      {/* Body */}
      <div className="px-5 pt-4 pb-3 flex-1 flex flex-col">
        {/* Category micro-label */}
        <p className="text-[0.60rem] font-bold text-slate-600 uppercase tracking-widest mb-1">
          {item.category}
        </p>

        {/* Product name */}
        <h3
          className="text-slate-100 font-medium text-lg leading-tight mt-2 line-clamp-2"
          title={item.name}
        >
          {item.name}
        </h3>

        {/* Price */}
        <p
          className="text-purple-400 font-bold text-2xl mt-3 tabular-nums"
          style={{ textShadow: '0 0 18px rgba(168,85,247,0.35)' }}
        >
          {fmtPrice(item.price)}
          <span className="text-sm font-semibold text-slate-400 ml-1.5">RON</span>
        </p>

        {/* AI reason box */}
        <div className="bg-slate-900/50 rounded-xl p-3 mt-4 flex items-start gap-2 flex-1">
          <Sparkles size={13} className="text-amber-400 flex-shrink-0 mt-0.5" />
          <p className="text-slate-300 text-sm italic leading-relaxed">{item.reason}</p>
        </div>
      </div>
    </article>
  )
}

// ── Main component ─────────────────────────────────────────────────────────────
export default function RecommendationList({ category }) {
  const [isLoading,       setIsLoading]       = useState(true)
  const [isError,         setIsError]         = useState(false)
  const [recommendations, setRecommendations] = useState([])

  function fetchData() {
    setIsLoading(true)
    setIsError(false)
    const params = {}
    if (category) params.category = category
    recommendationsApi
      .getRecommendations(params)
      .then(res => {
        setRecommendations(res.data?.recommendations ?? [])
      })
      .catch(() => setIsError(true))
      .finally(() => setIsLoading(false))
  }

  useEffect(() => {
    fetchData()
  }, [category]) // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">

      {/* Loading */}
      {isLoading && (
        <>
          <SkeletonCard />
          <SkeletonCard />
          <SkeletonCard />
        </>
      )}

      {/* Error */}
      {!isLoading && isError && (
        <ErrorState onRetry={fetchData} />
      )}

      {/* Empty */}
      {!isLoading && !isError && recommendations.length === 0 && (
        <EmptyState />
      )}

      {/* Cards */}
      {!isLoading && !isError && recommendations.map(item => (
        <ProductCard key={item.productId} item={item} />
      ))}

    </div>
  )
}
