import { useState } from 'react'
import { Download, X } from 'lucide-react'
import { useInstallPrompt } from '../../hooks/useInstallPrompt'

export default function InstallBanner() {
  const { canInstall, promptInstall } = useInstallPrompt()
  const [dismissed, setDismissed]    = useState(false)

  if (!canInstall || dismissed) return null

  return (
    <div className="fixed bottom-6 left-4 right-4 z-50 md:left-auto md:right-6 md:w-80">
      <div className="glass-card accent-glow rounded-2xl p-4 flex items-center gap-3">
        <div className="w-10 h-10 rounded-xl bg-purple-500/20 flex items-center justify-center flex-shrink-0">
          <Download size={20} className="text-purple-400" />
        </div>
        <div className="flex-1 min-w-0">
          <p className="text-sm font-bold text-slate-100 leading-tight">Install StuFi</p>
          <p className="text-xs text-slate-400 mt-0.5">Works offline · Native feel</p>
        </div>
        <div className="flex items-center gap-2 flex-shrink-0">
          <button
            onClick={promptInstall}
            className="px-3 py-1.5 rounded-xl bg-purple-500 text-white text-xs font-bold hover:bg-purple-400"
          >
            Install
          </button>
          <button
            onClick={() => setDismissed(true)}
            className="p-1.5 rounded-xl hover:bg-white/10 text-slate-500"
          >
            <X size={14} />
          </button>
        </div>
      </div>
    </div>
  )
}
