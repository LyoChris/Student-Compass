import { MapPin, Radar } from 'lucide-react'
import AppShell from '../../components/layout/AppShell'

export default function RadarPage() {
  return (
    <AppShell>
      <div className="max-w-2xl mx-auto px-4 pt-6 pb-28 md:pb-8 space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-black text-slate-100 tracking-tight">Radar</h1>
            <p className="text-slate-500 text-sm">Student deals near you</p>
          </div>
          <div className="w-10 h-10 rounded-2xl bg-green-500/20 flex items-center justify-center">
            <MapPin size={18} className="text-green-400" />
          </div>
        </div>

        <div className="glass-card rounded-3xl overflow-hidden" style={{ height: '200px' }}>
          <div className="w-full h-full bg-slate-800/60 flex flex-col items-center justify-center gap-2">
            <MapPin size={32} className="text-green-400" />
            <p className="text-slate-400 text-sm font-semibold">Map view coming soon</p>
          </div>
        </div>

        <div className="glass-card rounded-3xl p-10 flex flex-col items-center justify-center text-center gap-4">
          <div className="w-16 h-16 rounded-2xl bg-green-500/15 flex items-center justify-center">
            <Radar size={28} className="text-green-400" />
          </div>
          <div>
            <h2 className="text-lg font-black text-slate-100 mb-1">Radar is in development</h2>
            <p className="text-slate-500 text-sm leading-relaxed max-w-xs">
              Location-based student discounts and nearby campus deals will appear here once the feature launches.
            </p>
          </div>
          <span className="text-xs font-bold px-3 py-1.5 rounded-full bg-green-500/20 text-green-400">
            GPS + deal feed coming soon
          </span>
        </div>
      </div>
    </AppShell>
  )
}
