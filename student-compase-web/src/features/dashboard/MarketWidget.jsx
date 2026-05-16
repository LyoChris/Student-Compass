import { ShoppingBag, ChevronRight, Tag } from 'lucide-react'
import { NavLink } from 'react-router-dom'

function Sk({ className }) {
  return <div className={`skeleton ${className}`} />
}

const CONDITION_LABEL = {
  NEW:      'New',
  LIKE_NEW: 'Like New',
  GOOD:     'Good',
  FAIR:     'Fair',
}

export default function MarketWidget({ items, loading }) {
  if (loading) {
    return (
      <div className="glass-card rounded-3xl p-5">
        <div className="flex items-center justify-between mb-4">
          <Sk className="h-4 w-28" />
          <Sk className="h-4 w-14" />
        </div>
        <div className="space-y-2.5">
          <Sk className="h-16 w-full rounded-2xl" />
          <Sk className="h-16 w-full rounded-2xl" />
        </div>
      </div>
    )
  }

  return (
    <div className="glass-card rounded-3xl p-5 hover:border-purple-500/30">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <ShoppingBag size={16} className="text-purple-400" />
          <p className="text-slate-100 font-bold text-sm">Marketplace</p>
        </div>
        <NavLink
          to="/market"
          className="flex items-center gap-1 text-xs text-purple-400 font-semibold hover:text-purple-300"
        >
          See all <ChevronRight size={12} />
        </NavLink>
      </div>

      {items.length === 0 ? (
        <div className="py-6 text-center">
          <Tag size={24} className="text-slate-600 mx-auto mb-2" />
          <p className="text-slate-500 text-sm">No listings yet.</p>
        </div>
      ) : (
        <div className="space-y-2">
          {items.map((item) => (
            <NavLink
              key={item.id}
              to={`/marketplace/${item.id}`}
              className="flex items-center justify-between p-3 rounded-2xl bg-slate-700/50 hover:bg-slate-700 cursor-pointer active:scale-[0.98] transition-colors"
            >
              <div className="flex items-center gap-3 min-w-0">
                <div className="w-10 h-10 rounded-xl bg-purple-500/20 flex items-center justify-center flex-shrink-0">
                  {item.imageUrls?.[0]
                    ? <img src={item.imageUrls[0]} alt="" className="w-full h-full object-cover rounded-xl" />
                    : <ShoppingBag size={16} className="text-purple-400" />
                  }
                </div>
                <div className="min-w-0">
                  <p className="text-sm font-bold text-slate-100 leading-tight truncate">{item.title}</p>
                  <p className="text-xs text-slate-500 mt-0.5">
                    {CONDITION_LABEL[item.itemCondition] ?? item.itemCondition}
                  </p>
                </div>
              </div>
              <span className="text-sm font-black text-purple-400 flex-shrink-0 ml-3">
                {Number(item.price).toFixed(0)} RON
              </span>
            </NavLink>
          ))}
        </div>
      )}
    </div>
  )
}
