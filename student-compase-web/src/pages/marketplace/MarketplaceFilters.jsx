import { useState } from 'react'
import { RotateCcw, Search, SlidersHorizontal, X } from 'lucide-react'

const categories = [
  { value: '', label: 'All' },
  { value: 'BOOKS_NOTES', label: 'Books' },
  { value: 'ELECTRONICS', label: 'Tech' },
  { value: 'DORM_APPLIANCES', label: 'Dorm' },
  { value: 'CLOTHING', label: 'Clothing' },
  { value: 'OTHER', label: 'Other' },
]

const conditions = [
  { value: '', label: 'Any' },
  { value: 'NEW', label: 'New' },
  { value: 'LIKE_NEW', label: 'Like New' },
  { value: 'GOOD', label: 'Good' },
  { value: 'FAIR', label: 'Fair' },
]

const inputClass =
  'w-full rounded-2xl border border-white/10 bg-slate-950/45 px-4 py-3 text-sm text-slate-100 outline-none transition placeholder:text-slate-500 focus:border-purple-400/80 focus:ring-2 focus:ring-purple-500/30'

function PillGroup({ label, options, value, onChange }) {
  return (
    <div>
      <p className="mb-2 text-xs font-black uppercase tracking-[0.18em] text-slate-500">{label}</p>
      <div className="flex flex-wrap gap-2">
        {options.map((option) => {
          const active = value === option.value
          return (
            <button
              key={option.value || option.label}
              type="button"
              onClick={() => onChange(option.value)}
              className={`rounded-full border px-4 py-2 text-xs font-black transition ${
                active
                  ? 'border-purple-300 bg-purple-500 text-white shadow-[0_0_18px_rgba(168,85,247,0.35)]'
                  : 'border-white/10 bg-white/5 text-slate-400 hover:border-purple-300/40 hover:text-slate-100'
              }`}
            >
              {option.label}
            </button>
          )
        })}
      </div>
    </div>
  )
}

function FilterFields({ filters, onChange }) {
  const setField = (field, value) => {
    onChange({
      ...filters,
      [field]: field === 'minPrice' || field === 'maxPrice'
        ? Number(value) < 0 ? '0' : value
        : value,
    })
  }

  return (
    <div className="space-y-5">
      <label className="block">
        <span className="mb-2 block text-xs font-black uppercase tracking-[0.18em] text-slate-500">Search</span>
        <div className="relative">
          <Search size={17} className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500" />
          <input
            value={filters.search}
            onChange={(event) => setField('search', event.target.value)}
            placeholder="Textbooks, calculators, hoodies..."
            className={`${inputClass} pl-11`}
          />
        </div>
      </label>

      <PillGroup label="Category" options={categories} value={filters.category} onChange={(value) => setField('category', value)} />
      <PillGroup label="Condition" options={conditions} value={filters.condition} onChange={(value) => setField('condition', value)} />

      <div>
        <p className="mb-2 text-xs font-black uppercase tracking-[0.18em] text-slate-500">Price Range</p>
        <div className="grid grid-cols-2 gap-2">
          <input
            type="number"
            min="0"
            inputMode="decimal"
            value={filters.minPrice}
            onChange={(event) => setField('minPrice', event.target.value)}
            placeholder="Min RON"
            className={inputClass}
          />
          <input
            type="number"
            min="0"
            inputMode="decimal"
            value={filters.maxPrice}
            onChange={(event) => setField('maxPrice', event.target.value)}
            placeholder="Max RON"
            className={inputClass}
          />
        </div>
      </div>
    </div>
  )
}

export default function MarketplaceFilters({ filters, onChange, onClear, activeCount = 0 }) {
  const [open, setOpen] = useState(false)
  const [draft, setDraft] = useState(filters)

  const applyMobileFilters = () => {
    onChange(draft)
    setOpen(false)
  }

  const clearMobileFilters = () => {
    onClear()
    setOpen(false)
  }

  return (
    <>
      <div className="lg:hidden">
        <button
          type="button"
          onClick={() => {
            setDraft(filters)
            setOpen(true)
          }}
          className="glass-card flex w-full items-center justify-between rounded-[1.5rem] px-4 py-3 text-left"
        >
          <span className="flex items-center gap-3">
            <span className="flex h-10 w-10 items-center justify-center rounded-2xl bg-purple-500/15 text-purple-200">
              <SlidersHorizontal size={18} />
            </span>
            <span>
              <span className="block text-sm font-black text-slate-100">Filters & Sort</span>
              <span className="block text-xs text-slate-500">
                {activeCount ? `${activeCount} active filter${activeCount === 1 ? '' : 's'}` : 'Search, category, condition, price'}
              </span>
            </span>
          </span>
          <span className="rounded-full bg-purple-500 px-3 py-1.5 text-xs font-black text-white">Open</span>
        </button>
      </div>

      <aside className="glass-card hidden rounded-[2rem] p-5 lg:sticky lg:top-5 lg:block">
        <div className="mb-5 flex items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-purple-500/15 text-purple-200">
              <SlidersHorizontal size={18} />
            </div>
            <div>
              <p className="text-sm font-black text-slate-100">Filters</p>
              <p className="text-xs text-slate-500">Find the good stuff fast</p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClear}
            className="flex items-center gap-1.5 rounded-full border border-white/10 px-3 py-2 text-xs font-black text-slate-400 hover:border-purple-300/30 hover:text-purple-200"
          >
            <RotateCcw size={13} />
            Clear
          </button>
        </div>

        <FilterFields filters={filters} onChange={onChange} />
      </aside>

      {open && (
        <div className="fixed inset-0 z-50 bg-slate-950/75 backdrop-blur-sm lg:hidden">
          <div className="absolute inset-x-0 bottom-0 max-h-[88vh] overflow-y-auto rounded-t-[2rem] border border-white/10 bg-slate-900 p-4 shadow-2xl shadow-black/50">
            <div className="mx-auto mb-4 h-1.5 w-12 rounded-full bg-white/20" />
            <div className="mb-5 flex items-start justify-between gap-4">
              <div>
                <p className="text-xs font-black uppercase tracking-[0.2em] text-purple-200">Marketplace</p>
                <h2 className="mt-1 text-2xl font-black text-slate-100">Filters & Sort</h2>
              </div>
              <button
                type="button"
                onClick={() => setOpen(false)}
                className="flex h-10 w-10 items-center justify-center rounded-2xl border border-white/10 text-slate-400 hover:bg-white/5 hover:text-slate-100"
                aria-label="Close filters"
              >
                <X size={18} />
              </button>
            </div>

            <FilterFields filters={draft} onChange={setDraft} />

            <div className="sticky bottom-0 -mx-4 mt-6 grid grid-cols-[0.8fr_1.2fr] gap-3 border-t border-white/10 bg-slate-900/95 p-4 backdrop-blur">
              <button
                type="button"
                onClick={clearMobileFilters}
                className="rounded-2xl border border-white/10 px-4 py-3 text-sm font-black text-slate-300 hover:bg-white/5"
              >
                Clear
              </button>
              <button
                type="button"
                onClick={applyMobileFilters}
                className="rounded-2xl bg-purple-500 px-4 py-3 text-sm font-black text-white shadow-[0_0_24px_rgba(168,85,247,0.38)] hover:bg-purple-400"
              >
                Apply Filters
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}
