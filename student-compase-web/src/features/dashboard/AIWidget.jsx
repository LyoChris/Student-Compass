import { useState } from 'react'
import { Zap, Plus, Upload } from 'lucide-react'

export default function AIWidget() {
  const [message, setMessage] = useState('')

  return (
    <div className="glass-card rounded-3xl p-4 hover:border-purple-500/30">
      <div className="flex items-center gap-3 mb-3">
        <div className="w-8 h-8 rounded-xl bg-purple-500/20 flex items-center justify-center flex-shrink-0">
          <Zap size={15} className="text-purple-400" />
        </div>
        <p className="text-sm font-bold text-slate-300">Ask StuFi</p>
        <span className="ml-auto text-xs text-purple-400 font-semibold px-2 py-0.5 rounded-full bg-purple-500/15">
          AI
        </span>
      </div>

      <div className="flex items-center gap-3 bg-slate-700/50 rounded-2xl px-4 py-3">
        <input
          type="text"
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          placeholder="Ask StuFi about your budget..."
          className="flex-1 bg-transparent text-slate-100 placeholder-slate-500 text-sm outline-none"
        />
        <button
          className="w-8 h-8 rounded-xl bg-purple-500 flex items-center justify-center hover:bg-purple-400 flex-shrink-0 active:scale-90"
          aria-label="Send"
        >
          <Plus size={16} className="text-white" />
        </button>
      </div>

      <div className="flex items-center gap-2 mt-3">
        <button className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-700/50 text-slate-400 text-xs font-semibold hover:bg-slate-700">
          <Upload size={11} />
          Bank Statement
        </button>
        <button className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-700/50 text-slate-400 text-xs font-semibold hover:bg-slate-700">
          <Upload size={11} />
          Pachet acasa
        </button>
      </div>
    </div>
  )
}
