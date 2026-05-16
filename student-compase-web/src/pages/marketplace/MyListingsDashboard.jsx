import { useState, useEffect, useCallback } from 'react'
import {
  ImageIcon, Pencil, Tag, Trash2, HandshakeIcon,
  AlertCircle, Plus, PackageOpen, CheckCircle2, X,
  Loader2, RotateCcw,
} from 'lucide-react'
import { Link } from 'react-router-dom'
import AppShell from '../../components/layout/AppShell'
import { marketplaceApi } from '../../api/marketplaceApi'
import DeleteConfirmationModal from './DeleteConfirmationModal'
import EditAdModal from './EditAdModal'

// ─── Status badge config ──────────────────────────────────────────────────────
const STATUS_META = {
  ACTIVE:   { label: 'Active',   classes: 'bg-emerald-500/15 text-emerald-300 border-emerald-400/25' },
  RESERVED: { label: 'Reserved', classes: 'bg-amber-500/15  text-amber-300  border-amber-400/25'    },
  SOLD:     { label: 'Sold',     classes: 'bg-slate-600/50   text-slate-400  border-slate-500/30'    },
  INACTIVE: { label: 'Inactive', classes: 'bg-slate-600/50   text-slate-400  border-slate-500/30'    },
}

const CATEGORY_LABELS = {
  BOOKS_NOTES:     'Books & Notes',
  ELECTRONICS:     'Electronics',
  DORM_APPLIANCES: 'Dorm Gear',
  CLOTHING:        'Clothing',
  OTHER:           'Other',
}

function formatPrice(n) {
  return new Intl.NumberFormat('ro-RO', { style: 'currency', currency: 'RON', maximumFractionDigits: 0 }).format(Number(n ?? 0))
}

// ─── Skeleton card ────────────────────────────────────────────────────────────
function SkeletonCard() {
  return (
    <div className="glass-card rounded-[1.75rem] overflow-hidden">
      <div className="skeleton aspect-[4/3] w-full" />
      <div className="p-4 space-y-3">
        <div className="flex gap-2">
          <div className="skeleton h-5 w-16 rounded-full" />
          <div className="skeleton h-5 w-20 rounded-full" />
        </div>
        <div className="skeleton h-5 w-3/4 rounded-xl" />
        <div className="skeleton h-4 w-full rounded-xl" />
        <div className="skeleton h-4 w-2/3 rounded-xl" />
        <div className="flex gap-2 pt-1">
          <div className="skeleton h-9 flex-1 rounded-2xl" />
          <div className="skeleton h-9 flex-1 rounded-2xl" />
          <div className="skeleton h-9 w-9 rounded-2xl" />
        </div>
      </div>
    </div>
  )
}

// ─── Toast ────────────────────────────────────────────────────────────────────
function Toast({ toast, onClose }) {
  useEffect(() => {
    if (!toast) return
    const t = setTimeout(onClose, 3500)
    return () => clearTimeout(t)
  }, [toast, onClose])

  if (!toast) return null

  const isError = toast.type === 'error'
  return (
    <div className={`fixed bottom-24 md:bottom-8 left-1/2 -translate-x-1/2 z-50 flex items-center gap-3
        glass-card rounded-2xl px-4 py-3 shadow-xl max-w-xs w-[calc(100%-2rem)]
        border ${isError ? 'border-rose-500/30' : 'border-emerald-500/30'}`}
    >
      {isError
        ? <AlertCircle size={16} className="text-rose-400 flex-shrink-0" />
        : <CheckCircle2 size={16} className="text-emerald-400 flex-shrink-0" />
      }
      <p className={`text-sm flex-1 ${isError ? 'text-rose-300' : 'text-emerald-300'}`}>{toast.message}</p>
      <button onClick={onClose} className="text-slate-500 hover:text-slate-300 flex-shrink-0">
        <X size={13} />
      </button>
    </div>
  )
}

