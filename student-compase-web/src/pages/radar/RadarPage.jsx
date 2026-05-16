import { MapPin, Navigation } from 'lucide-react'
import AppShell from '../../components/layout/AppShell'

const MOCK_DEALS = [
  { id: 1, name: 'Cantina Universitatii', discount: '-30%', type: 'Food',    distance: '50 m',  dotColor: 'bg-green-500/20', textColor: 'text-green-400'  },
  { id: 2, name: 'Xerox & Print Hub',     discount: '-15%', type: 'Service', distance: '120 m', dotColor: 'bg-blue-500/20',  textColor: 'text-blue-400'   },
]

export default function RadarPage() {
  return (
    <AppShell>
      <div className="max-w-2xl mx-auto px-4 pt-6 pb-28 md:pb-8 space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-black text-slate-100 tracking-tight">Radar</h1>
            <p className="text-slate-500 text-sm">Student deals near you</p>
          </div>
          <button className="flex items-center gap-2 px-4 py-2 rounded-2xl glass-card text-green-400 text-sm font-bold border-green-500/30 hover:bg-green-500/10">
            <Navigation size={15} /> Update
          </button>
        </div>

        <div className="glass-card rounded-3xl overflow-hidden" style={{ height: '200px' }}>
          <div className="w-full h-full bg-slate-800/60 flex flex-col items-center justify-center gap-2">
            <MapPin size={32} className="text-green-400" />
            <p className="text-slate-400 text-sm font-semibold">Map view coming soon</p>
          </div>
        </div>

        <div className="glass-card rounded-3xl p-5">
          <p className="text-slate-100 font-bold text-sm mb-4">Nearby Deals</p>
          <div className="space-y-2">
            {MOCK_DEALS.map((deal) => (
              <div key={deal.id} className="flex items-center justify-between p-3 rounded-2xl bg-slate-700/50 hover:bg-slate-700 cursor-pointer active:scale-[0.98]">
                <div className="flex items-center gap-3">
                  <div className={`w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0 ${deal.dotColor}`}>
                    <MapPin size={16} className={deal.textColor} />
                  </div>
                  <div>
                    <p className="text-sm font-bold text-slate-100 leading-tight">{deal.name}</p>
                    <p className="text-xs text-slate-500 mt-0.5">{deal.type} · {deal.distance}</p>
                  </div>
                </div>
                <span className={`text-sm font-black flex-shrink-0 ml-3 ${deal.textColor}`}>{deal.discount}</span>
              </div>
            ))}
          </div>
        </div>

        <div className="glass-card rounded-3xl p-6 text-center">
          <span className="text-xs font-bold px-3 py-1.5 rounded-full bg-green-500/20 text-green-400">
            GPS + full deal list in development
          </span>
        </div>
      </div>
    </AppShell>
  )
}
