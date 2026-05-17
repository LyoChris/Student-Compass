import { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  ChevronLeft, ChevronRight, Wallet, TrendingUp, Trophy,
  Plus, Upload, SlidersHorizontal, AlertCircle,
  CheckCircle2, X, Loader2, Sparkles, Lock, ArrowDown, Equal,
} from 'lucide-react'
import AppShell from '../../components/layout/AppShell'
import { budgetApi } from '../../api/budgetApi'
import ManageCategoriesModal from './ManageCategoriesModal'
import AddTransactionModal from './AddTransactionModal'
import UploadStatementModal from './UploadStatementModal'

// ─── Category display meta ────────────────────────────────────────────────────
const CAT_META = {
  FOOD:      { label: 'Food',       color: '#A855F7' },
  TRANSPORT: { label: 'Transport',  color: '#22D3EE' },
  HOUSING:   { label: 'Housing',    color: '#F59E0B' },
  SUPPLIES:  { label: 'Supplies',   color: '#34D399' },
  PERSONAL:  { label: 'Personal',   color: '#F472B6' },
  LEISURE:   { label: 'Leisure',    color: '#60A5FA' },
  SAVINGS:   { label: 'Savings',    color: '#4ADE80' },
}

const MONTH_NAMES = [
  'January','February','March','April','May','June',
  'July','August','September','October','November','December',
]

