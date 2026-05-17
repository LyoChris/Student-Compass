import { useState, useEffect } from 'react'
import { useSearchParams, useNavigate } from 'react-router-dom'
import {
  ArrowLeft, Sparkles, BadgeCheck, Brain, Target,
  Coffee, ExternalLink, RefreshCw, Send, X, ChevronDown,
} from 'lucide-react'
import AppShell from '../../components/layout/AppShell'
import { recommendationsApi } from '../../api/recommendationsApi'

// ── Categories ─────────────────────────────────────────────────────────────────
const CATEGORIES = [
  { value: '',       label: 'All Categories',  emoji: '✨' },
  { value: 'FOOD',   label: 'Food',             emoji: '🍔' },
  { value: 'HOME',   label: 'Home',             emoji: '🏠' },
  { value: 'SOCIAL', label: 'Social',           emoji: '🎉' },
  { value: 'TECH',   label: 'Tech',             emoji: '💻' },
  { value: 'OTHER',  label: 'Other',            emoji: '🎁' },
]

// ── Helpers ─────────────────────────────────────────────────────────────────────
function fmtPrice(n) {
  return Number(n ?? 0).toFixed(2)
}

function toLabel(cat) {
  if (!cat) return ''
  return cat.charAt(0).toUpperCase() + cat.slice(1).toLowerCase()
}

// ── Skeleton card ──────────────────────────────────────────────────────────────
function Sk({ className }) {
  return <div className={`skeleton ${className}`} />
}

function SkeletonCard() {
  return (
    <div className="glass-card rounded-3xl p-5 space-y-4">
      <div className="flex items-center justify-between">
        <Sk className="h-3 w-24 rounded-full" />
        <Sk className="h-5 w-20 rounded-full" />
      </div>
      <Sk className="h-5 w-3/4 rounded-full" />
      <Sk className="h-8 w-28 rounded-full" />
      <Sk className="h-16 w-full rounded-2xl" />
      <Sk className="h-10 w-full rounded-2xl" />
    </div>
  )
}

// ── Error state ────────────────────────────────────────────────────────────────
function ErrorCard({ category, onRetry }) {
  return (
    <div className="glass-card rounded-3xl p-8 flex flex-col items-center text-center gap-4">
      <div className="w-16 h-16 rounded-2xl bg-amber-500/15 border border-amber-500/20 flex items-center justify-center">
        <Coffee size={28} className="text-amber-400" />
      </div>
      <div>
        <h3 className="text-base font-black text-slate-100 mb-1">
          AI Coach is on it!
        </h3>
        <p className="text-sm text-slate-400 leading-relaxed max-w-xs">
          The AI Finance Coach is currently calculating new deals
          {category ? ` for ${toLabel(category)}` : ''}. Please check back
          in a few minutes!
        </p>
      </div>
      <button
        onClick={onRetry}
        className="flex items-center gap-2 rounded-2xl border border-amber-500/30 bg-amber-500/10
                   px-5 py-2.5 text-sm font-black text-amber-300 hover:bg-amber-500/20 transition-all"
      >
        <RefreshCw size={14} />
        Try Again
      </button>
    </div>
  )
}

// ── Source badge ───────────────────────────────────────────────────────────────
function SourceBadge({ source }) {
  if (source === 'gemini_fallback') return (
    <>
      <style>{`
        @keyframes gemini-shimmer {
          0%   { background-position: 0%   50%; }
          50%  { background-position: 100% 50%; }
          100% { background-position: 0%   50%; }
        }
        .gemini-badge {
          background: linear-gradient(
            90deg,
            rgba(59,130,246,0.22),
            rgba(168,85,247,0.28),
            rgba(99,102,241,0.22)
          );
          background-size: 200% 200%;
          animation: gemini-shimmer 3s ease infinite;
          border: 1px solid rgba(99,102,241,0.45);
          color: #a5b4fc;
          box-shadow: 0 0 14px rgba(99,102,241,0.2);
        }
      `}</style>
      <span className="gemini-badge flex items-center gap-1.5 rounded-full px-3 py-1 text-[0.68rem] font-black">
        ✨ Recommendations by Gemini
      </span>
    </>
  )

  if (source === 'llm') return (
    <span
      className="flex items-center gap-1.5 rounded-full border border-purple-400/30 bg-purple-500/15
                 px-3 py-1 text-[0.68rem] font-black text-purple-300"
      style={{ boxShadow: '0 0 12px rgba(168,85,247,0.25)' }}
    >
      <Brain size={11} /> Smart AI Picks
    </span>
  )

  return (
    <span className="flex items-center gap-1.5 rounded-full border border-slate-600/40 bg-slate-700/30
                     px-3 py-1 text-[0.68rem] font-black text-slate-400">
      <Target size={11} /> AI Picks
    </span>
  )
}

