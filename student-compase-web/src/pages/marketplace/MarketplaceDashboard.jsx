import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { AlertCircle, ChevronLeft, ChevronRight, Plus, ShoppingBag, Sparkles } from 'lucide-react'
import { marketplaceApi } from '../../api/marketplaceApi'
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

function countActiveFilters(filters) {
  return Object.values(filters).filter(Boolean).length
}

export default function MarketplaceDashboard() {
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const [filters, setFilters] = useState(() => getFiltersFromParams(searchParams))
  const [items, setItems] = useState([])
  const [meta, setMeta] = useState({ pageNumber: Number(searchParams.get('page') || 0), totalPages: 0, totalElements: 0 })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const pageNumber = Number(searchParams.get('page') || 0)
  const activeFilters = useMemo(() => hasActiveFilters(filters), [filters])
  const activeFilterCount = useMemo(() => countActiveFilters(filters), [filters])

  useEffect(() => {
    const nextParams = new URLSearchParams()

    Object.entries(filters).forEach(([key, value]) => {
      if (value !== '') nextParams.set(key, value)
    })

    nextParams.set('page', String(pageNumber))
    nextParams.set('size', String(pageSize))
    setSearchParams(nextParams, { replace: true })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filters, pageNumber])

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
    const normalizedFilters = {
      ...nextFilters,
      minPrice: Number(nextFilters.minPrice) < 0 ? '0' : nextFilters.minPrice,
      maxPrice: Number(nextFilters.maxPrice) < 0 ? '0' : nextFilters.maxPrice,
    }

    setFilters(normalizedFilters)

    const nextParams = new URLSearchParams()
    Object.entries(normalizedFilters).forEach(([key, value]) => {
      if (value !== '') nextParams.set(key, value)
    })
    nextParams.set('page', '0')
    nextParams.set('size', String(pageSize))
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

  return (
    <div className="min-h-screen bg-[#0F172A] text-slate-100">
      <div className="pointer-events-none fixed inset-0">
        <div className="absolute right-[-14rem] top-[-16rem] h-[36rem] w-[36rem] rounded-full bg-purple-500/20 blur-3xl" />
        <div className="absolute bottom-[-18rem] left-[-12rem] h-[32rem] w-[32rem] rounded-full bg-cyan-500/10 blur-3xl" />
      </div>

      <div className="relative mx-auto max-w-7xl px-4 pb-32 pt-6 md:pb-24 lg:px-6">
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
            onClick={() => navigate('/marketplace/sell')}
            className="flex items-center gap-2 rounded-2xl bg-purple-500 px-4 py-3 text-sm font-black text-white shadow-[0_0_30px_rgba(168,85,247,0.35)] hover:bg-purple-400"
          >
            <Plus size={17} />
            Sell Item
          </button>
        </header>

        <div className="grid gap-5 lg:grid-cols-[21rem_1fr]">
          <MarketplaceFilters filters={filters} onChange={updateFilters} onClear={clearFilters} activeCount={activeFilterCount} />

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
              <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3 2xl:grid-cols-4">
                {Array.from({ length: 6 }, (_, index) => (
                  <SkeletonCard key={index} />
                ))}
              </div>
            ) : items.length > 0 ? (
              <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3 2xl:grid-cols-4">
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

    </div>
  )
}
