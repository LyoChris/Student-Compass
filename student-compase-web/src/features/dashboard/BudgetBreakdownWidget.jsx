import { TrendingUp, Lock, Wallet, ChevronRight } from 'lucide-react'
import { NavLink } from 'react-router-dom'

function Sk({ className }) {
  return <div className={`skeleton ${className}`} />
}

function fmtRon(n) {
  return new Intl.NumberFormat('ro-RO', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(Number(n ?? 0))
}

export default function BudgetBreakdownWidget({ budget, loading }) {
  if (loading) {
    return (
      <div className="glass-card rounded-3xl p-5 space-y-3">
        <Sk className="h-4 w-32 mb-1" />
        <Sk className="h-11 w-full rounded-2xl" />
        <Sk className="h-11 w-full rounded-2xl" />
        <Sk className="h-11 w-full rounded-2xl" />
      </div>
    )
  }

  const income   = Number(budget?.totalIncome ?? 0)
  const fixed    = Number(
    budget?.fixedExpensesTotal ??
    budget?.fixedTotal ??
    (income - Number(budget?.disposableIncome ?? budget?.totalAllocated ?? 0))
  )
  const disposable = income - fixed

  return (
    <div className="glass-card rounded-3xl p-5">
      {/* Header */}
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <Wallet size={15} className="text-purple-400" />
          <p className="text-sm font-bold text-slate-100">Budget Breakdown</p>
        </div>
        <NavLink
          to="/budget"
          className="flex items-center gap-0.5 text-xs text-purple-400 font-semibold hover:text-purple-300 transition-colors"
        >
          Details <ChevronRight size={12} />
        </NavLink>
      </div>

      {/* Income */}
      <div className="flex items-center justify-between px-3.5 py-2.5 rounded-2xl bg-emerald-500/8 border border-emerald-500/15 mb-2">
        <div className="flex items-center gap-2">
          <TrendingUp size={13} className="text-emerald-400" />
          <span className="text-xs font-semibold text-slate-400">Monthly Income</span>
        </div>
        <span className="text-sm font-black text-emerald-300 tabular-nums">
          {fmtRon(income)} <span className="text-[0.65rem] text-slate-500">RON</span>
        </span>
      </div>

      {/* Fixed expenses */}
      <div className="flex items-center justify-between px-3.5 py-2.5 rounded-2xl bg-rose-500/8 border border-rose-500/15 mb-2">
        <div className="flex items-center gap-2">
          <Lock size={13} className="text-rose-400" />
          <span className="text-xs font-semibold text-slate-400">Fixed Expenses</span>
        </div>
        <span className="text-sm font-black text-rose-400 tabular-nums">
          −{fmtRon(fixed)} <span className="text-[0.65rem] text-slate-500">RON</span>
        </span>
      </div>

      {/* Disposable */}
      <div
        className="flex items-center justify-between px-3.5 py-2.5 rounded-2xl border border-purple-500/25"
        style={{ background: 'rgba(168,85,247,0.09)', boxShadow: '0 0 14px rgba(168,85,247,0.08)' }}
      >
        <div className="flex items-center gap-2">
          <Wallet size={13} className="text-purple-400" />
          <span className="text-xs font-semibold text-slate-300">Disposable</span>
        </div>
        <span
          className="text-sm font-black tabular-nums"
          style={{ color: '#A855F7' }}
        >
          {fmtRon(disposable)} <span className="text-[0.65rem] text-slate-500">RON</span>
        </span>
      </div>
    </div>
  )
}