// ── Product card ───────────────────────────────────────────────────────────────
function RecommendationCard({ item }) {
  return (
    <article className="glass-card rounded-3xl overflow-hidden flex flex-col transition hover:-translate-y-0.5">

      {/* Store row */}
      <div className="flex items-center justify-between px-5 pt-5 pb-3 border-b border-white/5">
        <span className="text-xs font-black text-slate-400 uppercase tracking-widest truncate">
          {item.storeName}
        </span>
        {item.isPartner && (
          <span
            className="flex items-center gap-1 flex-shrink-0 ml-2 rounded-full border border-purple-400/30
                       bg-purple-500/15 px-2.5 py-1 text-[0.62rem] font-black text-purple-300"
            style={{ boxShadow: '0 0 10px rgba(168,85,247,0.2)' }}
          >
            <BadgeCheck size={10} /> Partner Verified
          </span>
        )}
      </div>

      {/* Main content */}
      <div className="px-5 pt-4 pb-2 flex-1 space-y-3">
        <div>
          <p className="text-[0.65rem] font-bold text-slate-500 uppercase tracking-wider mb-1">
            {item.category}
          </p>
          <h3 className="text-base font-black text-slate-100 leading-snug">
            {item.name}
          </h3>
        </div>

        <p
          className="text-3xl font-black tabular-nums leading-none"
          style={{ color: '#A855F7', textShadow: '0 0 20px rgba(168,85,247,0.4)' }}
        >
          {fmtPrice(item.price)}
          <span className="text-sm font-bold text-slate-400 ml-1.5">RON</span>
        </p>

        {/* AI reason */}
        <div className="rounded-2xl bg-slate-900/60 border border-white/5 px-4 py-3 flex gap-2.5">
          <Sparkles size={13} className="text-amber-400 flex-shrink-0 mt-0.5" />
          <p className="text-xs italic text-slate-300 leading-relaxed">{item.reason}</p>
        </div>
      </div>

      {/* Action */}
      <div className="px-5 pb-5 pt-3">
        <button
          className="w-full flex items-center justify-center gap-2 rounded-2xl border border-white/10
                     bg-white/5 px-4 py-2.5 text-sm font-black text-slate-200
                     hover:border-purple-500/40 hover:bg-purple-500/10 hover:text-purple-200 transition-all"
        >
          <ExternalLink size={14} />
          View Deal
        </button>
      </div>
    </article>
  )
}

