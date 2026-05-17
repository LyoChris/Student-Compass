import { useState, useEffect } from 'react'
import { Sparkles, BadgeCheck, Brain, Target, Coffee, ExternalLink } from 'lucide-react'
import { recommendationsApi } from '../../api/recommendationsApi'

// ── Helpers ────────────────────────────────────────────────────────────────────
function fmtPrice(n) {
  return Number(n ?? 0).toFixed(2)
}

// ── Skeleton card ──────────────────────────────────────────────────────────────
function SkeletonCard() {
  return (
    <div className="min-w-[272px] max-w-[288px] flex-shrink-0 rounded-3xl border border-white/8 bg-slate-800/50 backdrop-blur p-5 space-y-4 snap-center">
      <div className="flex items-center justify-between">
        <div className="skeleton h-3 w-24 rounded-full" />
        <div className="skeleton h-5 w-20 rounded-full" />
      </div>
      <div className="space-y-2">
        <div className="skeleton h-4 w-full rounded-full" />
        <div className="skeleton h-4 w-3/4 rounded-full" />
      </div>
      <div className="skeleton h-7 w-28 rounded-full" />
      <div className="skeleton h-16 w-full rounded-2xl" />
      <div className="skeleton h-10 w-full rounded-2xl" />
    </div>
  )
}

// ── Error banner ───────────────────────────────────────────────────────────────
function ErrorBanner() {
  return (
    <div className="glass-card rounded-3xl px-5 py-6 flex items-center gap-4">
      <div className="w-12 h-12 rounded-2xl bg-amber-500/15 border border-amber-500/20 flex items-center justify-center flex-shrink-0">
        <Coffee size={22} className="text-amber-400" />
      </div>
      <div>
        <p className="text-sm font-bold text-slate-200 leading-snug">
          Our AI Coach is currently analyzing the market.
        </p>
        <p className="text-xs text-slate-500 mt-0.5">
          Check back later for personalized deals!
        </p>
      </div>
    </div>
  )
}

// ── Single recommendation card ─────────────────────────────────────────────────
function RecommendationCard({ item }) {
  return (
    <article className="min-w-[272px] max-w-[288px] flex-shrink-0 snap-center rounded-3xl border border-white/8 bg-slate-800/50 backdrop-blur-xl flex flex-col overflow-hidden transition hover:-translate-y-0.5">

      {/* Top: store + partner badge */}
      <div className="flex items-center justify-between px-5 pt-5 pb-3 border-b border-white/5">
        <span className="text-xs font-black text-slate-400 uppercase tracking-widest truncate">
          {item.storeName}
        </span>
        {item.isPartner && (
          <span className="flex items-center gap-1 flex-shrink-0 ml-2 rounded-full border border-purple-400/30 bg-purple-500/15 px-2.5 py-1 text-[0.62rem] font-black text-purple-300"
            style={{ boxShadow: '0 0 10px rgba(168,85,247,0.2)' }}
          >
            <BadgeCheck size={10} />
            Partner
          </span>
        )}
      </div>

      {/* Middle: name + price + category */}
      <div className="px-5 pt-4 pb-2 flex-1">
        <p className="text-[0.65rem] font-bold text-slate-500 uppercase tracking-wider mb-1">
          {item.category}
        </p>
        <h3 className="text-base font-black text-slate-100 leading-snug mb-3">
          {item.name}
        </h3>
        <p
          className="text-3xl font-black tabular-nums leading-none"
          style={{ color: '#A855F7', textShadow: '0 0 24px rgba(168,85,247,0.45)' }}
        >
          {fmtPrice(item.price)}
          <span className="text-sm font-bold text-slate-400 ml-1.5">RON</span>
        </p>

        {/* AI reason */}
        <div className="mt-4 rounded-2xl bg-slate-900/60 border border-white/5 px-3.5 py-3 flex gap-2">
          <Sparkles size={13} className="text-amber-400 flex-shrink-0 mt-0.5" />
          <p className="text-xs italic text-slate-300 leading-relaxed">
            {item.reason}
          </p>
        </div>
      </div>

      {/* Action */}
      <div className="px-5 pb-5 pt-3">
        <button className="w-full flex items-center justify-center gap-2 rounded-2xl border border-white/10 bg-white/5 px-4 py-2.5 text-sm font-black text-slate-200 hover:border-purple-500/40 hover:bg-purple-500/10 hover:text-purple-200 transition-all">
          <ExternalLink size={14} />
          View Deal
        </button>
      </div>
    </article>
  )
}

// ── Source badge ───────────────────────────────────────────────────────────────
function SourceBadge({ source }) {
  if (source === 'llm') return (
    <span
      className="flex items-center gap-1.5 rounded-full border border-purple-400/30 bg-purple-500/15 px-3 py-1 text-[0.68rem] font-black text-purple-300"
      style={{ boxShadow: '0 0 14px rgba(168,85,247,0.25)' }}
    >
      <Brain size={11} />
      Powered by AI
    </span>
  )
  return (
    <span className="flex items-center gap-1.5 rounded-full border border-slate-600/40 bg-slate-700/30 px-3 py-1 text-[0.68rem] font-black text-slate-400">
      <Target size={11} />
      Curated for you
    </span>
  )
}

// ── Main widget ────────────────────────────────────────────────────────────────
export default function AiRecommendationsWidget() {
  const [status, setStatus]   = useState('loading') // 'loading' | 'error' | 'success'
  const [data,   setData]     = useState(null)

  useEffect(() => {
    recommendationsApi.getRecommendations()
      .then(res => {
        setData(res.data)
        setStatus('success')
      })
      .catch(err => {
        const code = err.response?.status
        // 503/504 = AI service down; treat any error as graceful degradation
        if (!code || code === 503 || code === 504 || code >= 500) {
          setStatus('error')
        } else {
          setStatus('error')
        }
      })
  }, [])

  return (
    <section className="w-full">
      {/* Header */}
      <div className="flex items-center justify-between mb-3 px-0.5">
        <div className="flex items-center gap-2">
          <Sparkles size={15} className="text-amber-400" />
          <h2 className="text-sm font-black text-slate-100 tracking-tight">
            Smart Picks for You
          </h2>
        </div>
        {status === 'success' && data?.source && (
          <SourceBadge source={data.source} />
        )}
      </div>

      {/* Loading */}
      {status === 'loading' && (
        <div className="flex gap-4 overflow-x-auto pb-2 snap-x" style={{ scrollbarWidth: 'none' }}>
          <SkeletonCard />
          <SkeletonCard />
          <SkeletonCard />
        </div>
      )}

      {/* Error */}
      {status === 'error' && <ErrorBanner />}

      {/* Success */}
      {status === 'success' && (
        <>
          {(!data?.recommendations || data.recommendations.length === 0) ? (
            <ErrorBanner />
          ) : (
            <div
              className="flex gap-4 overflow-x-auto pb-2 snap-x"
              style={{ scrollbarWidth: 'none', WebkitOverflowScrolling: 'touch' }}
            >
              {data.recommendations.map((item, i) => (
                <RecommendationCard key={item.productId ?? i} item={item} />
              ))}
            </div>
          )}
        </>
      )}
    </section>
  )
}
