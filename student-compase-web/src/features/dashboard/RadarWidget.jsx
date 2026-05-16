import { MapPin, Radar } from 'lucide-react'

function Sk({ className }) {
  return <div className={`skeleton ${className}`} />
}

export default function RadarWidget({ loading }) {
  if (loading) {
    return (
      <div className="glass-card rounded-3xl p-5">
        <div className="flex items-center justify-between mb-4">
          <Sk className="h-4 w-28" />
          <Sk className="h-5 w-16 rounded-full" />
        </div>
        <Sk className="h-24 w-full rounded-2xl" />
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

      <div className="flex flex-col items-center justify-center py-6 text-center gap-2">
        <Radar size={28} className="text-slate-600" />
        <p className="text-slate-500 text-sm font-semibold">Radar coming soon</p>
        <p className="text-slate-600 text-xs">
          Location-based student deals will appear here once the feature launches.
        </p>
      </div>
    </div>
  )
}
