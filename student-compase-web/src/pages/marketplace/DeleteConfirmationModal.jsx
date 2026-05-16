import { useEffect } from 'react'
import { AlertTriangle, Loader2, X } from 'lucide-react'

export default function DeleteConfirmationModal({ item, onConfirm, onCancel, loading }) {
  // Close on Escape key
  useEffect(() => {
    const handle = (e) => { if (e.key === 'Escape') onCancel() }
    document.addEventListener('keydown', handle)
    return () => document.removeEventListener('keydown', handle)
  }, [onCancel])

  if (!item) return null

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="delete-modal-title"
    >
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-slate-950/80 backdrop-blur-sm"
        onClick={!loading ? onCancel : undefined}
      />

      {/* Panel */}
      <div className="relative z-10 w-full max-w-sm glass-card rounded-3xl p-6 shadow-2xl border-rose-500/20 border">
        {/* Close button */}
        <button
          onClick={onCancel}
          disabled={loading}
          className="absolute right-4 top-4 w-8 h-8 flex items-center justify-center rounded-xl text-slate-500 hover:text-slate-300 hover:bg-white/5 disabled:opacity-40"
          aria-label="Close"
        >
          <X size={16} />
        </button>

        {/* Icon */}
        <div className="w-12 h-12 rounded-2xl bg-rose-500/15 flex items-center justify-center mb-4">
          <AlertTriangle size={22} className="text-rose-400" />
        </div>

        {/* Heading */}
        <h2
          id="delete-modal-title"
          className="text-lg font-black text-slate-100 mb-2"
        >
          Delete listing?
        </h2>

        {/* Body */}
        <p className="text-sm text-slate-400 leading-relaxed mb-1">
          You are about to permanently delete{' '}
          <span className="font-bold text-slate-200">&ldquo;{item.title}&rdquo;</span>.
        </p>
        <p className="text-sm text-rose-400/80 font-semibold mb-6">
          This action cannot be undone.
        </p>

        {/* Actions */}
        <div className="flex gap-3">
          <button
            onClick={onCancel}
            disabled={loading}
            className="flex-1 rounded-2xl border border-slate-600 px-4 py-2.5 text-sm font-bold text-slate-300
                       hover:border-slate-500 hover:bg-white/5 disabled:opacity-40 disabled:cursor-not-allowed"
          >
            Cancel
          </button>
          <button
            onClick={onConfirm}
            disabled={loading}
            className="flex-1 flex items-center justify-center gap-2 rounded-2xl bg-rose-600 hover:bg-rose-500
                       px-4 py-2.5 text-sm font-bold text-white shadow-[0_0_20px_rgba(239,68,68,0.3)]
                       disabled:opacity-60 disabled:cursor-not-allowed"
          >
            {loading && <Loader2 size={15} className="animate-spin" />}
            {loading ? 'Deleting…' : 'Delete Permanently'}
          </button>
        </div>
      </div>
    </div>
  )
}
