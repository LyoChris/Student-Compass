import { useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { AlertCircle, ChevronLeft, ChevronRight, Loader2, Plus, ShoppingBag, Sparkles, X } from 'lucide-react'
import { marketplaceApi } from '../../api/marketplaceApi'
import { useAuth } from '../../hooks/useAuth'
import MarketplaceCard from './MarketplaceCard'
import MarketplaceFilters from './MarketplaceFilters'

const pageSize = 12

const defaultFilters = {
  search: '',
  category: '',
  condition: '',
  minPrice: '',
  maxPrice: '',
}

const categoryOptions = [
  { value: 'BOOKS_NOTES', label: 'Books & Notes' },
  { value: 'ELECTRONICS', label: 'Electronics' },
  { value: 'DORM_APPLIANCES', label: 'Dorm Appliances' },
  { value: 'CLOTHING', label: 'Clothing' },
  { value: 'OTHER', label: 'Other' },
]

const conditionOptions = [
  { value: 'NEW', label: 'New' },
  { value: 'LIKE_NEW', label: 'Like New' },
  { value: 'GOOD', label: 'Good' },
  { value: 'FAIR', label: 'Fair' },
]

const createInitial = {
  title: '',
  description: '',
  price: '',
  category: 'BOOKS_NOTES',
  itemCondition: 'GOOD',
  tags: '',
  imageUrls: '',
}

const fieldClass =
  'w-full rounded-2xl border border-white/10 bg-slate-950/60 px-4 py-3 text-sm text-slate-100 outline-none transition placeholder:text-slate-500 focus:border-purple-400/80 focus:ring-2 focus:ring-purple-500/30'

function SkeletonCard() {
  return (
    <div className="rounded-[1.75rem] border border-white/10 bg-slate-800/70 p-3">
      <div className="skeleton aspect-[4/3] rounded-[1.35rem]" />
      <div className="space-y-3 p-2 pt-4">
        <div className="skeleton h-4 w-2/3" />
        <div className="skeleton h-3 w-full" />
        <div className="skeleton h-3 w-4/5" />
        <div className="skeleton h-10 w-full rounded-2xl" />
      </div>
    </div>
  )
}

function EmptyState({ hasFilters, onClear }) {
  return (
    <div className="glass-card rounded-[2rem] p-10 text-center">
      <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-3xl bg-purple-500/15 text-purple-200">
        <ShoppingBag size={28} />
      </div>
      <h2 className="text-xl font-black text-slate-100">No listings found</h2>
      <p className="mx-auto mt-2 max-w-sm text-sm leading-6 text-slate-400">
        {hasFilters ? 'Try clearing filters or widening the price range.' : 'Marketplace listings will show up here as students publish them.'}
      </p>
      {hasFilters && (
        <button
          type="button"
          onClick={onClear}
          className="mt-5 rounded-2xl bg-purple-500 px-4 py-3 text-sm font-black text-white hover:bg-purple-400"
        >
          Clear Filters
        </button>
      )}
    </div>
  )
}

function CreateListingModal({ open, form, saving, error, onChange, onClose, onSubmit }) {
  if (!open) return null

  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center bg-slate-950/75 p-3 backdrop-blur-sm sm:items-center">
      <div className="glass-card max-h-[92vh] w-full max-w-2xl overflow-y-auto rounded-[2rem] p-5 shadow-2xl shadow-black/40 sm:p-6">
        <div className="mb-5 flex items-start justify-between gap-4">
          <div>
            <p className="text-xs font-black uppercase tracking-[0.2em] text-purple-200">Sell Item</p>
            <h2 className="mt-1 text-2xl font-black text-slate-100">Create a marketplace listing</h2>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="flex h-10 w-10 items-center justify-center rounded-2xl border border-white/10 text-slate-400 hover:bg-white/5 hover:text-slate-100"
            aria-label="Close listing form"
          >
            <X size={18} />
          </button>
        </div>

        {error && (
          <div className="mb-4 flex gap-3 rounded-2xl border border-red-400/30 bg-red-500/10 p-4 text-sm text-red-200">
            <AlertCircle size={18} className="mt-0.5 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={onSubmit} className="space-y-4">
          <label className="block">
            <span className="mb-2 block text-sm font-bold text-slate-300">Title</span>
            <input name="title" value={form.title} onChange={onChange} required maxLength={100} placeholder="Math 1 Course Notes" className={fieldClass} />
          </label>

          <label className="block">
            <span className="mb-2 block text-sm font-bold text-slate-300">Description</span>
            <textarea
              name="description"
              value={form.description}
              onChange={onChange}
              required
              rows={4}
              maxLength={2000}
              placeholder="Condition, pickup details, course context..."
              className={`${fieldClass} resize-none`}
            />
          </label>

          <div className="grid gap-4 sm:grid-cols-3">
            <label className="block">
              <span className="mb-2 block text-sm font-bold text-slate-300">Price</span>
              <input name="price" type="number" min="0.01" step="0.01" value={form.price} onChange={onChange} required placeholder="45" className={fieldClass} />
            </label>

            <label className="block">
              <span className="mb-2 block text-sm font-bold text-slate-300">Category</span>
              <select name="category" value={form.category} onChange={onChange} className={fieldClass}>
                {categoryOptions.map((option) => (
                  <option key={option.value} value={option.value} className="bg-slate-900">
                    {option.label}
                  </option>
                ))}
              </select>
            </label>

            <label className="block">
              <span className="mb-2 block text-sm font-bold text-slate-300">Condition</span>
              <select name="itemCondition" value={form.itemCondition} onChange={onChange} className={fieldClass}>
                {conditionOptions.map((option) => (
                  <option key={option.value} value={option.value} className="bg-slate-900">
                    {option.label}
                  </option>
                ))}
              </select>
            </label>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <label className="block">
              <span className="mb-2 block text-sm font-bold text-slate-300">Tags</span>
              <input name="tags" value={form.tags} onChange={onChange} placeholder="math, FII, year-1" className={fieldClass} />
            </label>

            <label className="block">
              <span className="mb-2 block text-sm font-bold text-slate-300">Image URLs</span>
              <input name="imageUrls" value={form.imageUrls} onChange={onChange} placeholder="https://..." className={fieldClass} />
            </label>
          </div>

          <div className="flex flex-col-reverse gap-3 pt-2 sm:flex-row">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 rounded-2xl border border-white/10 px-4 py-3 text-sm font-black text-slate-300 hover:bg-white/5"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={saving}
              className="flex flex-1 items-center justify-center gap-2 rounded-2xl bg-purple-500 px-4 py-3 text-sm font-black text-white shadow-[0_0_28px_rgba(168,85,247,0.35)] hover:bg-purple-400 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {saving && <Loader2 size={17} className="animate-spin" />}
              Publish Listing
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

function Pagination({ pageNumber, totalPages, onPageChange }) {
  if (totalPages <= 1) return null

  const pages = Array.from({ length: totalPages }, (_, index) => index).filter((page) => {
    return page === 0 || page === totalPages - 1 || Math.abs(page - pageNumber) <= 1
  })

  return (
    <nav className="glass-card flex flex-wrap items-center justify-center gap-2 rounded-[1.5rem] p-3">
      <button
        type="button"
        disabled={pageNumber === 0}
        onClick={() => onPageChange(pageNumber - 1)}
        className="flex items-center gap-1 rounded-2xl border border-white/10 px-3 py-2 text-sm font-black text-slate-300 hover:bg-white/5 disabled:cursor-not-allowed disabled:opacity-40"
      >
        <ChevronLeft size={16} />
        Previous
      </button>

      {pages.map((page, index) => {
        const previous = pages[index - 1]
        const showGap = previous !== undefined && page - previous > 1
        return (
          <div key={page} className="flex items-center gap-2">
            {showGap && <span className="text-slate-600">...</span>}
            <button
              type="button"
              onClick={() => onPageChange(page)}
              className={`h-10 min-w-10 rounded-2xl px-3 text-sm font-black ${
                page === pageNumber
                  ? 'bg-purple-500 text-white shadow-[0_0_18px_rgba(168,85,247,0.35)]'
                  : 'border border-white/10 text-slate-400 hover:bg-white/5 hover:text-slate-100'
              }`}
            >
              {page + 1}
            </button>
          </div>
        )
      })}

      <button
        type="button"
        disabled={pageNumber >= totalPages - 1}
        onClick={() => onPageChange(pageNumber + 1)}
        className="flex items-center gap-1 rounded-2xl border border-white/10 px-3 py-2 text-sm font-black text-slate-300 hover:bg-white/5 disabled:cursor-not-allowed disabled:opacity-40"
      >
        Next
        <ChevronRight size={16} />
      </button>
    </nav>
  )
}

function getFiltersFromParams(searchParams) {
  return {
    search: searchParams.get('search') || '',
    category: searchParams.get('category') || '',
    condition: searchParams.get('condition') || '',
    minPrice: searchParams.get('minPrice') || '',
    maxPrice: searchParams.get('maxPrice') || '',
  }
}

function hasActiveFilters(filters) {
  return Object.values(filters).some(Boolean)
}

export default function MarketplaceDashboard() {
  const { user } = useAuth()
  const [searchParams, setSearchParams] = useSearchParams()
  const [filters, setFilters] = useState(() => getFiltersFromParams(searchParams))
  const [items, setItems] = useState([])
  const [meta, setMeta] = useState({ pageNumber: Number(searchParams.get('page') || 0), totalPages: 0, totalElements: 0 })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [reloadKey, setReloadKey] = useState(0)
  const [createOpen, setCreateOpen] = useState(false)
  const [createForm, setCreateForm] = useState(createInitial)
  const [createSaving, setCreateSaving] = useState(false)
  const [createError, setCreateError] = useState('')

  const pageNumber = Number(searchParams.get('page') || 0)
  const activeFilters = useMemo(() => hasActiveFilters(filters), [filters])

  useEffect(() => {
    const nextParams = new URLSearchParams()

    Object.entries(filters).forEach(([key, value]) => {
      if (value !== '') nextParams.set(key, value)
    })

    nextParams.set('page', String(pageNumber))
    nextParams.set('size', String(pageSize))
    setSearchParams(nextParams, { replace: true })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filters, pageNumber, reloadKey])

  useEffect(() => {
    let ignore = false

    const loadItems = async () => {
      setLoading(true)
      setError('')

      try {
        const params = {
          page: pageNumber,
          size: pageSize,
          ...(filters.search && { search: filters.search }),
          ...(filters.category && { category: filters.category }),
          ...(filters.condition && { condition: filters.condition }),
          ...(filters.minPrice && { minPrice: Number(filters.minPrice) }),
          ...(filters.maxPrice && { maxPrice: Number(filters.maxPrice) }),
        }

        const { data } = await marketplaceApi.search(params)
        if (ignore) return

        setItems(data.content || [])
        setMeta({
          pageNumber: data.pageNumber ?? pageNumber,
          totalPages: data.totalPages ?? 0,
          totalElements: data.totalElements ?? 0,
        })
      } catch (err) {
        if (ignore) return
        setItems([])
        setMeta({ pageNumber, totalPages: 0, totalElements: 0 })
        setError(err.response?.data?.message || 'Could not load marketplace listings.')
      } finally {
        if (!ignore) setLoading(false)
      }
    }

    loadItems()

    return () => {
      ignore = true
    }
  }, [filters, pageNumber])

  const updateFilters = (nextFilters) => {
    setFilters({
      ...nextFilters,
      minPrice: Number(nextFilters.minPrice) < 0 ? '0' : nextFilters.minPrice,
      maxPrice: Number(nextFilters.maxPrice) < 0 ? '0' : nextFilters.maxPrice,
    })

    const nextParams = new URLSearchParams(searchParams)
    nextParams.set('page', '0')
    setSearchParams(nextParams, { replace: true })
  }

  const clearFilters = () => {
    setFilters(defaultFilters)
    setSearchParams({ page: '0', size: String(pageSize) }, { replace: true })
  }

  const changePage = (page) => {
    const nextParams = new URLSearchParams(searchParams)
    nextParams.set('page', String(Math.max(0, page)))
    nextParams.set('size', String(pageSize))
    setSearchParams(nextParams)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const updateCreateForm = (event) => {
    const { name, value } = event.target
    setCreateForm((current) => ({
      ...current,
      [name]: name === 'price' && Number(value) < 0 ? '0' : value,
    }))
    if (createError) setCreateError('')
  }

  const closeCreateModal = () => {
    if (createSaving) return
    setCreateOpen(false)
    setCreateError('')
  }

  const submitCreateListing = async (event) => {
    event.preventDefault()

    if (!user?.id) {
      setCreateError('Nu am gasit userId-ul tau. Te rog autentifica-te din nou.')
      return
    }

    setCreateSaving(true)
    setCreateError('')

    try {
      await marketplaceApi.create({
        sellerId: user.id,
        title: createForm.title.trim(),
        description: createForm.description.trim(),
        price: Number(Number(createForm.price).toFixed(2)),
        category: createForm.category,
        itemCondition: createForm.itemCondition,
        tags: createForm.tags.split(',').map((tag) => tag.trim()).filter(Boolean),
        imageUrls: createForm.imageUrls.split(',').map((url) => url.trim()).filter(Boolean),
      })

      setCreateForm(createInitial)
      setCreateOpen(false)
      clearFilters()
      setReloadKey((current) => current + 1)
    } catch (err) {
      setCreateError(err.response?.data?.message || 'Nu am putut publica listarea. Verifica datele si incearca din nou.')
    } finally {
      setCreateSaving(false)
    }
  }

  return (
    <div className="min-h-screen bg-[#0F172A] text-slate-100">
      <div className="pointer-events-none fixed inset-0">
        <div className="absolute right-[-14rem] top-[-16rem] h-[36rem] w-[36rem] rounded-full bg-purple-500/20 blur-3xl" />
        <div className="absolute bottom-[-18rem] left-[-12rem] h-[32rem] w-[32rem] rounded-full bg-cyan-500/10 blur-3xl" />
      </div>

      <div className="relative mx-auto max-w-7xl px-4 pb-28 pt-6 md:pb-10 lg:px-6">
        <header className="mb-6 flex flex-wrap items-center justify-between gap-4">
          <div>
            <div className="mb-3 inline-flex items-center gap-2 rounded-full border border-purple-400/20 bg-purple-500/10 px-3 py-1.5 text-xs font-black uppercase tracking-[0.2em] text-purple-200">
              <Sparkles size={13} />
              Student Marketplace
            </div>
            <h1 className="text-3xl font-black tracking-tight sm:text-5xl">Buy smarter on campus</h1>
            <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-400">
              Textbooks, dorm gear, electronics, clothing, and student deals surfaced in a fast bento marketplace.
            </p>
          </div>
          <button
            type="button"
            onClick={() => setCreateOpen(true)}
            className="flex items-center gap-2 rounded-2xl bg-purple-500 px-4 py-3 text-sm font-black text-white shadow-[0_0_30px_rgba(168,85,247,0.35)] hover:bg-purple-400"
          >
            <Plus size={17} />
            Sell Item
          </button>
        </header>

        <div className="grid gap-5 lg:grid-cols-[21rem_1fr]">
          <MarketplaceFilters filters={filters} onChange={updateFilters} onClear={clearFilters} />

          <main className="space-y-5">
            <div className="glass-card flex flex-wrap items-center justify-between gap-3 rounded-[1.5rem] px-4 py-3">
              <p className="text-sm font-bold text-slate-300">
                {loading ? 'Searching campus listings...' : `${meta.totalElements} listing${meta.totalElements === 1 ? '' : 's'} found`}
              </p>
              <p className="text-xs font-bold uppercase tracking-[0.18em] text-slate-500">
                Page {meta.totalPages ? meta.pageNumber + 1 : 0} / {meta.totalPages}
              </p>
            </div>

            {error && (
              <div className="flex gap-3 rounded-2xl border border-red-400/30 bg-red-500/10 p-4 text-sm text-red-200">
                <AlertCircle size={18} className="mt-0.5 shrink-0" />
                <span>{error}</span>
              </div>
            )}

            {loading ? (
              <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
                {Array.from({ length: 6 }, (_, index) => (
                  <SkeletonCard key={index} />
                ))}
              </div>
            ) : items.length > 0 ? (
              <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
                {items.map((item) => (
                  <MarketplaceCard key={item.id} item={item} />
                ))}
              </div>
            ) : (
              <EmptyState hasFilters={activeFilters} onClear={clearFilters} />
            )}

            <Pagination pageNumber={meta.pageNumber} totalPages={meta.totalPages} onPageChange={changePage} />
          </main>
        </div>
      </div>

      <CreateListingModal
        open={createOpen}
        form={createForm}
        saving={createSaving}
        error={createError}
        onChange={updateCreateForm}
        onClose={closeCreateModal}
        onSubmit={submitCreateListing}
      />
    </div>
  )
}
