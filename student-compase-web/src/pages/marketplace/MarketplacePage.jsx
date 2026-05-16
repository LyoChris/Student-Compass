import { ShoppingBag, Plus, Search } from 'lucide-react'
import AppShell from '../../components/layout/AppShell'

export default function MarketplacePage() {
  return (
    <AppShell>
      <div className="max-w-2xl mx-auto px-4 pt-6 pb-28 md:pb-8 space-y-4">
        <div className="flex items-center justify-between">
          <h1 className="text-2xl font-black text-slate-100 tracking-tight">Marketplace</h1>
          <button className="flex items-center gap-2 px-4 py-2 rounded-2xl bg-purple-500 text-white text-sm font-bold hover:bg-purple-400 active:scale-95">
            <Plus size={16} />
            Sell
          </button>
        </div>

        <div className="glass-card rounded-2xl px-4 py-3 flex items-center gap-3">
          <Search size={16} className="text-slate-500 flex-shrink-0" />
          <input
            type="text"
            placeholder="Search textbooks, electronics..."
            className="flex-1 bg-transparent text-slate-100 placeholder-slate-500 text-sm outline-none"
          />
        </div>

        <div className="glass-card rounded-3xl p-12 text-center">
          <div className="w-16 h-16 rounded-2xl bg-blue-500/20 flex items-center justify-center mx-auto mb-4">
            <ShoppingBag size={28} className="text-blue-400" />
          </div>
          <h2 className="text-xl font-black text-slate-100 mb-2">Coming Soon</h2>
          <p className="text-slate-400 text-sm leading-relaxed max-w-xs mx-auto">
            Buy and sell textbooks, electronics, and gear with fellow students. Zero fees.
          </p>
          <span className="inline-block mt-4 text-xs font-bold px-3 py-1.5 rounded-full bg-blue-500/20 text-blue-400">
            In development
          </span>
        </div>
      </div>
    </AppShell>
  )
}
