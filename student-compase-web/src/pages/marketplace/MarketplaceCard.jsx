import { BadgeCheck, ImageIcon, Sparkles, Tag } from 'lucide-react'
import { Link } from 'react-router-dom'

const conditionStyles = {
  NEW: 'bg-emerald-500/15 text-emerald-200 border-emerald-300/20',
  LIKE_NEW: 'bg-cyan-500/15 text-cyan-200 border-cyan-300/20',
  GOOD: 'bg-purple-500/15 text-purple-200 border-purple-300/20',
  FAIR: 'bg-amber-500/15 text-amber-200 border-amber-300/20',
}

const categoryLabels = {
  BOOKS_NOTES: 'Books & Notes',
  ELECTRONICS: 'Electronics',
  DORM_APPLIANCES: 'Dorm Gear',
  CLOTHING: 'Clothing',
  OTHER: 'Other',
}

function formatPrice(price) {
  return new Intl.NumberFormat('ro-RO', {
    style: 'currency',
    currency: 'RON',
    maximumFractionDigits: 0,
  }).format(Number(price || 0))
}

function readable(value) {
  return String(value || '').replaceAll('_', ' ')
}

export default function MarketplaceCard({ item }) {
  const imageUrl = item.imageUrls?.[0]
  const boosted = Boolean(item.isBoosted)

  return (
    <article
      className={`group relative overflow-hidden rounded-[1.75rem] border bg-slate-800/75 backdrop-blur-xl transition hover:-translate-y-1 ${
        boosted
          ? 'border-purple-300/40 shadow-[0_0_15px_rgba(168,85,247,0.4)]'
          : 'border-white/10 shadow-lg shadow-black/10 hover:border-white/20'
      }`}
    >
      {boosted && (
        <>
          <div className="boosted-edge absolute inset-0 rounded-[1.75rem] opacity-70" />
          <div className="absolute left-4 top-4 z-20 flex items-center gap-1.5 rounded-full border border-purple-200/20 bg-purple-500/90 px-3 py-1.5 text-xs font-black text-white shadow-[0_0_20px_rgba(168,85,247,0.55)]">
            <Sparkles size={13} />
            Boosted
          </div>
        </>
      )}

      <div className="relative z-10">
        <Link to={`/marketplace/${item.id}`} className="block" aria-label={`View ${item.title}`}>
          <div className="relative aspect-[4/3] overflow-hidden bg-slate-950/50">
          {imageUrl ? (
            <img
              src={imageUrl}
              alt={item.title}
              className="h-full w-full object-cover transition duration-500 group-hover:scale-105"
              loading="lazy"
            />
          ) : (
            <div className="flex h-full w-full items-center justify-center">
              <div className="flex h-16 w-16 items-center justify-center rounded-3xl bg-white/5 text-slate-500">
                <ImageIcon size={28} />
              </div>
            </div>
          )}
            <div className="absolute bottom-3 right-3 rounded-2xl border border-white/10 bg-slate-950/80 px-3 py-2 text-lg font-black text-white backdrop-blur">
              {formatPrice(item.price)}
            </div>
          </div>
        </Link>

        <div className="space-y-4 p-4">
          <div className="flex flex-wrap items-center gap-2">
            <span className={`rounded-full border px-2.5 py-1 text-[0.68rem] font-black uppercase tracking-wide ${conditionStyles[item.itemCondition] || conditionStyles.GOOD}`}>
              {readable(item.itemCondition)}
            </span>
            <span className="flex items-center gap-1 rounded-full border border-white/10 bg-white/5 px-2.5 py-1 text-[0.68rem] font-black uppercase tracking-wide text-slate-400">
              <Tag size={11} />
              {categoryLabels[item.category] || readable(item.category)}
            </span>
          </div>

          <div>
            <h3 className="line-clamp-1 text-base font-black text-slate-100">{item.title}</h3>
            <p className="mt-1 line-clamp-2 min-h-10 text-sm leading-5 text-slate-400">{item.description}</p>
          </div>

          <div className="flex flex-wrap gap-2">
            {(item.tags || []).slice(0, 3).map((tag) => (
              <span key={tag} className="rounded-full bg-slate-950/45 px-2.5 py-1 text-xs font-bold text-slate-400">
                #{tag}
              </span>
            ))}
          </div>

          <Link
            to={`/marketplace/${item.id}`}
            className="flex w-full items-center justify-center gap-2 rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-sm font-black text-slate-200 hover:border-purple-300/30 hover:bg-purple-500/15 hover:text-purple-100"
          >
            <BadgeCheck size={16} />
            View Details
          </Link>
        </div>
      </div>
    </article>
  )
}
