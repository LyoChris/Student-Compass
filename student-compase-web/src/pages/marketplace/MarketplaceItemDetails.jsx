import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
  ArrowLeft,
  Heart,
  ImageIcon,
  MessageCircle,
  Share2,
  ShieldCheck,
  Tag,
} from 'lucide-react'
import { marketplaceApi } from '../../api/marketplaceApi'

const conditionStyles = {
  NEW: 'border-emerald-300/25 bg-emerald-500/15 text-emerald-200',
  LIKE_NEW: 'border-cyan-300/25 bg-cyan-500/15 text-cyan-200',
  GOOD: 'border-purple-300/25 bg-purple-500/15 text-purple-200',
  FAIR: 'border-amber-300/25 bg-amber-500/15 text-amber-200',
}

const categoryLabels = {
  BOOKS_NOTES: 'Books & Notes',
  ELECTRONICS: 'Electronics',
  DORM_APPLIANCES: 'Dorm Gear',
  CLOTHING: 'Clothing',
  OTHER: 'Other',
}

function readable(value) {
  return String(value || '').replaceAll('_', ' ')
}

function formatPrice(price) {
  return new Intl.NumberFormat('ro-RO', {
    style: 'currency',
    currency: 'RON',
    maximumFractionDigits: 0,
  }).format(Number(price || 0))
}

