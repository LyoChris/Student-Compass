import { MapPin } from 'lucide-react'

function Sk({ className }) {
  return <div className={`skeleton ${className}`} />
}

const MOCK_DEALS = [
  { id: 1, name: 'Cantina Universitatii', discount: '-30%', type: 'Food',    distance: '50 m'  },
  { id: 2, name: 'Xerox & Print Hub',     discount: '-15%', type: 'Service', distance: '120 m' },
]

export default function RadarWidget({ loading }) {
  if (loading) {
    return (
      <div className="glass-card rounded-3xl p-5">
        <div className="flex items-center justify-between mb-4">
          <Sk className="h-4 w-28" />
          <Sk className="h-5 w-16 rounded-full" />
        </div>
        <div className="space-y-2.5">
          <Sk className="h-16 w-full rounded-2xl" />
          <Sk className="h-16 w-full rounded-2xl" />
        </div>
      </div>
    )
  }

  return (
    <div className="glass-card rounded-3xl p-5 hover:border-green-500/30">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <MapPin size={16} className="text-green-400" />
          <p className="text-slate-100 font-bold text-sm">Radar Deals</p>
        </div>
        <span className="text-xs font-bold px-2.5 py-1 rounded-full bg-green-500/20 text-green-400">
          Nearby
        </span>
      </div>

      <div className="space-y-2">
        {MOCK_DEALS.map((deal) => (
          <div
            key={deal.id}
            className="flex items-center justify-between p-3 rounded-2xl bg-slate-700/50 hover:bg-slate-700 cursor-pointer active:scale-[0.98]"
          >
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-green-500/20 flex items-center justify-center flex-shrink-0">
                <MapPin size={16} className="text-green-400" />
              </div>
              <div>
                <p className="text-sm font-bold text-slate-100 leading-tight">{deal.name}</p>
                <p className="text-xs text-slate-500 mt-0.5">{deal.type} · {deal.distance}</p>
              </div>
            </div>
            <span className="text-sm font-black text-green-400 flex-shrink-0 ml-3">{deal.discount}</span>
          </div>
        ))}
      </div>
    </div>
  )
}