function fmt(n)  { return Number(n ?? 0).toFixed(2) }
function fmtRon(n) {
  return new Intl.NumberFormat('ro-RO', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(Number(n ?? 0))
}

function Sk({ className }) {
  return <div className={`skeleton ${className}`} />
}

// ─── Toast ────────────────────────────────────────────────────────────────────
function Toast({ toast, onClose }) {
  useEffect(() => {
    if (!toast) return
    const t = setTimeout(onClose, 4000)
    return () => clearTimeout(t)
  }, [toast, onClose])
  if (!toast) return null
  const isErr = toast.type === 'error'
  return (
    <div className={`fixed bottom-6 left-1/2 -translate-x-1/2 z-50 flex items-center gap-3
        glass-card rounded-2xl px-4 py-3 shadow-xl max-w-sm w-[calc(100%-2rem)]
        border ${isErr ? 'border-rose-500/30' : 'border-emerald-500/30'}`}>
      {isErr
        ? <AlertCircle size={16} className="text-rose-400 flex-shrink-0" />
        : <CheckCircle2 size={16} className="text-emerald-400 flex-shrink-0" />}
      <p className={`text-sm flex-1 ${isErr ? 'text-rose-300' : 'text-emerald-300'}`}>{toast.message}</p>
      <button onClick={onClose} className="text-slate-500 hover:text-slate-300 flex-shrink-0"><X size={13} /></button>
    </div>
  )
}

// ─── S2S KPI card ─────────────────────────────────────────────────────────────
function S2SCard({ value, loading }) {
  if (loading) return (
    <div className="glass-card rounded-3xl p-6 flex flex-col items-center justify-center text-center">
      <Sk className="h-4 w-36 mb-4 mx-auto" />
      <Sk className="h-14 w-48 mb-2 mx-auto rounded-2xl" />
      <Sk className="h-3 w-24 mx-auto" />
    </div>
  )

  const v       = parseFloat(value ?? 0)
  const display = v.toFixed(2)

  const isGreen  = v >= 25
  const isAmber  = v >= 10 && v < 25
  // isRed covers both < 10 and exactly 0

  const color = isGreen ? 'text-emerald-400' : isAmber ? 'text-amber-400' : 'text-rose-500'

  const glow  = isGreen
    ? 'shadow-[0_0_52px_rgba(16,185,129,0.30)]'
    : isAmber
    ? 'shadow-[0_0_52px_rgba(251,191,36,0.25)]'
    : 'shadow-[0_0_52px_rgba(244,63,94,0.35)] animate-pulse'

  const badge = isGreen
    ? 'bg-emerald-500/15 text-emerald-400 border-emerald-400/30'
    : isAmber
    ? 'bg-amber-500/15  text-amber-400  border-amber-400/30'
    : 'bg-rose-500/15   text-rose-400   border-rose-500/40'

  const label = isGreen ? 'You are good!' : isAmber ? 'Spend carefully' : v === 0 ? 'Budget exhausted' : 'Critical — slow down'

  return (
    <div className={`glass-card rounded-3xl p-6 flex flex-col items-center justify-center text-center ${glow}`}>
      <p className="text-slate-400 text-xs font-semibold uppercase tracking-wider mb-3">Safe to Spend Today</p>
      <p className={`text-6xl font-black tabular-nums leading-none ${color} mb-1`}>
        {display}
      </p>
      <p className="text-slate-500 text-xs mb-3">RON / day</p>
      <span className={`inline-block rounded-full border px-3 py-1 text-[0.68rem] font-black uppercase tracking-wide ${badge}`}>
        {label}
      </span>
    </div>
  )
}

// ─── Small KPI card ───────────────────────────────────────────────────────────
function KpiCard({ icon: Icon, label, value, accent, loading }) {
  if (loading) return (
    <div className="glass-card rounded-2xl p-4">
      <Sk className="h-8 w-8 rounded-xl mb-3" />
      <Sk className="h-3 w-20 mb-2" />
      <Sk className="h-6 w-28" />
    </div>
  )
  return (
    <div className={`glass-card rounded-2xl p-4 border-l-2 ${accent.border}`}>
      <div className={`w-9 h-9 rounded-xl flex items-center justify-center mb-3 ${accent.bg}`}>
        <Icon size={18} className={accent.text} />
      </div>
      <p className="text-slate-400 text-[0.68rem] font-semibold uppercase tracking-wider mb-1">{label}</p>
      <p className={`text-lg font-black ${accent.text}`}>{fmtRon(value)} <span className="text-xs font-bold">RON</span></p>
    </div>
  )
}

// ─── Income breakdown card ────────────────────────────────────────────────────
function IncomeBreakdownCard({ budget, loading }) {
  if (loading) return (
    <div className="glass-card rounded-3xl p-5 space-y-3">
      <div className="flex items-center gap-2 mb-1">
        <div className="skeleton h-4 w-4 rounded" />
        <div className="skeleton h-3 w-32 rounded" />
      </div>
      <div className="skeleton h-12 w-full rounded-2xl" />
      <div className="skeleton h-1 w-full rounded" />
      <div className="skeleton h-12 w-full rounded-2xl" />
      <div className="skeleton h-1 w-full rounded" />
      <div className="skeleton h-12 w-full rounded-2xl" />
    </div>
  )

  const income  = Number(budget?.totalIncome ?? 0)
  // fixed total: prefer explicit field, fall back to income - disposable
  const fixed   = Number(
    budget?.fixedExpensesTotal ?? budget?.fixedTotal ?? (income - Number(budget?.disposableIncome ?? budget?.totalAllocated ?? 0))
  )
  const disposable = income - fixed

  if (!income) return null

  return (
    <div className="glass-card rounded-3xl p-5">
      <div className="flex items-center gap-2 mb-4">
        <div className="w-7 h-7 rounded-xl bg-amber-500/15 flex items-center justify-center">
          <Lock size={13} className="text-amber-400" />
        </div>
        <span className="text-sm font-black text-slate-100">Income Breakdown</span>
        <span className="ml-auto text-[0.62rem] text-slate-600 font-semibold uppercase tracking-wider">How we calculate your budget</span>
      </div>

      {/* Row: Total Income */}
      <div className="flex items-center justify-between rounded-2xl bg-emerald-500/8 border border-emerald-500/15 px-4 py-3">
        <div className="flex items-center gap-2.5">
          <TrendingUp size={14} className="text-emerald-400" />
          <span className="text-sm font-bold text-slate-300">Monthly Income</span>
        </div>
        <span className="text-sm font-black text-emerald-300 tabular-nums">
          {fmtRon(income)} <span className="text-xs font-bold text-slate-500">RON</span>
        </span>
      </div>

      {/* Divider with arrow */}
      <div className="flex items-center gap-2 my-1.5 px-4">
        <ArrowDown size={12} className="text-rose-400/60 mx-auto" />
      </div>

      {/* Row: Fixed Expenses */}
      <div className="flex items-center justify-between rounded-2xl bg-rose-500/8 border border-rose-500/15 px-4 py-3">
        <div className="flex items-center gap-2.5">
          <Lock size={14} className="text-rose-400" />
          <span className="text-sm font-bold text-slate-300">Fixed Expenses</span>
        </div>
        <span className="text-sm font-black text-rose-400 tabular-nums">
          −{fmtRon(fixed)} <span className="text-xs font-bold text-slate-500">RON</span>
        </span>
      </div>

      {/* Divider with equals */}
      <div className="flex items-center gap-2 my-1.5 px-4">
        <Equal size={12} className="text-purple-400/60 mx-auto" />
      </div>

      {/* Row: Disposable */}
      <div
        className="flex items-center justify-between rounded-2xl px-4 py-3 border border-purple-500/25"
        style={{ background: 'rgba(168,85,247,0.10)', boxShadow: '0 0 16px rgba(168,85,247,0.1)' }}
      >
        <div className="flex items-center gap-2.5">
          <Wallet size={14} className="text-purple-400" />
          <span className="text-sm font-bold text-slate-200">Disposable Budget</span>
        </div>
        <span
          className="text-sm font-black tabular-nums"
          style={{ color: '#A855F7', textShadow: '0 0 12px rgba(168,85,247,0.4)' }}
        >
          {fmtRon(disposable)} <span className="text-xs font-bold text-slate-500">RON</span>
        </span>
      </div>

      <p className="text-[0.62rem] text-slate-600 text-center mt-3 leading-relaxed">
        Your safe-to-spend and category limits are calculated from the <span className="text-slate-500">Disposable Budget</span>, not total income.
      </p>
    </div>
  )
}

// ─── Category progress card ───────────────────────────────────────────────────
function CategoryCard({ cat, loading }) {
  const navigate = useNavigate()

  if (loading) return (
    <div className="glass-card rounded-2xl p-4 space-y-3">
      <div className="flex justify-between">
        <Sk className="h-4 w-20" />
        <Sk className="h-4 w-24" />
      </div>
      <Sk className="h-2 w-full rounded-full" />
      <Sk className="h-8 w-32 rounded-xl" />
    </div>
  )

  const meta      = CAT_META[cat.name?.toUpperCase()] ?? { label: cat.name, color: '#A855F7' }
  const alloc     = Number(cat.allocatedAmount ?? 0)
  const spent     = Number(cat.spentAmount ?? 0)
  const pct       = alloc > 0 ? Math.min((spent / alloc) * 100, 100) : 0
  const barColor  = pct < 50 ? '#10B981' : pct < 85 ? '#F59E0B' : '#F43F5E'
  const remaining = Number(cat.remaining ?? (alloc - spent))
  const catKey    = cat.name?.toUpperCase()

  return (
    <div className="glass-card rounded-2xl p-4 space-y-3 hover:border-slate-600/70 transition-colors">
      <div className="flex items-center justify-between gap-2">
        <div className="flex items-center gap-2 min-w-0">
          <span className="w-2.5 h-2.5 rounded-full flex-shrink-0" style={{ background: meta.color }} />
          <span className="text-sm font-black text-slate-100 truncate">{meta.label}</span>
        </div>
        <span className="text-xs text-slate-500 flex-shrink-0 font-medium">
          {fmtRon(spent)} / {fmtRon(alloc)} RON
        </span>
      </div>

      <div className="h-2 w-full bg-slate-700/60 rounded-full overflow-hidden">
        <div
          className="h-full rounded-full transition-all duration-700"
          style={{ width: `${pct.toFixed(1)}%`, background: barColor }}
        />
      </div>

      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3 text-xs">
          <span className="text-slate-500">{pct.toFixed(0)}% used</span>
          <span className={remaining >= 0 ? 'text-emerald-400 font-bold' : 'text-rose-400 font-bold'}>
            {remaining >= 0 ? '+' : ''}{fmtRon(remaining)} left
          </span>
        </div>

        <button
          onClick={() => navigate(`/recommendations?category=${catKey}`)}
          className="flex items-center gap-1.5 rounded-xl border border-purple-500/30 bg-purple-500/10
                     px-3 py-1.5 text-[0.68rem] font-black text-purple-300
                     hover:bg-purple-500/20 hover:border-purple-400/50 hover:text-purple-200
                     transition-all active:scale-95"
          style={{ boxShadow: '0 0 10px rgba(168,85,247,0.15)' }}
        >
          <Sparkles size={11} />
          AI Deals
        </button>
      </div>
    </div>
  )
}

// ─── Main component ───────────────────────────────────────────────────────────
export default function BudgetDashboard() {
  const now = new Date()
  const [month, setMonth] = useState(now.getMonth() + 1)
  const [year,  setYear]  = useState(now.getFullYear())

  const [budget, setBudget]     = useState(null)
  const [loading, setLoading]   = useState(true)
  const [error, setError]       = useState(null)

  const [showManage,  setShowManage]  = useState(false)
  const [showAddTx,   setShowAddTx]   = useState(false)
  const [showUpload,  setShowUpload]  = useState(false)

  const [toast, setToast] = useState(null)
  const showToast = useCallback((message, type = 'success') => setToast({ message, type }), [])

  // ── Fetch budget ─────────────────────────────────────────────────────────
  const fetchBudget = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const { data } = await budgetApi.getCurrentBudget(month, year)
      setBudget(data)
    } catch (err) {
      setError(err.response?.data?.message ?? 'Could not load budget.')
    } finally {
      setLoading(false)
    }
  }, [month, year])

  useEffect(() => { fetchBudget() }, [fetchBudget])

  // ── Month navigation ──────────────────────────────────────────────────────
  const prevMonth = () => {
    if (month === 1) { setMonth(12); setYear((y) => y - 1) }
    else setMonth((m) => m - 1)
  }
  const nextMonth = () => {
    if (month === 12) { setMonth(1); setYear((y) => y + 1) }
    else setMonth((m) => m + 1)
  }

  // ── KPI derived values ────────────────────────────────────────────────────
  const leftForMonth = budget
    ? Number(budget.totalAllocated ?? 0) - Number(budget.totalSpent ?? 0)
    : 0

  const cats = budget?.categories ?? []

  return (
    <AppShell>
      <div className="max-w-2xl mx-auto px-4 pt-6 pb-28 md:pb-8 space-y-5">

        {/* ── Header ── */}
        <div className="flex items-center justify-between">
          <div>
            <p className="text-slate-500 text-sm font-medium">Monthly planner</p>
            <h1 className="text-2xl font-black text-slate-100 tracking-tight">Budget</h1>
          </div>

          {/* Month / Year selector */}
          <div className="flex items-center gap-1 glass-card rounded-2xl px-2 py-1.5">
            <button
              onClick={prevMonth}
              className="w-7 h-7 flex items-center justify-center rounded-xl text-slate-400 hover:text-slate-100 hover:bg-white/5"
            >
              <ChevronLeft size={16} />
            </button>
            <span className="text-sm font-black text-slate-100 px-2 select-none whitespace-nowrap">
              {MONTH_NAMES[month - 1]} {year}
            </span>
            <button
              onClick={nextMonth}
              className="w-7 h-7 flex items-center justify-center rounded-xl text-slate-400 hover:text-slate-100 hover:bg-white/5"
            >
              <ChevronRight size={16} />
            </button>
          </div>
        </div>

        {/* ── Fetch error ── */}
        {error && !loading && (
          <div className="flex items-center gap-3 glass-card rounded-2xl px-4 py-3 border border-rose-500/30">
            <AlertCircle size={16} className="text-rose-400 flex-shrink-0" />
            <p className="text-rose-300 text-sm flex-1">{error}</p>
            <button onClick={fetchBudget} className="text-xs text-rose-400 font-bold hover:text-rose-200">Retry</button>
          </div>
        )}

        {/* ── Safe-to-Spend hero ── */}
        <S2SCard value={budget?.safeToSpendPerDay} loading={loading} />

        {/* ── Income breakdown ── */}
        <IncomeBreakdownCard budget={budget} loading={loading} />

        {/* ── KPI row ── */}
        <div className="grid grid-cols-2 gap-3">
          <KpiCard
            icon={Wallet}
            label="Left for Month"
            value={leftForMonth}
            loading={loading}
            accent={{ border: 'border-purple-500/60', bg: 'bg-purple-500/15', text: 'text-purple-300' }}
          />
          <KpiCard
            icon={Trophy}
            label="Rollover Savings"
            value={budget?.rolloverAmount ?? 0}
            loading={loading}
            accent={{ border: 'border-amber-500/60', bg: 'bg-amber-500/15', text: 'text-amber-300' }}
          />
        </div>

        {/* ── Monthly summary strip ── */}
        {(loading || budget) && (
          <div className="glass-card rounded-2xl px-4 py-3 flex items-center justify-between gap-4">
            {loading
              ? <><Sk className="h-4 w-28" /><Sk className="h-4 w-28" /></>
              : (
                <>
                  <div className="text-center flex-1">
                    <p className="text-[0.68rem] text-slate-500 uppercase tracking-wide font-semibold mb-0.5">Income</p>
                    <p className="text-sm font-black text-slate-100">{fmtRon(budget?.totalIncome)} RON</p>
                  </div>
                  <div className="w-px h-8 bg-slate-700" />
                  <div className="text-center flex-1">
                    <p className="text-[0.68rem] text-slate-500 uppercase tracking-wide font-semibold mb-0.5">Spent</p>
                    <p className="text-sm font-black text-rose-300">{fmtRon(budget?.totalSpent)} RON</p>
                  </div>
                  <div className="w-px h-8 bg-slate-700" />
                  <div className="text-center flex-1">
                    <p className="text-[0.68rem] text-slate-500 uppercase tracking-wide font-semibold mb-0.5">Remaining</p>
                    <p className="text-sm font-black text-emerald-300">{fmtRon(budget?.totalRemaining)} RON</p>
                  </div>
                </>
              )}
          </div>
        )}

        {/* ── Categories section ── */}
        <div>
          <div className="flex items-center justify-between mb-3">
            <h2 className="text-base font-black text-slate-100">Categories</h2>
            <button
              onClick={() => setShowManage(true)}
              disabled={loading || !budget}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl glass-card text-xs font-bold text-slate-300
                         hover:border-purple-500/40 hover:text-purple-300 disabled:opacity-40 disabled:cursor-not-allowed"
            >
              <SlidersHorizontal size={13} />
              Edit Categories
            </button>
          </div>

          <div className="space-y-2.5">
            {loading
              ? Array.from({ length: 4 }).map((_, i) => <CategoryCard key={i} loading />)
              : cats.length === 0
              ? (
                <div className="glass-card rounded-2xl p-8 text-center">
                  <p className="text-slate-500 text-sm">No categories yet — click "Edit Categories" to add some.</p>
                </div>
              )
              : cats.map((cat) => <CategoryCard key={cat.id ?? cat.name} cat={cat} />)
            }
          </div>
        </div>

        {/* ── Transactions section ── */}
        <div>
          <div className="flex items-center justify-between mb-3">
            <h2 className="text-base font-black text-slate-100">Record Spending</h2>
            <div className="flex gap-2">
              <button
                onClick={() => setShowAddTx(true)}
                disabled={loading || !budget}
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-purple-500 hover:bg-purple-400 text-xs font-bold text-white
                           disabled:opacity-40 disabled:cursor-not-allowed shadow-[0_0_16px_rgba(168,85,247,0.3)]"
              >
                <Plus size={13} />
                Add Manual
              </button>
              <button
                onClick={() => setShowUpload(true)}
                disabled={loading || !budget}
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl glass-card text-xs font-bold text-slate-300
                           hover:border-purple-500/40 hover:text-purple-300 disabled:opacity-40 disabled:cursor-not-allowed"
              >
                <Upload size={13} />
                Upload CSV
              </button>
            </div>
          </div>

          {/* Category spend summary as recent activity */}
          <div className="glass-card rounded-2xl divide-y divide-slate-700/50">
            {loading
              ? Array.from({ length: 3 }).map((_, i) => (
                <div key={i} className="flex items-center justify-between px-4 py-3">
                  <Sk className="h-4 w-28" />
                  <Sk className="h-4 w-16" />
                </div>
              ))
              : cats.filter((c) => Number(c.spentAmount) > 0).length === 0
              ? (
                <div className="px-4 py-8 text-center">
                  <p className="text-slate-500 text-sm">No spending recorded yet for {MONTH_NAMES[month - 1]}.</p>
                  <p className="text-slate-600 text-xs mt-1">Add a manual transaction or upload a bank statement.</p>
                </div>
              )
              : cats
                  .filter((c) => Number(c.spentAmount) > 0)
                  .sort((a, b) => Number(b.spentAmount) - Number(a.spentAmount))
                  .map((cat) => {
                    const meta = CAT_META[cat.name?.toUpperCase()] ?? { label: cat.name, color: '#A855F7' }
                    return (
                      <div key={cat.id ?? cat.name} className="flex items-center justify-between px-4 py-3">
                        <div className="flex items-center gap-3">
                          <span className="w-2.5 h-2.5 rounded-full flex-shrink-0" style={{ background: meta.color }} />
                          <span className="text-sm font-semibold text-slate-200">{meta.label}</span>
                        </div>
                        <span className="text-sm font-bold text-rose-300">−{fmtRon(cat.spentAmount)} RON</span>
                      </div>
                    )
                  })
            }
          </div>
        </div>

        {/* ── Last updated ── */}
        {budget?.updatedAt && (
          <p className="text-center text-xs text-slate-600">
            Updated: {new Date(budget.updatedAt).toLocaleString()}
          </p>
        )}
      </div>

      {/* ── Modals ── */}
      {showManage && budget && (
        <ManageCategoriesModal
          budget={budget}
          onClose={() => setShowManage(false)}
          onRefresh={fetchBudget}
          showToast={showToast}
        />
      )}
      {showAddTx && budget && (
        <AddTransactionModal
          budget={budget}
          onClose={() => setShowAddTx(false)}
          onRefresh={fetchBudget}
          showToast={showToast}
        />
      )}
      {showUpload && budget && (
        <UploadStatementModal
          budget={budget}
          onClose={() => setShowUpload(false)}
          onRefresh={fetchBudget}
          showToast={showToast}
        />
      )}

      <Toast toast={toast} onClose={() => setToast(null)} />
    </AppShell>
  )
}
