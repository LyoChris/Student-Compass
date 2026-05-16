import { useState } from 'react'
import { X, ThumbsUp, ThumbsDown, ChevronRight, Loader2, ShieldCheck } from 'lucide-react'
import { radarApi } from '../../api/radarApi'

const CATEGORY_COLORS = {
  FOOD:   '#F59E0B',
  HOME:   '#60A5FA',
  SOCIAL: '#F472B6',
  TECH:   '#22D3EE',
  OTHER:  '#A855F7',
}

function timeLeft(expiresAt) {
  if (!expiresAt) return ''
  const diff = new Date(expiresAt) - Date.now()
  if (diff <= 0) return 'Expired'
  const h = Math.floor(diff / 3600000)
  const m = Math.floor((diff % 3600000) / 60000)
  if (h >= 24) return `${Math.floor(h / 24)}d ${h % 24}h left`
  return h > 0 ? `${h}h ${m}m left` : `${m}m left`
}

export default function DealBottomSheet({ deal, onClose, onOpenDetails, onVoted }) {
  const [voting, setVoting]   = useState(null) // 'UPVOTE' | 'DOWNVOTE' | null
  const [voteError, setVoteError] = useState(null)
  const [voted, setVoted]     = useState(null)

  if (!deal) return null

  const dotColor = CATEGORY_COLORS[deal.category] || CATEGORY_COLORS.OTHER
  const ttl      = timeLeft(deal.expiresAt)
  const isExpired = ttl === 'Expired'

  async function handleVote(type) {
    if (voted || voting) return
    setVoting(type)
    setVoteError(null)
    try {
      await radarApi.vote(deal.id, type)
      setVoted(type)
      onVoted?.()
    } catch (err) {
      if (err.response?.status === 409) {
        setVoteError('You already voted on this deal.')
      } else {
        setVoteError('Vote failed. Try again.')
      }
    } finally {
      setVoting(null)
    }
  }

  const upvotes   = Number(deal.upvotes   ?? 0) + (voted === 'UPVOTE'   ? 1 : 0)
  const downvotes = Number(deal.downvotes ?? 0) + (voted === 'DOWNVOTE' ? 1 : 0)
  const score     = upvotes - downvotes

  return (
    <>
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-black/40 backdrop-blur-sm"
        style={{ zIndex: 900 }}
        onClick={onClose}
      />

      {/* Sheet */}
      <div className="fixed bottom-0 left-0 right-0 max-w-2xl mx-auto" style={{ zIndex: 901 }}>
        <div className="rounded-t-[2rem] bg-slate-900/95 backdrop-blur-xl border-t border-x border-white/10 shadow-2xl px-5 pt-4 pb-8">
          {/* Drag handle */}
          <div className="w-10 h-1 rounded-full bg-slate-700 mx-auto mb-5" />

          {/* Close */}
          <button
            onClick={onClose}
            className="absolute top-4 right-5 w-8 h-8 rounded-xl bg-slate-800/60 flex items-center justify-center text-slate-400 hover:text-slate-100"
          >
            <X size={16} />
          </button>

          {/* Category + time */}
          <div className="flex items-center gap-2 mb-3">
            <div
              className="w-3 h-3 rounded-full flex-shrink-0"
              style={{ background: dotColor, boxShadow: `0 0 8px ${dotColor}` }}
            />
            <span className="text-xs font-black text-slate-400 uppercase tracking-wider">
              {deal.category}
            </span>
            <span className="ml-auto text-xs font-bold" style={{ color: isExpired ? '#f87171' : '#86efac' }}>
              {ttl}
            </span>
          </div>

          {/* Title */}
          <h2 className="text-xl font-black text-slate-100 mb-1">{deal.title}</h2>

          {/* Trust score row */}
          {deal.reporterTrustScore !== undefined && (
            <div className="flex items-center gap-1.5 mb-4">
              <ShieldCheck size={13} className="text-purple-400" />
              <span className="text-xs text-slate-400">
                Reporter trust: <span className="text-purple-300 font-bold">{deal.reporterTrustScore}</span>
              </span>
              <span className="text-slate-700 text-xs mx-1">·</span>
              <span className="text-xs text-slate-500">
                Score: <span className={`font-bold ${score >= 0 ? 'text-emerald-400' : 'text-rose-400'}`}>{score >= 0 ? '+' : ''}{score}</span>
              </span>
            </div>
          )}

          {/* Votes */}
          <div className="flex gap-3 mb-4">
            <button
              onClick={() => handleVote('UPVOTE')}
              disabled={!!voted || !!voting || isExpired}
              className={`flex-1 flex items-center justify-center gap-2 py-3.5 rounded-2xl font-black text-sm transition-all
                ${voted === 'UPVOTE'
                  ? 'bg-emerald-500/30 border border-emerald-400/40 text-emerald-300'
                  : 'bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 hover:bg-emerald-500/20'}
                disabled:opacity-40`}
            >
              {voting === 'UPVOTE' ? <Loader2 size={16} className="animate-spin" /> : <ThumbsUp size={16} />}
              {upvotes} Upvote{upvotes !== 1 ? 's' : ''}
            </button>
            <button
              onClick={() => handleVote('DOWNVOTE')}
              disabled={!!voted || !!voting || isExpired}
              className={`flex-1 flex items-center justify-center gap-2 py-3.5 rounded-2xl font-black text-sm transition-all
                ${voted === 'DOWNVOTE'
                  ? 'bg-rose-500/30 border border-rose-400/40 text-rose-300'
                  : 'bg-rose-500/10 border border-rose-500/20 text-rose-400 hover:bg-rose-500/20'}
                disabled:opacity-40`}
            >
              {voting === 'DOWNVOTE' ? <Loader2 size={16} className="animate-spin" /> : <ThumbsDown size={16} />}
              {downvotes} Downvote{downvotes !== 1 ? 's' : ''}
            </button>
          </div>

          {voteError && (
            <p className="text-xs text-rose-400 text-center mb-3">{voteError}</p>
          )}

          {/* See full details */}
          <button
            onClick={() => { onOpenDetails(deal); onClose() }}
            className="w-full flex items-center justify-center gap-2 rounded-2xl bg-purple-500/15 border border-purple-500/30 py-3 text-sm font-black text-purple-300 hover:bg-purple-500/25"
          >
            See Full Details <ChevronRight size={15} />
          </button>
        </div>
      </div>
    </>
  )
}
