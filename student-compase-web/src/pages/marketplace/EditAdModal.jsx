import { useState, useEffect } from 'react'
import { AlertCircle, Loader2, X } from 'lucide-react'
import { marketplaceApi } from '../../api/marketplaceApi'

const CATEGORIES = [
  { value: 'BOOKS_NOTES',     label: 'Books'    },
  { value: 'ELECTRONICS',     label: 'Tech'     },
  { value: 'DORM_APPLIANCES', label: 'Dorm'     },
  { value: 'CLOTHING',        label: 'Clothing' },
  { value: 'OTHER',           label: 'Other'    },
]

const CONDITIONS = [
  { value: 'NEW',      label: 'New'      },
  { value: 'LIKE_NEW', label: 'Like New' },
  { value: 'GOOD',     label: 'Good'     },
  { value: 'FAIR',     label: 'Fair'     },
]

const inputClass =
  'w-full rounded-2xl border border-white/10 bg-slate-950/45 px-4 py-3 text-sm text-slate-100 outline-none transition placeholder:text-slate-500 focus:border-purple-400/80 focus:ring-2 focus:ring-purple-500/30'

function PillSelector({ options, value, onChange, disabled }) {
  return (
    <div className="flex flex-wrap gap-2">
      {options.map((opt) => {
        const active = opt.value === value
        return (
          <button
            key={opt.value}
            type="button"
            disabled={disabled}
            onClick={() => onChange(opt.value)}
            className={`rounded-full border px-3.5 py-1.5 text-xs font-black uppercase tracking-wide transition
              disabled:cursor-not-allowed disabled:opacity-50 ${
              active
                ? 'border-purple-300 bg-purple-500 text-white shadow-[0_0_16px_rgba(168,85,247,0.35)]'
                : 'border-white/10 bg-white/5 text-slate-400 hover:border-purple-300/40 hover:text-slate-100'
            }`}
          >
            {opt.label}
          </button>
        )
      })}
    </div>
  )
}

