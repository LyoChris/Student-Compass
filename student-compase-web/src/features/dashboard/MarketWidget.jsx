import { BookOpen, ChevronRight } from 'lucide-react'

function Sk({ className }) {
  return <div className={`skeleton ${className}`} />
}

const MOCK_ITEMS = [
  { id: 1, title: 'Calculus Vol. 1', price: 'RON 35', condition: 'Good',     seller: 'Mihai D.' },
  { id: 2, title: 'Fizica Cuantica',  price: 'RON 28', condition: 'Like New', seller: 'Ana P.'   },
]

export default function MarketWidget({ loading }) {
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
          <BookOpen size={16} className="text-purple-400" />
          <p className="text-slate-100 font-bold text-sm">Marketplace</p>
        </div>
        <button className="flex items-center gap-1 text-xs text-purple-400 font-semibold hover:text-purple-300">
          See all <ChevronRight size={12} />
        </button>
      </div>

      <div className="space-y-2">
        {MOCK_ITEMS.map((item) => (
          <div
            key={item.id}
            className="flex items-center justify-between p-3 rounded-2xl bg-slate-700/50 hover:bg-slate-700 cursor-pointer active:scale-[0.98]"
          >
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-purple-500/20 flex items-center justify-center flex-shrink-0">
                <BookOpen size={16} className="text-purple-400" />
              </div>
              <div>
                <p className="text-sm font-bold text-slate-100 leading-tight">{item.title}</p>
                <p className="text-xs text-slate-500 mt-0.5">{item.condition} · {item.seller}</p>
              </div>
            </div>
            <span className="text-sm font-black text-purple-400 flex-shrink-0 ml-3">{item.price}</span>
          </div>
        ))}
      </div>
    </div>
  )
}
