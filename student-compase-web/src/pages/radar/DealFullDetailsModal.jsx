import { useState, useEffect, useRef } from 'react'
import { X, Navigation, Send, Loader2, MessageCircle, ShieldCheck, ThumbsUp, ThumbsDown } from 'lucide-react'
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

function fmt(iso) {
  if (!iso) return ''
  return new Date(iso).toLocaleTimeString('ro-RO', { hour: '2-digit', minute: '2-digit' })
}

export default function DealFullDetailsModal({ deal: initialDeal, onClose }) {
  const [deal, setDeal]           = useState(initialDeal)
  const [loading, setLoading]     = useState(true)
  const [comment, setComment]     = useState('')
  const [sending, setSending]     = useState(false)
  const [sendError, setSendError] = useState(null)
  const [voted, setVoted]         = useState(null)   // 'UPVOTE' | 'DOWNVOTE' | null
  const [voting, setVoting]       = useState(null)
  const [voteError, setVoteError] = useState(null)
  const bottomRef = useRef(null)

  useEffect(() => {
    radarApi.getDeal(initialDeal.id)
      .then(res => setDeal(res.data))
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [initialDeal.id])

  useEffect(() => {
    if (!loading) bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [loading, deal?.comments?.length])

  async function handleVote(type) {
    if (voted || voting) return
    setVoting(type)
    setVoteError(null)
    try {
      await radarApi.vote(deal.id, type)
      setVoted(type)
      setDeal(d => ({
        ...d,
        upvotes:   type === 'UPVOTE'   ? (d.upvotes   ?? 0) + 1 : d.upvotes,
        downvotes: type === 'DOWNVOTE' ? (d.downvotes ?? 0) + 1 : d.downvotes,
      }))
    } catch (err) {
      setVoteError(err.response?.status === 409 ? 'You already voted on this deal.' : 'Vote failed.')
    } finally {
      setVoting(null)
    }
  }

  async function handleSendComment(e) {
    e.preventDefault()
    const text = comment.trim()
    if (!text) return
    setSending(true)
    setSendError(null)
    try {
      const res = await radarApi.addComment(deal.id, text)
      setDeal(d => ({
        ...d,
        comments: [...(d.comments ?? []), res.data],
      }))
      setComment('')
    } catch (err) {
      if (err.response?.status === 403) {
        setSendError('Your trust score is too low to comment (minimum 30 required).')
      } else {
        setSendError('Failed to post comment.')
      }
    } finally {
      setSending(false)
    }
  }

  const dotColor  = CATEGORY_COLORS[deal?.category] || CATEGORY_COLORS.OTHER
  const ttl       = timeLeft(deal?.expiresAt)
  const isExpired = ttl === 'Expired'
  const comments  = deal?.comments ?? []
  const upvotes   = Number(deal?.upvotes   ?? 0)
  const downvotes = Number(deal?.downvotes ?? 0)
  const score     = upvotes - downvotes
  const mapsUrl   = `https://maps.google.com/?q=${deal?.latitude},${deal?.longitude}`

  return (
    <div className="fixed inset-0 flex flex-col bg-slate-900/98 backdrop-blur-xl" style={{ zIndex: 1000 }}>
      {/* Header */}
      <div className="flex items-center justify-between px-5 pt-6 pb-4 border-b border-white/10 flex-shrink-0">
        <div className="flex items-center gap-2">
          <div
            className="w-3 h-3 rounded-full flex-shrink-0"
            style={{ background: dotColor, boxShadow: `0 0 8px ${dotColor}` }}
          />
          <span className="text-xs font-black text-slate-400 uppercase tracking-wider">{deal?.category}</span>
        </div>
        <button
          onClick={onClose}
          className="w-9 h-9 rounded-xl bg-slate-800/60 flex items-center justify-center text-slate-400 hover:text-slate-100"
        >
          <X size={18} />
        </button>
      </div>

      {/* Scrollable content */}
      <div className="flex-1 overflow-y-auto px-5 py-5 space-y-5">
        {/* Title + meta */}
        <div>
          <h1 className="text-2xl font-black text-slate-100 mb-2">{deal?.title}</h1>
          <div className="flex flex-wrap items-center gap-3 text-xs text-slate-500">
            <span className={ttl === 'Expired' ? 'text-rose-400 font-bold' : 'text-emerald-400 font-bold'}>
              {ttl}
            </span>
            <span>·</span>
            <span>Score: <span className={`font-bold ${score >= 0 ? 'text-emerald-400' : 'text-rose-400'}`}>{score > 0 ? '+' : ''}{score}</span></span>
            {deal?.reporterTrustScore !== undefined && (
              <>
                <span>·</span>
                <span className="flex items-center gap-1">
                  <ShieldCheck size={11} className="text-purple-400" />
                  Trust {deal.reporterTrustScore}
                </span>
              </>
            )}
          </div>
        </div>

        {/* Vote buttons */}
        <div className="flex gap-3">
          <button
            onClick={() => handleVote('UPVOTE')}
            disabled={!!voted || !!voting || isExpired}
            className={`flex-1 flex items-center justify-center gap-2 py-3.5 rounded-2xl font-black text-sm transition-all disabled:opacity-40 ${
              voted === 'UPVOTE'
                ? 'bg-emerald-500/30 border border-emerald-400/40 text-emerald-300'
                : 'bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 hover:bg-emerald-500/20'
            }`}
          >
            {voting === 'UPVOTE' ? <Loader2 size={16} className="animate-spin" /> : <ThumbsUp size={16} />}
            {upvotes} Upvote{upvotes !== 1 ? 's' : ''}
          </button>
          <button
            onClick={() => handleVote('DOWNVOTE')}
            disabled={!!voted || !!voting || isExpired}
            className={`flex-1 flex items-center justify-center gap-2 py-3.5 rounded-2xl font-black text-sm transition-all disabled:opacity-40 ${
              voted === 'DOWNVOTE'
                ? 'bg-rose-500/30 border border-rose-400/40 text-rose-300'
                : 'bg-rose-500/10 border border-rose-500/20 text-rose-400 hover:bg-rose-500/20'
            }`}
          >
            {voting === 'DOWNVOTE' ? <Loader2 size={16} className="animate-spin" /> : <ThumbsDown size={16} />}
            {downvotes} Downvote{downvotes !== 1 ? 's' : ''}
          </button>
        </div>
        {voteError && <p className="text-xs text-rose-400 text-center -mt-2">{voteError}</p>}

        {/* Description */}
        {deal?.description && (
          <div className="glass-card rounded-2xl p-4">
            <p className="text-sm text-slate-300 leading-relaxed">{deal.description}</p>
          </div>
        )}

        {/* Navigate CTA */}
        <a
          href={mapsUrl}
          target="_blank"
          rel="noopener noreferrer"
          className="flex items-center justify-center gap-2 w-full rounded-2xl bg-emerald-500/15 border border-emerald-500/30 py-3.5 text-sm font-black text-emerald-300 hover:bg-emerald-500/25"
        >
          <Navigation size={16} />
          Take Me There
        </a>

        {/* Comments */}
        <div>
          <div className="flex items-center gap-2 mb-3">
            <MessageCircle size={14} className="text-purple-400" />
            <h3 className="text-sm font-bold text-slate-300 uppercase tracking-wider">
              Comments {!loading && `(${comments.length})`}
            </h3>
          </div>

          {loading && (
            <div className="flex items-center justify-center py-8">
              <Loader2 size={20} className="animate-spin text-purple-400" />
            </div>
          )}

          {!loading && comments.length === 0 && (
            <p className="text-center text-sm text-slate-600 py-6">No comments yet. Be the first!</p>
          )}

          {!loading && comments.length > 0 && (
            <div className="space-y-3">
              {comments.map((c, i) => (
                <div key={c.id ?? i} className="glass-card rounded-2xl px-4 py-3">
                  <div className="flex items-center justify-between mb-1">
                    <span className="text-xs font-bold text-purple-300">{c.authorName ?? 'Student'}</span>
                    <span className="text-xs text-slate-600">{fmt(c.createdAt)}</span>
                  </div>
                  <p className="text-sm text-slate-300">{c.content}</p>
                </div>
              ))}
              <div ref={bottomRef} />
            </div>
          )}
        </div>
      </div>

      {/* Comment input */}
      <form onSubmit={handleSendComment} className="px-5 py-4 border-t border-white/10 flex-shrink-0 bg-slate-900/80">
        {sendError && (
          <p className="text-xs text-rose-400 mb-2">{sendError}</p>
        )}
        <div className="flex items-center gap-3">
          <input
            className="flex-1 rounded-2xl bg-slate-800/60 border border-white/10 px-4 py-3 text-sm text-slate-100 placeholder-slate-500 focus:outline-none focus:border-purple-500/50"
            placeholder="Add a comment…"
            value={comment}
            onChange={e => setComment(e.target.value)}
            maxLength={500}
            disabled={sending}
          />
          <button
            type="submit"
            disabled={sending || !comment.trim()}
            className="w-11 h-11 flex-shrink-0 flex items-center justify-center rounded-2xl bg-purple-500 text-white shadow-[0_0_16px_rgba(168,85,247,0.4)] hover:bg-purple-400 disabled:opacity-40"
          >
            {sending ? <Loader2 size={16} className="animate-spin" /> : <Send size={16} />}
          </button>
        </div>
      </form>
    </div>
  )
}
