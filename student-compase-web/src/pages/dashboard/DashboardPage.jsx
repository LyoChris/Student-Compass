import { useState, useEffect } from 'react'
import { Bell } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import AppShell                from '../../components/layout/AppShell'
import { useAuth }             from '../../hooks/useAuth'
import HealthGauge             from '../../features/dashboard/HealthGauge'
import QuickStats              from '../../features/dashboard/QuickStats'
import BudgetBreakdownWidget   from '../../features/dashboard/BudgetBreakdownWidget'
import ExpandableAiChat        from '../../features/dashboard/ExpandableAiChat'
import AiRecommendationsWidget from '../../features/dashboard/AiRecommendationsWidget'
import MarketWidget            from '../../features/dashboard/MarketWidget'
import RadarWidget             from '../../features/dashboard/RadarWidget'
import RecentActivity          from '../../features/dashboard/RecentActivity'
import { budgetApi }           from '../../api/budgetApi'
import { marketplaceApi }      from '../../api/marketplaceApi'
import { profileApi }          from '../../api/profileApi'

export default function DashboardPage() {
  const { user } = useAuth()
  const { t }    = useTranslation()

  const [budget,           setBudget]           = useState(null)
  const [spendToday,       setSpendToday]       = useState(null)
  const [items,            setItems]            = useState([])
  const [loading,          setLoading]          = useState(true)
  const [profileFixedTotal, setProfileFixedTotal] = useState(null)

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

    if (user?.id) {
      profileApi.getProfile(user.id)
        .then(({ data }) => {
          const expenses = Array.isArray(data?.fixedExpenses) ? data.fixedExpenses : []
          const total = expenses.reduce((sum, e) => sum + Number(e.amount ?? 0), 0)
          setProfileFixedTotal(total)
        })
        .catch(() => {})
    }
  }, [])

  const firstName = user?.firstName ?? 'Student'
  const hour      = new Date().getHours()
  const greetKey  = hour < 12 ? 'dashboard.goodMorning' : hour < 17 ? 'dashboard.goodAfternoon' : 'dashboard.goodEvening'
  const greeting  = t(greetKey)

  return (
    <AppShell>
      <div className="max-w-3xl mx-auto px-4 pt-6 pb-10 space-y-5">

        {/* ── Greeting header ── */}
        <div className="flex items-center justify-between">
          <div>
            <p className="text-slate-500 text-sm font-medium">{greeting},</p>
            <h1 className="text-2xl font-black text-slate-100 tracking-tight">
              Hey, {firstName}!
            </h1>
          </div>
          <button
            className="relative w-11 h-11 rounded-2xl glass-card flex items-center justify-center hover:border-purple-500/40"
            aria-label={t('common.notifications')}
          >
            <Bell size={18} className="text-slate-400" />
          </button>
        </div>

        {/* ── Financial health gauge — full width ── */}
        <HealthGauge budget={budget} loading={loading} />

        {/* ── Quick stats strip — full width ── */}
        <QuickStats budget={budget} loading={loading} />

        {/* ── 2-col grid: Budget breakdown + Radar widget ── */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <BudgetBreakdownWidget budget={budget} loading={loading} profileFixedTotal={profileFixedTotal} />
          <RadarWidget />
        </div>

        {/* ── Expandable AI Chat — floating widget ── */}
        <ExpandableAiChat />

        {/* ── AI picks carousel — full width (FOOD category default) ── */}
        <AiRecommendationsWidget category="FOOD" />

        {/* ── 2-col grid: Marketplace + Recent activity ── */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <MarketWidget items={items} loading={loading} />
          <RecentActivity spendToday={spendToday} loading={loading} />
        </div>

      </div>
    </AppShell>
  )
}