// ─── Single management card ───────────────────────────────────────────────────
function ManagementCard({ item, onEdit, onDelete, onMarkSold }) {
  const imageUrl = item.imageUrls?.[0]
  const statusMeta = STATUS_META[item.status] ?? STATUS_META.INACTIVE
  const isSold = item.status === 'SOLD'
  const [statusLoading, setStatusLoading] = useState(false)

  const handleMarkSold = async () => {
    if (statusLoading || isSold) return
    setStatusLoading(true)
    await onMarkSold(item)
    setStatusLoading(false)
  }

  return (
    <article className="glass-card rounded-[1.75rem] overflow-hidden flex flex-col border-slate-700/60 hover:border-slate-600/80 transition-colors">
      {/* Image */}
      <div className="relative aspect-[4/3] bg-slate-900/60 overflow-hidden flex-shrink-0">
        {imageUrl ? (
          <img src={imageUrl} alt={item.title} className="h-full w-full object-cover" loading="lazy" />
        ) : (
          <div className="flex h-full w-full items-center justify-center">
            <ImageIcon size={28} className="text-slate-600" />
          </div>
        )}
        {/* Status badge overlaid */}
        <span className={`absolute top-3 left-3 rounded-full border px-2.5 py-1 text-[0.65rem] font-black uppercase tracking-wide ${statusMeta.classes}`}>
          {statusMeta.label}
        </span>
        {/* Price badge */}
        <span className="absolute bottom-3 right-3 rounded-2xl border border-white/10 bg-slate-950/80 px-3 py-1.5 text-base font-black text-white backdrop-blur">
          {formatPrice(item.price)}
        </span>
      </div>

      {/* Body */}
      <div className="flex flex-col flex-1 p-4 gap-3">
        {/* Category tag */}
        <div className="flex items-center gap-1.5">
          <Tag size={11} className="text-slate-500" />
          <span className="text-[0.68rem] font-bold uppercase tracking-wide text-slate-500">
            {CATEGORY_LABELS[item.category] ?? item.category}
          </span>
        </div>

        {/* Title + description */}
        <div className="flex-1">
          <h3 className="font-black text-slate-100 line-clamp-1 text-sm">{item.title}</h3>
          <p className="mt-1 text-xs text-slate-500 line-clamp-2 leading-relaxed">{item.description}</p>
        </div>

        {/* Action row */}
        <div className="flex gap-2 pt-1">
          {/* Edit */}
          <button
            onClick={() => onEdit(item)}
            disabled={isSold}
            className="flex-1 flex items-center justify-center gap-1.5 rounded-2xl border border-white/10
                       bg-white/5 px-3 py-2 text-xs font-bold text-slate-300
                       hover:border-purple-400/40 hover:bg-purple-500/10 hover:text-purple-200
                       disabled:opacity-40 disabled:cursor-not-allowed"
            title="Edit listing"
          >
            <Pencil size={13} />
            Edit
          </button>

          {/* Mark as Sold */}
          <button
            onClick={handleMarkSold}
            disabled={isSold || statusLoading}
            className="flex-1 flex items-center justify-center gap-1.5 rounded-2xl border border-white/10
                       bg-white/5 px-3 py-2 text-xs font-bold text-slate-300
                       hover:border-emerald-400/40 hover:bg-emerald-500/10 hover:text-emerald-200
                       disabled:opacity-40 disabled:cursor-not-allowed"
            title={isSold ? 'Already sold' : 'Mark as sold'}
          >
            {statusLoading
              ? <Loader2 size={13} className="animate-spin" />
              : <HandshakeIcon size={13} />
            }
            {isSold ? 'Sold' : 'Sold?'}
          </button>

          {/* Delete */}
          <button
            onClick={() => onDelete(item)}
            className="w-9 h-9 flex items-center justify-center rounded-2xl border border-white/10
                       bg-white/5 text-slate-500
                       hover:border-rose-500/40 hover:bg-rose-500/10 hover:text-rose-300"
            title="Delete listing"
          >
            <Trash2 size={14} />
          </button>
        </div>
      </div>
    </article>
  )
}

// ─── Empty state ──────────────────────────────────────────────────────────────
function EmptyState() {
  return (
    <div className="col-span-full glass-card rounded-3xl p-12 flex flex-col items-center text-center">
      <div className="w-14 h-14 rounded-2xl bg-purple-500/15 flex items-center justify-center mb-4">
        <PackageOpen size={26} className="text-purple-400" />
      </div>
      <h2 className="text-lg font-black text-slate-100 mb-2">No listings yet</h2>
      <p className="text-slate-400 text-sm mb-6 max-w-xs leading-relaxed">
        You have not published anything yet. Create your first ad and start selling to fellow students.
      </p>
      <Link
        to="/marketplace/sell"
        className="inline-flex items-center gap-2 rounded-2xl bg-purple-500 hover:bg-purple-400 px-5 py-2.5 text-sm font-bold text-white"
      >
        <Plus size={16} />
        Create your first ad
      </Link>
    </div>
  )
}

