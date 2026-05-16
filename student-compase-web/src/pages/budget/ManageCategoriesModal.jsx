import { useState, useEffect } from 'react'
import { X, Trash2, Plus, Loader2, AlertCircle, Check } from 'lucide-react'
import { budgetApi } from '../../api/budgetApi'

const CAT_META = {
  FOOD:      { label: 'Food',      color: '#A855F7' },
  TRANSPORT: { label: 'Transport', color: '#22D3EE' },
  HOUSING:   { label: 'Housing',   color: '#F59E0B' },
  SUPPLIES:  { label: 'Supplies',  color: '#34D399' },
  PERSONAL:  { label: 'Personal',  color: '#F472B6' },
  LEISURE:   { label: 'Leisure',   color: '#60A5FA' },
  SAVINGS:   { label: 'Savings',   color: '#4ADE80' },
}

function catLabel(name) {
  return CAT_META[name?.toUpperCase()]?.label ?? name
}
function catColor(name) {
  return CAT_META[name?.toUpperCase()]?.color ?? '#A855F7'
}

const inputClass =
  'w-full rounded-2xl border border-white/10 bg-slate-950/45 px-4 py-2.5 text-sm text-slate-100 outline-none transition placeholder:text-slate-500 focus:border-purple-400/80 focus:ring-2 focus:ring-purple-500/30'

export default function ManageCategoriesModal({ budget, onClose, onRefresh, showToast }) {
  const [categories, setCategories] = useState(budget?.categories ?? [])
  const [deletingId, setDeletingId] = useState(null)

  const [newName,   setNewName]   = useState('')
  const [newAmount, setNewAmount] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError]   = useState('')

  // Keep local list in sync if budget prop changes
  useEffect(() => { setCategories(budget?.categories ?? []) }, [budget])

  // Escape key
  useEffect(() => {
    const h = (e) => { if (e.key === 'Escape') onClose() }
    document.addEventListener('keydown', h)
    return () => document.removeEventListener('keydown', h)
  }, [onClose])

  const handleDelete = async (cat) => {
    setDeletingId(cat.id ?? cat.name)
    try {
      await budgetApi.deleteCategory(budget.budgetId, cat.name)
      setCategories((prev) => prev.filter((c) => (c.id ?? c.name) !== (cat.id ?? cat.name)))
      showToast(`"${catLabel(cat.name)}" category removed.`)
      onRefresh()
    } catch (err) {
      showToast(err.response?.data?.message ?? 'Could not delete category.', 'error')
    } finally {
      setDeletingId(null)
    }
  }

  const handleSave = async (e) => {
    e.preventDefault()
    const name = newName.trim()
    const amount = Number(newAmount)
    if (!name) { setError('Category name is required.'); return }
    if (isNaN(amount) || amount < 0) { setError('Amount must be 0 or greater.'); return }
    setSaving(true)
    setError('')
    try {
      await budgetApi.adjustCategory(budget.budgetId, name, amount)
      showToast(`"${catLabel(name) !== name ? catLabel(name) : name}" ${categories.find((c) => c.name.toUpperCase() === name.toUpperCase()) ? 'updated' : 'added'}.`)
      setNewName('')
      setNewAmount('')
      onRefresh()
      // Optimistic local refresh — full data via onRefresh
    } catch (err) {
      setError(err.response?.data?.message ?? 'Could not save category.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4" role="dialog" aria-modal="true">
      <div className="absolute inset-0 bg-slate-950/80 backdrop-blur-sm" onClick={onClose} />

      <div className="relative z-10 w-full max-w-md glass-card rounded-3xl shadow-2xl flex flex-col max-h-[88vh]">
        {/* Header */}
        <div className="flex items-center justify-between px-6 pt-5 pb-4 border-b border-white/8 flex-shrink-0">
          <h2 className="text-lg font-black text-slate-100">Manage Categories</h2>
          <button onClick={onClose} className="w-8 h-8 flex items-center justify-center rounded-xl text-slate-500 hover:text-slate-300 hover:bg-white/5">
            <X size={16} />
          </button>
        </div>

        {/* Existing categories list */}
        <div className="overflow-y-auto flex-1 px-6 py-4 space-y-2">
          {categories.length === 0 && (
            <p className="text-slate-500 text-sm text-center py-4">No categories yet.</p>
          )}
          {categories.map((cat) => {
            const isDel = (deletingId === (cat.id ?? cat.name))
            return (
              <div
                key={cat.id ?? cat.name}
                className="flex items-center justify-between gap-3 glass-card rounded-2xl px-4 py-3"
              >
                <div className="flex items-center gap-2.5 min-w-0">
                  <span className="w-2.5 h-2.5 rounded-full flex-shrink-0" style={{ background: catColor(cat.name) }} />
                  <span className="text-sm font-bold text-slate-100 truncate">{catLabel(cat.name)}</span>
                </div>
                <div className="flex items-center gap-3 flex-shrink-0">
                  <span className="text-xs font-semibold text-slate-400">{Number(cat.allocatedAmount ?? 0).toFixed(2)} RON</span>
                  <button
                    onClick={() => handleDelete(cat)}
                    disabled={isDel}
                    className="w-7 h-7 flex items-center justify-center rounded-xl text-slate-500 hover:text-rose-400 hover:bg-rose-500/10 disabled:opacity-40"
                    title={`Remove ${catLabel(cat.name)}`}
                  >
                    {isDel ? <Loader2 size={14} className="animate-spin" /> : <Trash2 size={14} />}
                  </button>
                </div>
              </div>
            )
          })}
        </div>

        {/* Add / Edit form */}
        <div className="border-t border-white/8 px-6 py-4 flex-shrink-0">
          <p className="text-xs font-black text-slate-400 uppercase tracking-wider mb-3">Add or Update Category</p>
          {error && (
            <div className="flex items-center gap-2 mb-3 rounded-xl border border-rose-400/30 bg-rose-500/10 px-3 py-2 text-xs text-rose-200">
              <AlertCircle size={13} className="flex-shrink-0" />
              {error}
            </div>
          )}
          <form onSubmit={handleSave} className="space-y-2">
            <input
              value={newName}
              onChange={(e) => { setNewName(e.target.value); setError('') }}
              placeholder="Category name (e.g. GYM, FOOD)"
              disabled={saving}
              className={inputClass}
            />
            <div className="flex gap-2">
              <div className="relative flex-1">
                <input
                  type="number"
                  min="0"
                  step="0.01"
                  inputMode="decimal"
                  value={newAmount}
                  onChange={(e) => { setNewAmount(e.target.value); setError('') }}
                  placeholder="Allocation"
                  disabled={saving}
                  className={`${inputClass} pr-12`}
                />
                <span className="absolute right-4 top-1/2 -translate-y-1/2 text-xs font-bold text-slate-500">RON</span>
              </div>
              <button
                type="submit"
                disabled={saving}
                className="flex items-center gap-1.5 px-4 py-2.5 rounded-2xl bg-purple-500 hover:bg-purple-400 text-sm font-bold text-white
                           disabled:opacity-60 disabled:cursor-not-allowed flex-shrink-0"
              >
                {saving ? <Loader2 size={15} className="animate-spin" /> : <Check size={15} />}
                {saving ? 'Saving…' : 'Save'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  )
}
