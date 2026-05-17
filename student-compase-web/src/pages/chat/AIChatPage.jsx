import { useState, useEffect, useRef, useCallback } from 'react'
import { Zap, Send, AlertCircle, X } from 'lucide-react'
import AppShell from '../../components/layout/AppShell'
import { aiChatApi } from '../../api/aiChatApi'

// ─── Typing indicator (animated dots) ────────────────────────────────────────
function TypingBubble() {
  return (
    <div className="flex items-end gap-2 justify-start">
      <div className="w-7 h-7 rounded-full bg-purple-500/20 flex items-center justify-center flex-shrink-0 mb-0.5">
        <Zap size={13} className="text-purple-400" />
      </div>
      <div className="bg-slate-800 border border-slate-700/60 rounded-2xl rounded-bl-sm px-4 py-3">
        <div className="flex items-center gap-1.5 h-4">
          {[0, 1, 2].map((i) => (
            <span
              key={i}
              className="w-1.5 h-1.5 rounded-full bg-slate-400"
              style={{
                animation: 'bounce 1.2s ease-in-out infinite',
                animationDelay: `${i * 0.2}s`,
              }}
            />
          ))}
        </div>
      </div>
    </div>
  )
}

// ─── Single message bubble ────────────────────────────────────────────────────
function MessageBubble({ role, content, createdAt }) {
  const isUser = role === 'USER'

  return (
    <div className={`flex items-end gap-2 ${isUser ? 'justify-end' : 'justify-start'}`}>
      {!isUser && (
        <div className="w-7 h-7 rounded-full bg-purple-500/20 flex items-center justify-center flex-shrink-0 mb-0.5">
          <Zap size={13} className="text-purple-400" />
        </div>
      )}

      <div className={`max-w-[78%] ${isUser ? 'items-end' : 'items-start'} flex flex-col gap-1`}>
        <div
          className={`px-4 py-2.5 rounded-2xl text-sm leading-relaxed whitespace-pre-wrap break-words ${
            isUser
              ? 'bg-purple-500 text-white rounded-br-sm'
              : 'bg-slate-800 border border-slate-700/60 text-gray-200 rounded-bl-sm'
          }`}
        >
          {content}
        </div>
        {createdAt && (
          <span className="text-[10px] text-slate-600 px-1">
            {new Date(createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
          </span>
        )}
      </div>

      {isUser && (
        <div className="w-7 h-7 rounded-full bg-purple-500/30 flex items-center justify-center flex-shrink-0 mb-0.5">
          <span className="text-purple-200 text-[10px] font-black">ME</span>
        </div>
      )}
    </div>
  )
}

// ─── Toast notification ───────────────────────────────────────────────────────
function Toast({ message, onClose }) {
  useEffect(() => {
    const t = setTimeout(onClose, 4000)
    return () => clearTimeout(t)
  }, [onClose])

  return (
    <div className="fixed bottom-6 left-1/2 -translate-x-1/2 z-50 flex items-center gap-3
                    glass-card border border-rose-500/30 rounded-2xl px-4 py-3 shadow-xl max-w-sm w-[calc(100%-2rem)]">
      <AlertCircle size={16} className="text-rose-400 flex-shrink-0" />
      <p className="text-rose-300 text-sm flex-1">{message}</p>
      <button onClick={onClose} className="text-slate-500 hover:text-slate-300">
        <X size={14} />
      </button>
    </div>
  )
}

// ─── Empty chat state ─────────────────────────────────────────────────────────
function EmptyChat() {
  const SUGGESTIONS = [
    'How much should I spend on groceries?',
    'Give me tips to save more this month.',
    'How can I reduce transport costs?',
    "What's a good emergency fund target?",
  ]
  return (
    <div className="flex flex-col items-center justify-center h-full text-center px-6 gap-5">
      <div className="w-14 h-14 rounded-2xl bg-purple-500/20 flex items-center justify-center">
        <Zap size={26} className="text-purple-400" />
      </div>
      <div>
        <h2 className="text-lg font-black text-slate-100 mb-1">AI Finance Coach</h2>
        <p className="text-slate-400 text-sm leading-relaxed">
          Ask me anything about your budget, spending habits, or student finance.
        </p>
      </div>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 w-full max-w-sm">
        {SUGGESTIONS.map((s) => (
          <div
            key={s}
            className="glass-card rounded-xl px-3 py-2 text-xs text-slate-400 text-left hover:border-purple-500/30 cursor-default"
          >
            {s}
          </div>
        ))}
      </div>
    </div>
  )
}

// ─── Main component ───────────────────────────────────────────────────────────
export default function AIChatPage() {
  const [messages, setMessages]   = useState([])
  const [input, setInput]         = useState('')
  const [histLoading, setHistLoad] = useState(true)
  const [sending, setSending]     = useState(false)
  const [isTyping, setIsTyping]   = useState(false)
  const [toast, setToast]         = useState(null)

  const messagesEndRef = useRef(null)
  const inputRef       = useRef(null)

  // ── Auto-scroll whenever messages change ───────────────────────────────────
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, isTyping])

  // ── Load history on mount ──────────────────────────────────────────────────
  useEffect(() => {
    const load = async () => {
      try {
        const { data } = await aiChatApi.getHistory(50)
        // Backend returns newest-first; reverse to chronological order
        const items = Array.isArray(data?.content) ? data.content : (Array.isArray(data) ? data : [])
        setMessages([...items].reverse())
      } catch {
        // Silently ignore — user can still chat
      } finally {
        setHistLoad(false)
      }
    }
    load()
  }, [])

  // ── Send message ───────────────────────────────────────────────────────────
  const handleSend = useCallback(async () => {
    const text = input.trim()
    if (!text || sending) return

    setInput('')
    setSending(true)

    // 1. Optimistic user bubble
    const optimisticMsg = {
      id:        `opt-${Date.now()}`,
      role:      'USER',
      content:   text,
      createdAt: new Date().toISOString(),
      _optimistic: true,
    }
    setMessages((prev) => [...prev, optimisticMsg])

    // 2. Typing indicator
    setIsTyping(true)

    try {
      const { data } = await aiChatApi.sendMessage(text)

      // 3. Replace optimistic message with persisted one, add AI reply
      setIsTyping(false)
      setMessages((prev) => {
        const withoutOptimistic = prev.filter((m) => !m._optimistic)
        const userMsg = { ...optimisticMsg, _optimistic: false }
        const aiMsg   = {
          id:        data.messageId,
          role:      'ASSISTANT',
          content:   data.reply,
          createdAt: new Date().toISOString(),
        }
        return [...withoutOptimistic, userMsg, aiMsg]
      })
    } catch (err) {
      setIsTyping(false)
      // Roll back the optimistic message
      setMessages((prev) => prev.filter((m) => !m._optimistic))
      const msg = err.response?.data?.message ?? 'Failed to send message. Please try again.'
      setToast(msg)
    } finally {
      setSending(false)
      inputRef.current?.focus()
    }
  }, [input, sending])

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  const isEmpty = !histLoading && messages.length === 0

  return (
    <AppShell>
      {/* h-[calc(100dvh-3.5rem)]: subtract 56px mobile header added by AppShell */}
      <div className="max-w-2xl mx-auto w-full px-4 pt-4 pb-4 flex flex-col h-[calc(100dvh-3.5rem)] md:h-dvh">

        {/* ── Header ── */}
        <div className="flex items-center justify-between mb-4 flex-shrink-0">
          <div>
            <h1 className="text-2xl font-black text-slate-100 tracking-tight">AI Chat</h1>
            <p className="text-slate-500 text-sm">Your personal finance coach</p>
          </div>
          <div className="w-10 h-10 rounded-2xl bg-purple-500/20 flex items-center justify-center">
            <Zap size={18} className="text-purple-400" />
          </div>
        </div>

        {/* ── Chat Window ── */}
        <div className="flex-1 glass-card rounded-3xl flex flex-col overflow-hidden mb-3">

          {/* Message list */}
          <div className="flex-1 overflow-y-auto p-4 space-y-4">
            {histLoading ? (
              <div className="flex flex-col gap-3 py-4">
                {[1, 2, 3].map((i) => (
                  <div key={i} className={`flex gap-2 ${i % 2 === 0 ? 'justify-end' : 'justify-start'}`}>
                    <div className={`skeleton h-10 rounded-2xl ${i % 2 === 0 ? 'w-48' : 'w-64'}`} />
                  </div>
                ))}
              </div>
            ) : isEmpty ? (
              <EmptyChat />
            ) : (
              messages.map((msg) => (
                <MessageBubble
                  key={msg.id}
                  role={msg.role}
                  content={msg.content}
                  createdAt={msg.createdAt}
                />
              ))
            )}

            {isTyping && <TypingBubble />}
            <div ref={messagesEndRef} />
          </div>
        </div>

        {/* ── Input Area ── */}
        <div className="glass-card rounded-3xl p-3 flex-shrink-0">
          <div className="flex items-end gap-2">
            <textarea
              ref={inputRef}
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Ask StuFi about your budget…"
              rows={1}
              maxLength={4000}
              disabled={sending || histLoading}
              className="flex-1 bg-slate-700/50 text-slate-100 placeholder-slate-500 text-sm
                         rounded-2xl px-4 py-3 outline-none resize-none leading-relaxed
                         disabled:opacity-50 disabled:cursor-not-allowed
                         focus:ring-1 focus:ring-purple-500/50"
              style={{ maxHeight: '120px', overflowY: 'auto' }}
              onInput={(e) => {
                e.target.style.height = 'auto'
                e.target.style.height = `${Math.min(e.target.scrollHeight, 120)}px`
              }}
            />
            <button
              onClick={handleSend}
              disabled={!input.trim() || sending || histLoading}
              className="w-11 h-11 rounded-2xl bg-purple-500 hover:bg-purple-400 flex items-center justify-center
                         disabled:opacity-40 disabled:cursor-not-allowed flex-shrink-0 mb-0.5
                         shadow-[0_0_20px_rgba(168,85,247,0.4)]"
              aria-label="Send message"
            >
              <Send size={17} className="text-white" />
            </button>
          </div>

          {input.length > 3500 && (
            <p className="text-right text-xs text-slate-600 mt-1 pr-1">
              {input.length} / 4000
            </p>
          )}
        </div>
      </div>

      {/* ── Toast ── */}
      {toast && <Toast message={toast} onClose={() => setToast(null)} />}

      {/* ── Bounce keyframe (inline for portability) ── */}
      <style>{`
        @keyframes bounce {
          0%, 60%, 100% { transform: translateY(0); }
          30%            { transform: translateY(-5px); }
        }
      `}</style>
    </AppShell>
  )
}
