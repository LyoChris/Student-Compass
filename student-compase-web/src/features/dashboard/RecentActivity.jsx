const TRANSACTIONS = [
  { label: 'Cantina lunch',       amount: '-RON 12',  time: 'Today, 13:24',     color: 'text-red-400',   dot: 'bg-red-500'    },
  { label: 'Book sold - Calculus', amount: '+RON 35',  time: 'Yesterday, 18:01', color: 'text-green-400', dot: 'bg-green-500'  },
  { label: 'Parents transfer',     amount: '+RON 300', time: '3 days ago',       color: 'text-green-400', dot: 'bg-purple-500' },
]

export default function RecentActivity({ loading }) {
  if (loading) return null

  return (
    <div className="glass-card rounded-3xl p-5">
      <p className="text-slate-100 font-bold text-sm mb-4">Recent Activity</p>
      <div className="space-y-3">
        {TRANSACTIONS.map((tx) => (
          <div key={tx.label} className="flex items-center gap-3">
            <div className={`w-2 h-2 rounded-full flex-shrink-0 ${tx.dot}`} />
            <div className="flex-1 min-w-0">
              <p className="text-sm font-semibold text-slate-100 truncate">{tx.label}</p>
              <p className="text-xs text-slate-500">{tx.time}</p>
            </div>
            <span className={`text-sm font-black flex-shrink-0 ${tx.color}`}>{tx.amount}</span>
          </div>
        ))}
      </div>
    </div>
  )
}