// ── Page ───────────────────────────────────────────────────────────────────────
export default function AiCategoryRecommendations() {
  const [searchParams] = useSearchParams()
  const navigate       = useNavigate()

  // Category field — pre-filled from ?category= URL param, freely editable
  const urlCategory = searchParams.get('category') ?? ''
  const [categoryInput, setCategoryInput] = useState(urlCategory)
  const [userNote,      setUserNote]      = useState('')

  // Applied values (what was last sent to API)
  const [appliedCategory, setAppliedCategory] = useState(urlCategory)
  const [appliedNote,     setAppliedNote]     = useState('')

  const [status, setStatus] = useState('loading')
  const [data,   setData]   = useState(null)

  function fetchData(cat, note) {
    setStatus('loading')
    setData(null)
    const params = {}
    if (cat)  params.category = cat.trim().toUpperCase()
    if (note) params.userNote = note.trim()
    recommendationsApi.getRecommendations(params)
      .then(res => {
        setData(res.data)
        setStatus('success')
      })
      .catch(() => setStatus('error'))
  }

  // Initial load uses URL param
  useEffect(() => {
    fetchData(urlCategory, '')
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  function handleSubmit(e) {
    e.preventDefault()
    const cat  = categoryInput.trim()
    const note = userNote.trim()
    setAppliedCategory(cat)
    setAppliedNote(note)
    fetchData(cat, note)
  }

  function handleClearNote() {
    setUserNote('')
  }

  const recs = data?.recommendations ?? []

  return (
    <AppShell>
      <div className="max-w-2xl mx-auto px-4 pt-6 pb-28 md:pb-8 space-y-5">

        {/* ── Back button ── */}
        <button
          onClick={() => navigate('/budget')}
          className="flex items-center gap-2 text-sm font-bold text-slate-400 hover:text-slate-100 transition-colors"
        >
          <ArrowLeft size={16} />
          Back to Budget
        </button>

        {/* ── Hero header ── */}
        <div className="space-y-3">
          <div className="flex items-start justify-between gap-3">
            <h1
              className="text-3xl font-black text-slate-100 leading-tight tracking-tight"
              style={{ textShadow: '0 0 32px rgba(168,85,247,0.35)' }}
            >
              <span className="text-amber-400">✨</span>{' '}
              Smart Picks
              {appliedCategory && (
                <span style={{ color: '#A855F7' }}> for {toLabel(appliedCategory)}</span>
              )}
            </h1>
            {status === 'success' && data?.source && (
              <div className="flex-shrink-0 mt-1">
                <SourceBadge source={data.source} />
              </div>
            )}
          </div>

          {/* Context micro-copy */}
          <div className="flex items-center gap-2 rounded-2xl bg-slate-800/50 border border-white/5 px-4 py-2.5">
            <Brain size={13} className="text-purple-400 flex-shrink-0" />
            <p className="text-xs text-slate-400 leading-relaxed">
              Based on your remaining budget
              {appliedCategory ? ` for ${toLabel(appliedCategory)}` : ''} and your profile habits.
            </p>
          </div>

          {/* ── Two-field form ── */}
          <form onSubmit={handleSubmit} className="space-y-2">

            {/* Field 1 — Category dropdown */}
            <div
              className="relative"
              style={{ boxShadow: '0 0 16px rgba(168,85,247,0.08)' }}
            >
              <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-base pointer-events-none leading-none">
                {CATEGORIES.find(c => c.value === categoryInput)?.emoji ?? '✨'}
              </span>
              <select
                value={categoryInput}
                onChange={e => setCategoryInput(e.target.value)}
                className="w-full appearance-none rounded-2xl border border-white/10 bg-slate-800/70 backdrop-blur
                           pl-9 pr-9 py-2.5 text-sm text-slate-200
                           focus:outline-none focus:border-purple-500/50 focus:bg-slate-800/90
                           transition-all cursor-pointer"
              >
                {CATEGORIES.map(c => (
                  <option key={c.value} value={c.value} className="bg-slate-900 text-slate-200">
                    {c.emoji} {c.label}
                  </option>
                ))}
              </select>
              <ChevronDown
                size={13}
                className="absolute right-3.5 top-1/2 -translate-y-1/2 text-purple-400 pointer-events-none"
              />
            </div>

            {/* Field 2 — User note / specific request */}
            <div className="flex items-center gap-2">
              <div
                className="relative flex-1"
                style={{ boxShadow: '0 0 16px rgba(168,85,247,0.08)' }}
              >
                <Sparkles
                  size={13}
                  className="absolute left-3.5 top-1/2 -translate-y-1/2 text-purple-400 pointer-events-none"
                />
                <input
                  type="text"
                  value={userNote}
                  onChange={e => setUserNote(e.target.value)}
                  placeholder="Any specific requests? (e.g. 'Vegan only', 'Under 50 RON')"
                  className="w-full rounded-2xl border border-white/10 bg-slate-800/70 backdrop-blur
                             pl-9 pr-9 py-2.5 text-sm text-slate-200 placeholder-slate-500
                             focus:outline-none focus:border-purple-500/50 focus:bg-slate-800/90
                             transition-all"
                />
                {userNote && (
                  <button
                    type="button"
                    onClick={handleClearNote}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 hover:text-slate-300 transition-colors"
                  >
                    <X size={13} />
                  </button>
                )}
              </div>
              <button
                type="submit"
                disabled={status === 'loading'}
                className="flex items-center gap-1.5 rounded-2xl border border-purple-500/40 bg-purple-500/15
                           px-4 py-2.5 text-sm font-black text-purple-300
                           hover:bg-purple-500/25 hover:border-purple-400/60 hover:text-purple-200
                           disabled:opacity-50 disabled:cursor-not-allowed transition-all"
                style={{ boxShadow: '0 0 12px rgba(168,85,247,0.2)' }}
              >
                <Send size={13} />
                Ask AI
              </button>
            </div>
          </form>

          {/* Applied note chip */}
          {appliedNote && status !== 'loading' && (
            <div className="flex items-center gap-2">
              <span className="text-[0.65rem] text-slate-500">Filtered by:</span>
              <span
                className="flex items-center gap-1.5 rounded-full border border-purple-500/30 bg-purple-500/10
                           px-2.5 py-0.5 text-[0.65rem] font-bold text-purple-300 max-w-xs truncate"
              >
                <Sparkles size={9} />
                {appliedNote}
              </span>
            </div>
          )}
        </div>

        {/* ── Loading ── */}
        {status === 'loading' && (
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <SkeletonCard />
            <SkeletonCard />
            <SkeletonCard />
          </div>
        )}

        {/* ── Error ── */}
        {status === 'error' && (
          <ErrorCard category={appliedCategory} onRetry={() => fetchData(appliedCategory, appliedNote)} />
        )}

        {/* ── Success ── */}
        {status === 'success' && (
          <>
            {recs.length === 0 ? (
              <ErrorCard category={appliedCategory} onRetry={() => fetchData(appliedCategory, appliedNote)} />
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                {recs.map((item, i) => (
                  <RecommendationCard key={item.productId ?? i} item={item} />
                ))}
              </div>
            )}
          </>
        )}
      </div>
    </AppShell>
  )
}
