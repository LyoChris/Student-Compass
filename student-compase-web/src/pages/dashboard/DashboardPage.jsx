import { useState, useEffect } from 'react'
import { Bell } from 'lucide-react'
import AppShell      from '../../components/layout/AppShell'
import { useAuth }   from '../../hooks/useAuth'
import HealthGauge    from '../../features/dashboard/HealthGauge'
import QuickStats     from '../../features/dashboard/QuickStats'
import AIWidget       from '../../features/dashboard/AIWidget'
import MarketWidget   from '../../features/dashboard/MarketWidget'
import RadarWidget    from '../../features/dashboard/RadarWidget'
import RecentActivity from '../../features/dashboard/RecentActivity'

export default function DashboardPage() {
  const { user } = useAuth()
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const t = setTimeout(() => setLoading(false), 1800)
    return () => clearTimeout(t)
  }, [])

  const firstName = user?.firstName ?? 'Student'
  const hour      = new Date().getHours()
  const greeting  =
    hour < 12 ? 'Good morning' : hour < 17 ? 'Good afternoon' : 'Good evening'

  return (
    <AppShell>
      <div className="max-w-2xl mx-auto px-4 pt-6 pb-28 md:pb-8 space-y-4">

        <div className="flex items-center justify-between mb-2">
          <div>
            <p className="text-slate-500 text-sm font-medium">{greeting},</p>
            <h1 className="text-2xl font-black text-slate-100 tracking-tight">
              Hey, {firstName}!
            </h1>
          </div>
          <button
            className="relative w-11 h-11 rounded-2xl glass-card flex items-center justify-center hover:border-purple-500/40"
            aria-label="Notifications"
          >
            <Bell size={18} className="text-slate-400" />
            <span className="absolute top-2.5 right-2.5 w-2 h-2 rounded-full bg-purple-500 ring-2 ring-slate-900" />
          </button>
        </div>

        <HealthGauge    percentage={85} loading={loading} />
        <QuickStats     loading={loading} />
        <AIWidget />
        <MarketWidget   loading={loading} />
        <RadarWidget    loading={loading} />
        <RecentActivity loading={loading} />

      </div>
    </AppShell>
  )
}
