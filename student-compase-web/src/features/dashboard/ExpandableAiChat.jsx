import { useState, useEffect, useRef, useCallback } from 'react'
import { Sparkles, X, Send, Zap, Loader2 } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../../hooks/useAuth'
import { aiChatApi } from '../../api/aiChatApi'

// ── Source badge ───────────────────────────────────────────────────────────────
function SourceBadge({ source }) {
  if (!source) return null
  const isGemini = source === 'gemini_fallback'
  return (
    <span
      className="inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[9px] font-black"
      style={isGemini
        ? {
            background: 'linear-gradient(90deg, rgba(59,130,246,0.18), rgba(168,85,247,0.18))',
            border: '1px solid rgba(99,102,241,0.35)',
            color: '#a5b4fc',
          }
        : {
            background: 'rgba(168,85,247,0.12)',
            border: '1px solid rgba(168,85,247,0.25)',
            color: '#c4b5fd',
          }
      }
    >
      {isGemini ? '✨ Gemini' : '🧠 Local AI'}
    </span>
  )
}

// ── Typing indicator ───────────────────────────────────────────────────────────
function TypingBubble() {
  return (
    <div className="flex items-end gap-2">
      <div className="w-7 h-7 rounded-full bg-purple-500/20 border border-purple-500/30
                      flex items-center justify-center flex-shrink-0 mb-0.5">
        <Zap size={12} className="text-purple-400" />
      </div>
      <div className="rounded-2xl rounded-bl-sm px-4 py-3 border border-white/8"
           style={{ background: 'rgba(168,85,247,0.10)' }}>
        <div className="flex items-center gap-1.5 h-4">
          {[0, 1, 2].map(i => (
            <span
              key={i}
              className="w-1.5 h-1.5 rounded-full bg-purple-400"
              style={{ animation: `typing-dot 1.2s ease-in-out ${i * 0.2}s infinite` }}
            />
          ))}
        </div>
      </div>
    </div>
  )
}