export default function EditAdModal({ item, onSuccess, onCancel }) {
  const [form, setForm] = useState({
    title:         item?.title         ?? '',
    description:   item?.description   ?? '',
    price:         item?.price         ?? '',
    category:      item?.category      ?? 'BOOKS_NOTES',
    itemCondition: item?.itemCondition ?? 'GOOD',
  })
  const [saving, setSaving] = useState(false)
  const [error, setError]   = useState('')

  // Sync if the item prop changes (e.g. modal reused)
  useEffect(() => {
    if (item) {
      setForm({
        title:         item.title         ?? '',
        description:   item.description   ?? '',
        price:         item.price         ?? '',
        category:      item.category      ?? 'BOOKS_NOTES',
        itemCondition: item.itemCondition ?? 'GOOD',
      })
      setError('')
    }
  }, [item?.id])  // eslint-disable-line react-hooks/exhaustive-deps

  // Close on Escape
  useEffect(() => {
    const handle = (e) => { if (e.key === 'Escape' && !saving) onCancel() }
    document.addEventListener('keydown', handle)
    return () => document.removeEventListener('keydown', handle)
  }, [saving, onCancel])

  if (!item) return null

  const update = (e) => {
    const { name, value } = e.target
    setForm((prev) => ({ ...prev, [name]: name === 'price' && Number(value) < 0 ? '0' : value }))
    if (error) setError('')
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (Number(form.price) <= 0) { setError('Price must be greater than 0.'); return }

    setSaving(true)
    setError('')
    try {
      const { data } = await marketplaceApi.updateItem(item.id, {
        title:         form.title.trim(),
        description:   form.description.trim(),
        price:         Number(Number(form.price).toFixed(2)),
        category:      form.category,
        itemCondition: form.itemCondition,
      })
      onSuccess(data)
    } catch (err) {
      setError(err.response?.data?.message ?? 'Could not save changes. Please try again.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4" role="dialog" aria-modal="true">
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-slate-950/80 backdrop-blur-sm"
        onClick={!saving ? onCancel : undefined}
      />

      {/* Panel */}
      <div className="relative z-10 w-full max-w-lg glass-card rounded-3xl shadow-2xl overflow-hidden flex flex-col max-h-[90vh]">

        {/* Header */}
        <div className="flex items-center justify-between px-6 pt-5 pb-4 border-b border-white/8 flex-shrink-0">
          <h2 className="text-lg font-black text-slate-100">Edit Listing</h2>
          <button
            onClick={onCancel}
            disabled={saving}
            className="w-8 h-8 flex items-center justify-center rounded-xl text-slate-500 hover:text-slate-300 hover:bg-white/5 disabled:opacity-40"
            aria-label="Close"
          >
            <X size={16} />
          </button>
        </div>

        {/* Body — scrollable */}
        <div className="overflow-y-auto flex-1 px-6 py-5">
          {error && (
            <div className="flex items-start gap-3 mb-4 rounded-2xl border border-rose-400/30 bg-rose-500/10 p-3 text-sm text-rose-200">
              <AlertCircle size={16} className="mt-0.5 flex-shrink-0" />
              {error}
            </div>
          )}

          <form id="edit-ad-form" onSubmit={handleSubmit} className="space-y-5">
            {/* Title + Price */}
            <div className="grid gap-4 sm:grid-cols-[1fr_9rem]">
              <label className="block">
                <span className="mb-2 block text-sm font-black text-slate-300">Title</span>
                <input
                  name="title"
                  value={form.title}
                  onChange={update}
                  required
                  maxLength={100}
                  disabled={saving}
                  placeholder="Item title"
                  className={inputClass}
                />
              </label>
              <label className="block">
                <span className="mb-2 block text-sm font-black text-slate-300">Price</span>
                <div className="relative">
                  <input
                    name="price"
                    type="number"
                    min="0.01"
                    step="0.01"
                    inputMode="decimal"
                    value={form.price}
                    onChange={update}
                    required
                    disabled={saving}
                    placeholder="0"
                    className={`${inputClass} pr-12`}
                  />
                  <span className="absolute right-4 top-1/2 -translate-y-1/2 text-xs font-black text-slate-500">RON</span>
                </div>
              </label>
            </div>

            {/* Description */}
            <label className="block">
              <span className="mb-2 block text-sm font-black text-slate-300">Description</span>
              <textarea
                name="description"
                value={form.description}
                onChange={update}
                required
                rows={4}
                maxLength={2000}
                disabled={saving}
                placeholder="Describe the item…"
                className={`${inputClass} resize-none`}
              />
            </label>

            {/* Category */}
            <div>
              <p className="mb-2 text-sm font-black text-slate-300">Category</p>
              <PillSelector options={CATEGORIES} value={form.category} onChange={(v) => setForm((p) => ({ ...p, category: v }))} disabled={saving} />
            </div>

            {/* Condition */}
            <div>
              <p className="mb-2 text-sm font-black text-slate-300">Condition</p>
              <PillSelector options={CONDITIONS} value={form.itemCondition} onChange={(v) => setForm((p) => ({ ...p, itemCondition: v }))} disabled={saving} />
            </div>
          </form>
        </div>

        {/* Footer */}
        <div className="flex gap-3 px-6 py-4 border-t border-white/8 flex-shrink-0">
          <button
            type="button"
            onClick={onCancel}
            disabled={saving}
            className="flex-1 rounded-2xl border border-slate-600 px-4 py-2.5 text-sm font-bold text-slate-300
                       hover:border-slate-500 hover:bg-white/5 disabled:opacity-40 disabled:cursor-not-allowed"
          >
            Cancel
          </button>
          <button
            type="submit"
            form="edit-ad-form"
            disabled={saving}
            className="flex-1 flex items-center justify-center gap-2 rounded-2xl bg-purple-500 hover:bg-purple-400
                       px-4 py-2.5 text-sm font-bold text-white shadow-[0_0_20px_rgba(168,85,247,0.3)]
                       disabled:opacity-60 disabled:cursor-not-allowed"
          >
            {saving && <Loader2 size={15} className="animate-spin" />}
            {saving ? 'Saving…' : 'Save Changes'}
          </button>
        </div>
      </div>
    </div>
  )
}
