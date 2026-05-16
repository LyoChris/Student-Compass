import { Zap, Plus, Upload } from 'lucide-react'
import AppShell from '../../components/layout/AppShell'

export default function AIChatPage() {
  return (
    <AppShell>
      <div className="max-w-2xl mx-auto px-4 pt-6 pb-28 md:pb-8 flex flex-col" style={{ height: 'calc(100vh - 0px)' }}>
        <div className="flex items-center justify-between mb-6">
          <div>
            <h1 className="text-2xl font-black text-slate-100 tracking-tight">AI Chat</h1>
            <p className="text-slate-500 text-sm">Your personal finance coach</p>
          </div>
          <div className="w-10 h-10 rounded-2xl bg-purple-500/20 flex items-center justify-center">
            <Zap size={18} className="text-purple-400" />
          </div>
        </div>

        <div className="flex-1 glass-card rounded-3xl p-8 flex flex-col items-center justify-center text-center mb-4">
          <div className="w-16 h-16 rounded-2xl bg-purple-500/20 flex items-center justify-center mx-auto mb-4">
            <Zap size={28} className="text-purple-400" />
          </div>
          <h2 className="text-xl font-black text-slate-100 mb-2">AI Financial Coach</h2>
          <p className="text-slate-400 text-sm leading-relaxed max-w-xs">
            Ask anything about your budget, expenses, or student finance. Upload bank statements for personalized insights.
          </p>
          <span className="inline-block mt-4 text-xs font-bold px-3 py-1.5 rounded-full bg-purple-500/20 text-purple-400">
            In development
          </span>
        </div>

        <div className="glass-card rounded-3xl p-4">
          <div className="flex items-center gap-3 bg-slate-700/50 rounded-2xl px-4 py-3 mb-3">
            <input type="text" placeholder="Ask StuFi about your budget..." className="flex-1 bg-transparent text-slate-100 placeholder-slate-500 text-sm outline-none" disabled />
            <button className="w-8 h-8 rounded-xl bg-purple-500/50 flex items-center justify-center cursor-not-allowed" disabled>
              <Plus size={16} className="text-white/50" />
            </button>
          </div>
          <div className="flex gap-2">
            <button className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-700/50 text-slate-500 text-xs font-semibold cursor-not-allowed" disabled>
              <Upload size={11} /> Bank Statement
            </button>
            <button className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-700/50 text-slate-500 text-xs font-semibold cursor-not-allowed" disabled>
              <Upload size={11} /> Pachet acasa
            </button>
          </div>
        </div>
      </div>
    </AppShell>
  )
}