// ── Single message bubble ──────────────────────────────────────────────────────
function MessageBubble({ role, content, createdAt, source }) {
  const isUser = role === 'USER'
  return (
    <div className={`flex items-end gap-2 ${isUser ? 'flex-row-reverse' : 'flex-row'}`}>
      <div className={`w-7 h-7 rounded-full flex items-center justify-center flex-shrink-0 mb-0.5 ${
        isUser
          ? 'bg-purple-500/30 border border-purple-400/40'
          : 'bg-purple-500/20 border border-purple-500/30'
      }`}>
        {isUser
          ? <span className="text-purple-200 text-[9px] font-black">ME</span>
          : <Zap size={12} className="text-purple-400" />
        }
      </div>

      <div className={`max-w-[78%] flex flex-col gap-1 ${isUser ? 'items-end' : 'items-start'}`}>
        <div
          className={`px-4 py-2.5 text-sm leading-relaxed whitespace-pre-wrap break-words ${
            isUser
              ? 'rounded-2xl rounded-br-sm text-white'
              : 'rounded-2xl rounded-bl-sm text-slate-200 border border-white/8'
          }`}
          style={isUser
            ? { background: 'linear-gradient(135deg, #A855F7, #7C3AED)', boxShadow: '0 4px 16px rgba(168,85,247,0.3)' }
            : { background: 'rgba(30,41,59,0.90)', backdropFilter: 'blur(8px)' }
          }
        >
          {content}
        </div>
        <div className={`flex items-center gap-1.5 px-1 ${isUser ? 'flex-row-reverse' : 'flex-row'}`}>
          {createdAt && (
            <span className="text-[10px] text-slate-600">
              {new Date(createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
            </span>
          )}
          {!isUser && <SourceBadge source={source} />}
        </div>
      </div>
    </div>
  )
}

// ── Empty / welcome state ──────────────────────────────────────────────────────
function WelcomeState({ firstName }) {
  const { t } = useTranslation()
  const suggestions = [t('chat.suggestion1'), t('chat.suggestion2'), t('chat.suggestion3')]
  return (
    <div className="flex flex-col items-center justify-center h-full text-center px-6 py-8 gap-4">
      <div
        className="w-16 h-16 rounded-2xl flex items-center justify-center"
        style={{
          background: 'linear-gradient(135deg, rgba(168,85,247,0.25), rgba(99,102,241,0.15))',
          border: '1px solid rgba(168,85,247,0.3)',
        }}
      >
        <Zap size={28} className="text-purple-400" />
      </div>
      <div>
        <h3 className="text-base font-black text-slate-100 mb-1">
          {t('chat.welcomeTitle', { name: firstName })}
        </h3>
        <p className="text-slate-400 text-xs leading-relaxed max-w-[220px]">
          {t('chat.welcomeBody')}
        </p>
      </div>
      <div className="flex flex-wrap justify-center gap-2 mt-1">
        {suggestions.map(s => (
          <span key={s} className="glass-card rounded-xl px-3 py-1.5 text-[0.65rem] font-semibold text-slate-500">
            {s}
          </span>
        ))}
      </div>
    </div>
  )
}

// ── Main component ─────────────────────────────────────────────────────────────
export default function ExpandableAiChat() {
  const { user }  = useAuth()
  const { t }     = useTranslation()
  const firstName = user?.firstName ?? 'there'

  const [isExpanded,  setIsExpanded]  = useState(false)
  const [messages,    setMessages]    = useState([])
  const [histLoaded,  setHistLoaded]  = useState(false)
  const [histLoading, setHistLoading] = useState(false)
  const [input,       setInput]       = useState('')
  const [sending,     setSending]     = useState(false)
  const [isTyping,    setIsTyping]    = useState(false)

  const textareaRef    = useRef(null)
  const messagesEndRef = useRef(null)

  // ── Load history once on first open ──────────────────────────────────────────
  useEffect(() => {
    if (!isExpanded || histLoaded || histLoading) return
    setHistLoading(true)
    aiChatApi.getHistory(40)
      .then(({ data }) => {
        const items = Array.isArray(data?.content) ? data.content
                    : Array.isArray(data)          ? data
                    : []
        setMessages([...items].reverse())
      })
      .catch(() => {})
      .finally(() => { setHistLoaded(true); setHistLoading(false) })
  }, [isExpanded, histLoaded, histLoading])

  // ── Auto-scroll ───────────────────────────────────────────────────────────────
  useEffect(() => {
    if (isExpanded) messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, isTyping, isExpanded])

  // ── Focus textarea after expand ───────────────────────────────────────────────
  useEffect(() => {
    if (isExpanded) {
      const id = setTimeout(() => textareaRef.current?.focus(), 320)
      return () => clearTimeout(id)
    }
  }, [isExpanded])

  // ── Send via Multi-LLM stateless endpoint ─────────────────────────────────────
  const handleSend = useCallback(async () => {
    const text = input.trim()
    if (!text || sending) return

    setInput('')
    if (textareaRef.current) textareaRef.current.style.height = 'auto'
    setSending(true)

    const optimisticUser = {
      id: `opt-${Date.now()}`, role: 'USER',
      content: text, createdAt: new Date().toISOString(), _optimistic: true,
    }
    setMessages(prev => [...prev, optimisticUser])
    setIsTyping(true)

    try {
      const { data } = await aiChatApi.statelessChat(text)
      setIsTyping(false)
      setMessages(prev => {
        const clean = prev.filter(m => !m._optimistic)
        return [
          ...clean,
          { ...optimisticUser, _optimistic: false },
          { id: `ai-${Date.now()}`, role: 'ASSISTANT', content: data.reply, createdAt: new Date().toISOString(), source: data.source },
        ]
      })
    } catch {
      setIsTyping(false)
      setMessages(prev => prev.filter(m => !m._optimistic))
    } finally {
      setSending(false)
      textareaRef.current?.focus()
    }
  }, [input, sending])

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); handleSend() }
  }

  const handleTextareaInput = (e) => {
    const el = e.target
    el.style.height = 'auto'
    el.style.height = `${Math.min(el.scrollHeight, 112)}px`
    setInput(el.value)
  }

  const isEmpty = histLoaded && messages.length === 0

  return (
    <>
      <style>{`
        @keyframes chat-glow {
          0%, 100% { box-shadow: 0 0 12px rgba(168,85,247,0.35); }
          50%       { box-shadow: 0 0 30px rgba(168,85,247,0.75), 0 0 56px rgba(168,85,247,0.2); }
        }
        @keyframes typing-dot {
          0%, 60%, 100% { transform: translateY(0);    opacity: 0.5; }
          30%           { transform: translateY(-5px); opacity: 1;   }
        }
        .chat-trigger-pulse { animation: chat-glow 2.8s ease-in-out infinite; }
        .chat-inner-fade-in  { animation: fadeSlideUp 0.26s cubic-bezier(0.16,1,0.3,1) forwards; }
        @keyframes fadeSlideUp {
          from { opacity: 0; transform: translateY(10px); }
          to   { opacity: 1; transform: translateY(0);    }
        }
      `}</style>

      {/* ── Expandable container ─────────────────────────────────────────────── */}
      <div
        className="relative rounded-3xl overflow-hidden border"
        style={{
          height:     isExpanded ? '520px' : '72px',
          transition: 'height 0.32s cubic-bezier(0.16,1,0.3,1), border-color 0.2s ease, box-shadow 0.2s ease',
          borderColor: isExpanded ? 'rgba(168,85,247,0.35)' : 'rgba(255,255,255,0.08)',
          background:  isExpanded
            ? 'rgba(13,18,30,0.97)'
            : 'rgba(30,41,59,0.8)',
          backdropFilter:       'blur(16px)',
          WebkitBackdropFilter: 'blur(16px)',
          boxShadow: isExpanded
            ? '0 0 0 1px rgba(168,85,247,0.15), 0 24px 60px rgba(0,0,0,0.6), 0 0 40px rgba(168,85,247,0.12)'
            : undefined,
        }}
      >
        {/* ── Collapsed trigger (visible only when not expanded) ── */}
        <button
          onClick={() => setIsExpanded(true)}
          aria-label={t('chat.askCoach')}
          className={`chat-trigger-pulse absolute inset-0 w-full flex items-center gap-4 px-5 py-4
                      hover:bg-purple-500/5 active:scale-[0.99] text-left
                      transition-opacity duration-200 ${isExpanded ? 'opacity-0 pointer-events-none' : 'opacity-100'}`}
        >
          <div
            className="w-11 h-11 rounded-2xl flex items-center justify-center flex-shrink-0"
            style={{
              background: 'linear-gradient(135deg, rgba(168,85,247,0.25), rgba(99,102,241,0.18))',
              border: '1px solid rgba(168,85,247,0.4)',
            }}
          >
            <Sparkles size={20} className="text-purple-300" />
          </div>
          <div className="flex-1 min-w-0">
            <p className="text-sm font-black text-slate-100 leading-tight">{t('chat.askCoach')}</p>
            <p className="text-xs text-slate-500 mt-0.5 truncate">{t('chat.welcomeBody')}</p>
          </div>
          <div className="flex items-center gap-1.5 flex-shrink-0 rounded-full px-3 py-1.5
                          border border-purple-500/30 bg-purple-500/10">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse flex-shrink-0" />
            <span className="text-[0.65rem] font-black text-purple-300">{t('common.online')}</span>
          </div>
        </button>

        {/* ── Expanded chat panel (visible only when expanded) ── */}
        {isExpanded && (
          <div className="chat-inner-fade-in absolute inset-0 flex flex-col">

            {/* Header */}
            <div
              className="flex items-center gap-3 px-4 py-3.5 flex-shrink-0 border-b border-white/8"
              style={{ background: 'rgba(168,85,247,0.07)' }}
            >
              <div
                className="w-9 h-9 rounded-2xl flex items-center justify-center flex-shrink-0"
                style={{ background: 'linear-gradient(135deg, #A855F7, #6366F1)', boxShadow: '0 0 16px rgba(168,85,247,0.5)' }}
              >
                <Zap size={16} className="text-white" />
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-black text-slate-100 leading-tight">{t('chat.title')}</p>
                <div className="flex items-center gap-1.5 mt-0.5">
                  <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
                  <span className="text-[0.65rem] font-semibold text-emerald-400">{t('common.online')} · Multi-LLM HA</span>
                </div>
              </div>
              <button
                onClick={() => setIsExpanded(false)}
                aria-label={t('common.close')}
                className="w-8 h-8 rounded-xl flex items-center justify-center text-slate-500
                           hover:text-slate-200 hover:bg-white/8 active:scale-90 transition-all flex-shrink-0"
              >
                <X size={16} />
              </button>
            </div>

            {/* Messages */}
            <div
              className="flex-1 overflow-y-auto px-4 py-4 space-y-4"
              style={{ scrollbarWidth: 'thin', scrollbarColor: 'rgba(168,85,247,0.25) transparent' }}
            >
              {histLoading && (
                <div className="space-y-4 py-2">
                  {[1, 2, 3].map(i => (
                    <div key={i} className={`flex gap-2 ${i % 2 === 0 ? 'flex-row-reverse' : ''}`}>
                      <div className="w-7 h-7 rounded-full skeleton flex-shrink-0" />
                      <div className={`skeleton h-10 rounded-2xl ${i % 2 === 0 ? 'w-40' : 'w-52'}`} />
                    </div>
                  ))}
                </div>
              )}

              {!histLoading && isEmpty && <WelcomeState firstName={firstName} />}

              {!histLoading && messages.map(msg => (
                <MessageBubble
                  key={msg.id}
                  role={msg.role}
                  content={msg.content}
                  createdAt={msg.createdAt}
                  source={msg.source}
                />
              ))}

              {isTyping && <TypingBubble />}
              <div ref={messagesEndRef} />
            </div>

            {/* Input */}
            <div
              className="flex-shrink-0 px-3 pb-3 pt-2 border-t border-white/8"
              style={{ background: 'rgba(168,85,247,0.04)' }}
            >
              <div
                className="flex items-end gap-2 rounded-2xl border border-white/10 px-3 py-2"
                style={{ background: 'rgba(30,41,59,0.6)', backdropFilter: 'blur(8px)' }}
              >
                <textarea
                  ref={textareaRef}
                  value={input}
                  onChange={handleTextareaInput}
                  onKeyDown={handleKeyDown}
                  placeholder={t('chat.placeholder')}
                  rows={1}
                  maxLength={4000}
                  disabled={sending || histLoading}
                  className="flex-1 bg-transparent text-slate-100 placeholder-slate-500 text-sm
                             outline-none resize-none leading-relaxed py-1
                             disabled:opacity-50 disabled:cursor-not-allowed"
                  style={{ maxHeight: '112px', overflowY: 'auto' }}
                />
                <button
                  onClick={handleSend}
                  disabled={!input.trim() || sending || histLoading}
                  aria-label={t('common.send')}
                  className="w-9 h-9 rounded-xl flex items-center justify-center flex-shrink-0 mb-0.5
                             transition-all duration-200 active:scale-90
                             disabled:opacity-35 disabled:cursor-not-allowed"
                  style={input.trim()
                    ? { background: 'linear-gradient(135deg,#A855F7,#7C3AED)', boxShadow: '0 0 18px rgba(168,85,247,0.5)' }
                    : { background: 'rgba(30,41,59,0.8)' }
                  }
                >
                  {sending
                    ? <Loader2 size={15} className="text-white animate-spin" />
                    : <Send size={15} className={input.trim() ? 'text-white' : 'text-slate-500'} />
                  }
                </button>
              </div>

              {input.length > 3500 && (
                <p className="text-right text-[10px] text-slate-600 mt-1 pr-1">
                  {t('chat.charLimit', { count: input.length })}
                </p>
              )}
            </div>
          </div>
        )}
      </div>
    </>
  )
}