function DetailSkeleton() {
  return (
    <div className="min-h-screen bg-[#0F172A] px-4 pb-36 pt-5 md:pb-28 lg:px-6">
      <div className="mx-auto max-w-6xl">
        <div className="skeleton mb-5 h-10 w-32 rounded-2xl" />
        <div className="grid gap-5 lg:grid-cols-[1.15fr_0.85fr]">
          <div className="skeleton aspect-[4/3] rounded-[2rem] lg:aspect-[5/4]" />
          <div className="glass-card rounded-[2rem] p-5">
            <div className="skeleton h-8 w-4/5 rounded-xl" />
            <div className="skeleton mt-4 h-12 w-48 rounded-xl" />
            <div className="mt-6 space-y-3">
              <div className="skeleton h-4 w-full rounded-lg" />
              <div className="skeleton h-4 w-5/6 rounded-lg" />
              <div className="skeleton h-4 w-3/4 rounded-lg" />
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

function GalleryFallback() {
  return (
    <div className="glass-card flex aspect-[4/3] min-h-80 w-full flex-col items-center justify-center rounded-[2rem] border border-white/10 bg-slate-950/35 p-8 text-center lg:aspect-[5/4]">
      <div className="flex h-20 w-20 items-center justify-center rounded-[1.75rem] bg-purple-500/10 text-purple-200 shadow-[0_0_28px_rgba(168,85,247,0.18)]">
        <ImageIcon size={34} />
      </div>
      <p className="mt-4 text-sm font-black text-slate-200">No Image Provided</p>
      <p className="mt-1 max-w-xs text-xs leading-5 text-slate-500">The seller has not uploaded photos for this item yet.</p>
    </div>
  )
}

function ImageGallery({ images, activeImage, onActiveImageChange, title }) {
  if (!images.length) return <GalleryFallback />

  return (
    <section className="space-y-3">
      <div className="lg:hidden">
        <div className="glass-card flex snap-x snap-mandatory overflow-x-auto rounded-[2rem] scroll-smooth">
          {images.map((image, index) => (
            <button
              key={`${image}-${index}`}
              type="button"
              onClick={() => onActiveImageChange(index)}
              className="relative h-[46vh] min-h-80 min-w-full snap-center bg-slate-950/50"
            >
              <img
                src={image}
                alt={`${title} ${index + 1}`}
                className="h-full w-full object-cover"
              />
            </button>
          ))}
        </div>

        {images.length > 1 && (
          <div className="mt-3 flex justify-center gap-2">
            {images.map((image, index) => (
              <button
                key={`${image}-dot`}
                type="button"
                onClick={() => onActiveImageChange(index)}
                className={`h-2 rounded-full transition-all ${activeImage === index ? 'w-7 bg-purple-400 shadow-[0_0_12px_rgba(168,85,247,0.7)]' : 'w-2 bg-white/25'}`}
                aria-label={`Show image ${index + 1}`}
              />
            ))}
          </div>
        )}
      </div>

      <div className="hidden lg:block">
        <div className="glass-card overflow-hidden rounded-[2rem] border border-white/10 bg-slate-950/35">
          <div className="aspect-[5/4] bg-slate-950/50">
            <img
              src={images[activeImage] || images[0]}
              alt={`${title} selected`}
              className="h-full w-full object-cover"
            />
          </div>
        </div>

        {images.length > 1 && (
          <div className="mt-3 flex gap-3 overflow-x-auto pb-1">
            {images.map((image, index) => (
              <button
                key={`${image}-thumb`}
                type="button"
                onClick={() => onActiveImageChange(index)}
                className={`h-20 w-20 shrink-0 overflow-hidden rounded-2xl border bg-slate-900 transition xl:h-24 xl:w-24 ${
                  activeImage === index
                    ? 'border-purple-300 shadow-[0_0_20px_rgba(168,85,247,0.35)]'
                    : 'border-white/10 opacity-70 hover:border-white/25 hover:opacity-100'
                }`}
                aria-label={`Select image ${index + 1}`}
              >
                <img src={image} alt="" className="h-full w-full object-cover" />
              </button>
            ))}
          </div>
        )}
      </div>
    </section>
  )
}

export default function MarketplaceItemDetails() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [item, setItem] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [activeImage, setActiveImage] = useState(0)
  const [saved, setSaved] = useState(false)
  const [contacted, setContacted] = useState(false)

  useEffect(() => {
    let ignore = false

    const loadItem = async () => {
      setLoading(true)
      setError('')

      try {
        const { data } = await marketplaceApi.getById(id)
        if (!ignore) setItem(data)
      } catch (err) {
        if (!ignore) setError(err.response?.data?.message || 'Could not load this marketplace item.')
      } finally {
        if (!ignore) setLoading(false)
      }
    }

    loadItem()

    return () => {
      ignore = true
    }
  }, [id])

  const images = useMemo(() => item?.imageUrls?.filter(Boolean) || [], [item])
  const safeActiveImage = Math.min(activeImage, Math.max(images.length - 1, 0))

  if (loading) return <DetailSkeleton />

  if (error || !item) {
    return (
      <div className="min-h-screen bg-[#0F172A] px-4 py-8 text-slate-100">
        <div className="mx-auto max-w-xl rounded-[2rem] border border-red-400/25 bg-red-500/10 p-6 text-center">
          <p className="text-sm font-bold text-red-200">{error || 'Item not found.'}</p>
          <button
            type="button"
            onClick={() => navigate('/marketplace')}
            className="mt-5 rounded-2xl bg-purple-500 px-4 py-3 text-sm font-black text-white hover:bg-purple-400"
          >
            Back to Marketplace
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-[#0F172A] px-4 pb-40 pt-5 text-slate-100 md:pb-32 lg:px-6">
      <div className="pointer-events-none fixed inset-0">
        <div className="absolute right-[-14rem] top-[-16rem] h-[34rem] w-[34rem] rounded-full bg-purple-500/20 blur-3xl" />
        <div className="absolute bottom-[-18rem] left-[-10rem] h-[32rem] w-[32rem] rounded-full bg-cyan-500/10 blur-3xl" />
      </div>

      <div className="relative mx-auto max-w-6xl">
        <div className="mb-5 flex items-center justify-between gap-3">
          <Link
            to="/marketplace"
            className="flex items-center gap-2 rounded-2xl border border-white/10 bg-white/5 px-3 py-2 text-sm font-black text-slate-300 hover:bg-white/10 hover:text-slate-100"
          >
            <ArrowLeft size={17} />
            Market
          </Link>
          <button
            type="button"
            className="flex h-10 w-10 items-center justify-center rounded-2xl border border-white/10 bg-white/5 text-slate-400 hover:bg-white/10 hover:text-slate-100"
            aria-label="Share item"
          >
            <Share2 size={17} />
          </button>
        </div>

        <div className="grid gap-5 lg:grid-cols-[1.12fr_0.88fr]">
          <ImageGallery
            images={images}
            activeImage={safeActiveImage}
            onActiveImageChange={setActiveImage}
            title={item.title}
          />

          <section className="space-y-4">
            <div className="glass-card rounded-[2rem] p-5 sm:p-6">
              <div className="mb-4 flex flex-wrap items-center gap-2">
                {item.isBoosted && (
                  <span className="rounded-full border border-purple-300/25 bg-purple-500/20 px-3 py-1.5 text-xs font-black text-purple-100 shadow-[0_0_18px_rgba(168,85,247,0.25)]">
                    Boosted
                  </span>
                )}
                <span className={`rounded-full border px-3 py-1.5 text-xs font-black uppercase tracking-wide ${conditionStyles[item.itemCondition] || conditionStyles.GOOD}`}>
                  {readable(item.itemCondition)}
                </span>
              </div>

              <h1 className="text-3xl font-black leading-tight tracking-tight sm:text-4xl">{item.title}</h1>
              <p className="mt-4 text-5xl font-black text-purple-300 drop-shadow-[0_0_18px_rgba(168,85,247,0.35)]">
                {formatPrice(item.price)}
              </p>

              <div className="mt-5 flex flex-wrap gap-2">
                <span className="flex items-center gap-1.5 rounded-full border border-white/10 bg-white/5 px-3 py-1.5 text-xs font-black text-slate-300">
                  <Tag size={12} />
                  {categoryLabels[item.category] || readable(item.category)}
                </span>
                {(item.tags || []).map((tag) => (
                  <span key={tag} className="rounded-full bg-purple-500/10 px-3 py-1.5 text-xs font-bold text-purple-200">
                    #{tag}
                  </span>
                ))}
              </div>
            </div>

            <div className="glass-card rounded-[2rem] p-5 sm:p-6">
              <h2 className="mb-3 text-lg font-black text-slate-100">Description</h2>
              <p className="whitespace-pre-wrap text-sm leading-7 text-slate-300">{item.description}</p>
            </div>

            <div className="glass-card rounded-[2rem] p-5 sm:p-6">
              <div className="flex items-center gap-3">
                <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-emerald-500/15 text-emerald-200">
                  <ShieldCheck size={21} />
                </div>
                <div>
                  <p className="text-sm font-black text-slate-100">Student-safe marketplace</p>
                  <p className="text-xs leading-5 text-slate-500">Meet on campus and verify the item before payment.</p>
                </div>
              </div>
            </div>

            {contacted && (
              <div className="rounded-2xl border border-purple-300/25 bg-purple-500/10 p-4 text-sm font-bold text-purple-100">
                Chat simulation opened. In production this can route to `/chat` or WhatsApp.
              </div>
            )}
          </section>
        </div>
      </div>

      <div className="fixed inset-x-0 bottom-20 z-40 border-t border-white/10 bg-slate-950/90 p-3 backdrop-blur-xl md:bottom-0 md:left-64">
        <div className="mx-auto flex max-w-4xl gap-3">
          <button
            type="button"
            onClick={() => setSaved((current) => !current)}
            className={`flex h-14 w-16 shrink-0 items-center justify-center rounded-2xl border text-sm font-black transition ${
              saved
                ? 'border-red-300/30 bg-red-500/15 text-red-200'
                : 'border-white/10 bg-white/5 text-slate-300 hover:bg-white/10'
            }`}
            aria-label="Save item"
          >
            <Heart size={21} fill={saved ? 'currentColor' : 'none'} />
          </button>
          <button
            type="button"
            onClick={() => setContacted(true)}
            className="flex min-h-14 flex-1 items-center justify-center gap-2 rounded-2xl bg-purple-500 px-5 py-3 text-sm font-black text-white shadow-[0_0_32px_rgba(168,85,247,0.45)] hover:bg-purple-400"
          >
            <MessageCircle size={19} />
            Contact Seller
          </button>
        </div>
      </div>
    </div>
  )
}
