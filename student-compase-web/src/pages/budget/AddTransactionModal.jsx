import { useState, useEffect } from 'react'
import { X, AlertCircle, Loader2, Receipt } from 'lucide-react'
import { budgetApi } from '../../api/budgetApi'

const CAT_META = {
  FOOD:      { label: 'Food'      },
  TRANSPORT: { label: 'Transport' },
  HOUSING:   { label: 'Housing'   },
  SUPPLIES:  { label: 'Supplies'  },
  PERSONAL:  { label: 'Personal'  },
  LEISURE:   { label: 'Leisure'   },
  SAVINGS:   { label: 'Savings'   },
}

function catLabel(name) {
  return CAT_META[name?.toUpperCase()]?.label ?? name
}

const inputClass =
  'w-full rounded-2xl border border-white/10 bg-slate-950/45 px-4 py-3 text-sm text-slate-100 outline-none transition placeholder:text-slate-500 focus:border-purple-400/80 focus:ring-2 focus:ring-purple-500/30'

export default function AddTransactionModal({ budget, onClose, onRefresh, showToast }) {
  const [amount, setAmount]   = useState('')
  const [desc,   setDesc]     = useState('')
  const [cat,    setCat]      = useState(budget?.categories?.[0]?.name ?? '')
  const [saving, setSaving]   = useState(false)
  const [error,  setError]    = useState('')

  const categories = budget?.categories ?? []

  useEffect(() => {
    const h = (e) => { if (e.key === 'Escape') onClose() }
    document.addEventListener('keydown', h)
    return () => document.removeEventListener('keydown', h)
  }, [onClose])

  const handleSubmit = async (e) => {
    e.preventDefault()
    const n = Number(amount)
    if (isNaN(n) || n <= 0) { setError('Amount must be greater than 0.'); return }
    if (!cat) { setError('Please select a category.'); return }
    setSaving(true)
    setError('')
    try {
      await budgetApi.logTransaction(budget.budgetId, cat, n, desc.trim() || undefined)
      showToast(`${fmtRon(n)} RON logged to ${catLabel(cat)}.`)
      onRefresh()
      onClose()
    } catch (err) {
      setError(err.response?.data?.message ?? 'Could not log transaction. Please try again.')
    } finally {
      setSaving(false)
    }
  }

  function fmtRon(v) {
    return Number(v ?? 0).toFixed(2)
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4" role="dialog" aria-modal="true">
      <div className="absolute inset-0 bg-slate-950/80 backdrop-blur-sm" onClick={!saving ? onClose : undefined} />

      <div className="relative z-10 w-full max-w-sm glass-card rounded-3xl shadow-2xl">
        {/* Header */}
        <div className="flex items-center justify-between px-6 pt-5 pb-4 border-b border-white/8">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-xl bg-purple-500/20 flex items-center justify-center">
              <Receipt size={16} className="text-purple-400" />
            </div>
            <h2 className="text-lg font-black text-slate-100">Add Transaction</h2>
          </div>
          <button
            onClick={onClose}
            disabled={saving}
            className="w-8 h-8 flex items-center justify-center rounded-xl text-slate-500 hover:text-slate-300 hover:bg-white/5 disabled:opacity-40"
          >
            <X size={16} />
          </button>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="px-6 py-5 space-y-4">
          {error && (
            <div className="flex items-start gap-2 rounded-2xl border border-rose-400/30 bg-rose-500/10 px-3 py-2.5 text-sm text-rose-200">
              <AlertCircle size={14} className="mt-0.5 flex-shrink-0" />
              {error}
            </div>
          )}

          {/* Amount */}
          <label className="block">
            <span className="mb-2 block text-sm font-black text-slate-300">Amount</span>
            <div className="relative">
              <input
                type="number"
                min="0.01"
                step="0.01"
                inputMode="decimal"
                value={amount}
                onChange={(e) => { setAmount(e.target.value); setError('') }}
                required
                disabled={saving}
                placeholder="0.00"
                className={`${inputClass} pr-14 text-lg font-black`}
              />
              <span className="absolute right-4 top-1/2 -translate-y-1/2 text-sm font-black text-slate-500">RON</span>
            </div>
          </label>

          {/* Category */}
          <label className="block">
            <span className="mb-2 block text-sm font-black text-slate-300">Category</span>
            <select
              value={cat}
              onChange={(e) => { setCat(e.target.value); setError('') }}
              required
              disabled={saving}
              className={`${inputClass} appearance-none`}
            >
              <option value="" disabled>Select a category…</option>
              {categories.map((c) => (
                <option key={c.id ?? c.name} value={c.name}>
                  {catLabel(c.name)}
                </option>
              ))}
            </select>
          </label>

          {/* Description */}
          <label className="block">
            <span className="mb-2 block text-sm font-black text-slate-300">Description <span className="font-normal text-slate-500">(optional)</span></span>
            <input
              value={desc}
              onChange={(e) => setDesc(e.target.value)}
              disabled={saving}
              placeholder="e.g. Kaufland — groceries"
              maxLength={200}
              className={inputClass}
            />
          </label>

          {/* Buttons */}
          <div className="flex gap-3 pt-1">
            <button
              type="button"
              onClick={onClose}
              disabled={saving}
              className="flex-1 rounded-2xl border border-slate-600 px-4 py-2.5 text-sm font-bold text-slate-300
                         hover:border-slate-500 hover:bg-white/5 disabled:opacity-40 disabled:cursor-not-allowed"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={saving}
              className="flex-1 flex items-center justify-center gap-2 rounded-2xl bg-purple-500 hover:bg-purple-400
                         px-4 py-2.5 text-sm font-bold text-white shadow-[0_0_20px_rgba(168,85,247,0.3)]
                         disabled:opacity-60 disabled:cursor-not-allowed"
            >
              {saving && <Loader2 size={15} className="animate-spin" />}
              {saving ? 'Logging…' : 'Log Spending'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
