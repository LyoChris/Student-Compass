import { ReceiptText, ShoppingCart } from 'lucide-react'

const PALETTE = [
  '#A855F7', '#22D3EE', '#F59E0B', '#34D399',
  '#F472B6', '#60A5FA', '#4ADE80', '#FB923C',
]

function Sk({ className }) {
  return <div className={`skeleton ${className}`} />
}

function fmt(n) {
  return new Intl.NumberFormat('ro-RO', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(Number(n ?? 0))
}

function timeLabel(isoString) {
  if (!isoString) return ''
  const d = new Date(isoString)
  return d.toLocaleTimeString('ro-RO', { hour: '2-digit', minute: '2-digit' })
}

export default function RecentActivity({ spendToday, loading }) {
  if (loading) {
    return (
      <div className="glass-card rounded-3xl p-5">
        <Sk className="h-4 w-40 mb-4" />
        <div className="space-y-3">
          {[1, 2, 3].map((i) => (
            <div key={i} className="flex items-center gap-3">
              <Sk className="w-2 h-2 rounded-full flex-shrink-0" />
              <Sk className="h-4 flex-1" />
              <Sk className="h-4 w-20" />
            </div>
          ))}
        </div>
      </div>
    )
  }

  const transactions = spendToday?.transactions ?? []
  const totalToday   = Number(spendToday?.totalToday ?? 0)

  if (transactions.length === 0) {
    return (
      <div className="glass-card rounded-3xl p-5 flex flex-col items-center text-center py-8 gap-2">
        <ShoppingCart size={24} className="text-slate-600" />
        <p className="text-slate-500 text-sm font-semibold">Nothing spent today</p>
        <p className="text-slate-600 text-xs">Transactions you log will appear here.</p>
      </div>
    )
  }

  return (
    <div className="glass-card rounded-3xl p-5">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <ReceiptText size={15} className="text-purple-400" />
          <p className="text-slate-100 font-bold text-sm">Today's Spending</p>
        </div>
        <span className="text-xs font-black text-rose-300">−{fmt(totalToday)} RON</span>
      </div>
      <div className="space-y-3">
        {transactions.map((tx, i) => (
          <div key={i} className="flex items-center gap-3">
            <div
              className="w-2 h-2 rounded-full flex-shrink-0"
              style={{ background: PALETTE[i % PALETTE.length] }}
            />
            <div className="flex-1 min-w-0">
              <p className="text-sm font-semibold text-slate-100 truncate">
                {tx.description || tx.categoryName}
              </p>
              <p className="text-xs text-slate-500">{tx.categoryName} · {timeLabel(tx.transactionDate)}</p>
            </div>
            <span className="text-sm font-black text-rose-300 flex-shrink-0">
              −{fmt(tx.amount)} RON
            </span>
          </div>
        ))}
      </div>
    </div>
  )
}
