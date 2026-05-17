import { useState, useEffect } from 'react'
import { Bell } from 'lucide-react'
import AppShell        from '../../components/layout/AppShell'
import { useAuth }     from '../../hooks/useAuth'
import HealthGauge     from '../../features/dashboard/HealthGauge'
import QuickStats      from '../../features/dashboard/QuickStats'
import AIWidget                from '../../features/dashboard/AIWidget'
import AiRecommendationsWidget from '../../features/dashboard/AiRecommendationsWidget'
import MarketWidget            from '../../features/dashboard/MarketWidget'
import RadarWidget     from '../../features/dashboard/RadarWidget'
import RecentActivity  from '../../features/dashboard/RecentActivity'
import { budgetApi }      from '../../api/budgetApi'
import { marketplaceApi } from '../../api/marketplaceApi'

export default function DashboardPage() {
  const { user } = useAuth()

  const [budget,     setBudget]     = useState(null)
  const [spendToday, setSpendToday] = useState(null)
  const [items,      setItems]      = useState([])
  const [loading,    setLoading]    = useState(true)

  useEffect(() => {
    const now = new Date()
    Promise.all([
      budgetApi.getCurrentBudget(now.getMonth() + 1, now.getFullYear()),
      budgetApi.getSpendToday(),
      marketplaceApi.search({ page: 0, size: 3, sort: 'createdAt,desc' }),
    ])
      .then(([budgetRes, todayRes, marketRes]) => {
        setBudget(budgetRes.data)
        setSpendToday(todayRes.data)
        setItems(marketRes.data?.content ?? [])
      })
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [])

  const firstName = user?.firstName ?? 'Student'
  const hour      = new Date().getHours()
  const greeting  = hour < 12 ? 'Good morning' : hour < 17 ? 'Good afternoon' : 'Good evening'

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
          </button>
        </div>

        <HealthGauge    budget={budget}      loading={loading} />
        <QuickStats     budget={budget}      loading={loading} />
        <AIWidget />
        <AiRecommendationsWidget />
        <MarketWidget   items={items}        loading={loading} />
        <RadarWidget />
        <RecentActivity spendToday={spendToday} loading={loading} />
      </div>
    </AppShell>
  )
}
