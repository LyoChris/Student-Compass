import { useState } from 'react'
import { Sparkles, X, Zap, Wrench } from 'lucide-react'
import { useTranslation } from 'react-i18next'

// ── Maintenance state ──────────────────────────────────────────────────────────
function MaintenancePanel() {
  return (
    <div className="flex flex-col items-center justify-center h-full text-center px-8 gap-5">
      {/* Animated icon */}
      <div
        className="w-20 h-20 rounded-3xl flex items-center justify-center"
        style={{
          background: 'linear-gradient(135deg, rgba(168,85,247,0.18), rgba(99,102,241,0.12))',
          border: '1px solid rgba(168,85,247,0.28)',
          boxShadow: '0 0 40px rgba(168,85,247,0.15)',
          animation: 'maint-pulse 3s ease-in-out infinite',
        }}
      >
        <Wrench size={34} className="text-purple-400" />
      </div>

      {/* Text */}
      <div className="space-y-2">
        <h3 className="text-base font-black text-slate-100 leading-tight">
          AI Coach — Coming Soon
        </h3>
        <p className="text-sm text-slate-500 leading-relaxed max-w-[220px]">
          The chat endpoint is under maintenance. Check back in a bit!
        </p>
      </div>

      {/* Status pill */}
      <div
        className="flex items-center gap-2 rounded-full px-4 py-2"
        style={{
          background: 'rgba(251,191,36,0.08)',
          border: '1px solid rgba(251,191,36,0.22)',
        }}
      >
        <span
          className="w-2 h-2 rounded-full bg-amber-400 flex-shrink-0"
          style={{ animation: 'maint-pulse 1.6s ease-in-out infinite' }}
        />
        <span className="text-[0.68rem] font-black text-amber-400 tracking-wide">
          MAINTENANCE
        </span>
      </div>
    </div>
  )
}

// ── Main component ─────────────────────────────────────────────────────────────
export default function ExpandableAiChat() {
  const { t }        = useTranslation()
  const [isExpanded, setIsExpanded] = useState(false)

  return (
    <>
      <style>{`
        @keyframes chat-glow {
          0%, 100% { box-shadow: 0 0 12px rgba(168,85,247,0.35); }
          50%       { box-shadow: 0 0 30px rgba(168,85,247,0.75), 0 0 56px rgba(168,85,247,0.2); }
        }
        @keyframes maint-pulse {
          0%, 100% { opacity: 1; }
          50%       { opacity: 0.55; }
        }
        .chat-trigger-pulse  { animation: chat-glow 2.8s ease-in-out infinite; }
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
          height:     isExpanded ? '360px' : '72px',
          transition: 'height 0.32s cubic-bezier(0.16,1,0.3,1), border-color 0.2s ease, box-shadow 0.2s ease',
          borderColor: isExpanded ? 'rgba(168,85,247,0.35)' : 'rgba(255,255,255,0.08)',
          background:  isExpanded ? 'rgba(13,18,30,0.97)' : 'rgba(30,41,59,0.8)',
          backdropFilter:       'blur(16px)',
          WebkitBackdropFilter: 'blur(16px)',
          boxShadow: isExpanded
            ? '0 0 0 1px rgba(168,85,247,0.15), 0 24px 60px rgba(0,0,0,0.6), 0 0 40px rgba(168,85,247,0.12)'
            : undefined,
        }}
      >
        {/* ── Collapsed trigger ── */}
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
          <div
            className="flex items-center gap-1.5 flex-shrink-0 rounded-full px-3 py-1.5"
            style={{
              border: '1px solid rgba(251,191,36,0.25)',
              background: 'rgba(251,191,36,0.08)',
            }}
          >
            <span className="w-1.5 h-1.5 rounded-full bg-amber-400 flex-shrink-0"
                  style={{ animation: 'maint-pulse 1.6s ease-in-out infinite' }} />
            <span className="text-[0.65rem] font-black text-amber-400">Maintenance</span>
          </div>
        </button>

        {/* ── Expanded panel ── */}
        {isExpanded && (
          <div className="chat-inner-fade-in absolute inset-0 flex flex-col">

            {/* Header */}
            <div
              className="flex items-center gap-3 px-4 py-3.5 flex-shrink-0 border-b border-white/8"
              style={{ background: 'rgba(168,85,247,0.07)' }}
            >
              <div
                className="w-9 h-9 rounded-2xl flex items-center justify-center flex-shrink-0"
                style={{
                  background: 'linear-gradient(135deg, #A855F7, #6366F1)',
                  boxShadow: '0 0 16px rgba(168,85,247,0.5)',
                }}
              >
                <Zap size={16} className="text-white" />
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-black text-slate-100 leading-tight">{t('chat.title')}</p>
                <div className="flex items-center gap-1.5 mt-0.5">
                  <span className="w-1.5 h-1.5 rounded-full bg-amber-400"
                        style={{ animation: 'maint-pulse 1.6s ease-in-out infinite' }} />
                  <span className="text-[0.65rem] font-semibold text-amber-400">Under Maintenance</span>
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

            {/* Maintenance body */}
            <div className="flex-1">
              <MaintenancePanel />
            </div>
          </div>
        )}
      </div>
    </>
  )
}