// ─── Main component ───────────────────────────────────────────────────────────
export default function MyListingsDashboard() {
  const [items, setItems]       = useState([])
  const [loading, setLoading]   = useState(true)
  const [fetchError, setFetchError] = useState(null)

  const [editItem, setEditItem]     = useState(null)    // item being edited
  const [deleteItem, setDeleteItem] = useState(null)    // item pending delete
  const [deleting, setDeleting]     = useState(false)

  const [toast, setToast] = useState(null)

  const showToast = useCallback((message, type = 'success') => {
    setToast({ message, type })
  }, [])

  // ── Fetch on mount ─────────────────────────────────────────────────────────
  const fetchMyItems = useCallback(async () => {
    setFetchError(null)
    setLoading(true)
    try {
      const { data } = await marketplaceApi.getMyItems({ size: 100 })
      const list = data?.content ?? (Array.isArray(data) ? data : [])
      // Sort newest first (backend should, but ensure client-side too)
      setItems([...list].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt)))
    } catch (err) {
      setFetchError(err.response?.data?.message ?? 'Could not load your listings.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { fetchMyItems() }, [fetchMyItems])

  // ── Mark as Sold ───────────────────────────────────────────────────────────
  const handleMarkSold = useCallback(async (item) => {
    try {
      const { data } = await marketplaceApi.changeStatus(item.id, 'SOLD')
      setItems((prev) => prev.map((i) => (i.id === item.id ? data : i)))
      showToast(`"${item.title}" marked as sold.`)
    } catch (err) {
      showToast(err.response?.data?.message ?? 'Could not update status.', 'error')
    }
  }, [showToast])

  // ── Delete ─────────────────────────────────────────────────────────────────
  const handleDeleteConfirm = async () => {
    if (!deleteItem || deleting) return
    setDeleting(true)
    try {
      await marketplaceApi.deleteItem(deleteItem.id)
      setItems((prev) => prev.filter((i) => i.id !== deleteItem.id))
      showToast(`"${deleteItem.title}" deleted permanently.`)
      setDeleteItem(null)
    } catch (err) {
      showToast(err.response?.data?.message ?? 'Delete failed. Please try again.', 'error')
    } finally {
      setDeleting(false)
    }
  }

  // ── Edit success ───────────────────────────────────────────────────────────
  const handleEditSuccess = (updatedItem) => {
    setItems((prev) => prev.map((i) => (i.id === updatedItem.id ? updatedItem : i)))
    setEditItem(null)
    showToast(`"${updatedItem.title}" updated.`)
  }

  return (
    <AppShell>
      <div className="max-w-4xl mx-auto px-4 pt-6 pb-28 md:pb-8">

        {/* ── Header ── */}
        <div className="flex items-center justify-between mb-6">
          <div>
            <p className="text-slate-500 text-sm font-medium">Manage your ads</p>
            <h1 className="text-2xl font-black text-slate-100 tracking-tight">My Listings</h1>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={fetchMyItems}
              disabled={loading}
              className="w-9 h-9 flex items-center justify-center glass-card rounded-2xl text-slate-400
                         hover:text-purple-300 hover:border-purple-500/30 disabled:opacity-40"
              title="Refresh"
            >
              <RotateCcw size={15} className={loading ? 'animate-spin' : ''} />
            </button>
            <Link
              to="/marketplace/sell"
              className="flex items-center gap-2 rounded-2xl bg-purple-500 hover:bg-purple-400
                         px-4 py-2 text-sm font-bold text-white shadow-[0_0_20px_rgba(168,85,247,0.3)]"
            >
              <Plus size={16} />
              New Ad
            </Link>
          </div>
        </div>

        {/* ── Fetch error ── */}
        {fetchError && !loading && (
          <div className="flex items-center gap-3 glass-card rounded-2xl px-4 py-3 border-rose-500/30 border mb-4">
            <AlertCircle size={16} className="text-rose-400 flex-shrink-0" />
            <p className="text-rose-300 text-sm flex-1">{fetchError}</p>
            <button onClick={fetchMyItems} className="text-xs text-rose-400 font-bold hover:text-rose-200">
              Retry
            </button>
          </div>
        )}

        {/* ── Count badge ── */}
        {!loading && !fetchError && items.length > 0 && (
          <p className="text-slate-500 text-sm mb-4">
            {items.length} listing{items.length !== 1 ? 's' : ''}
          </p>
        )}

        {/* ── Grid ── */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {loading
            ? Array.from({ length: 6 }).map((_, i) => <SkeletonCard key={i} />)
            : items.length === 0 && !fetchError
            ? <EmptyState />
            : items.map((item) => (
              <ManagementCard
                key={item.id}
                item={item}
                onEdit={setEditItem}
                onDelete={setDeleteItem}
                onMarkSold={handleMarkSold}
              />
            ))
          }
        </div>
      </div>

      {/* ── Modals ── */}
      {editItem && (
        <EditAdModal
          item={editItem}
          onSuccess={handleEditSuccess}
          onCancel={() => setEditItem(null)}
        />
      )}

      {deleteItem && (
        <DeleteConfirmationModal
          item={deleteItem}
          loading={deleting}
          onConfirm={handleDeleteConfirm}
          onCancel={() => !deleting && setDeleteItem(null)}
        />
      )}

      {/* ── Toast ── */}
      <Toast toast={toast} onClose={() => setToast(null)} />
    </AppShell>
  )
}
