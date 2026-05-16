import { DollarSign, CreditCard, PiggyBank, ArrowUpRight, ArrowDownRight } from 'lucide-react'

function Sk({ className }) {
  return <div className={`skeleton ${className}`} />
}

const STATS = [
  { label: 'Budget', value: 'RON 800', Icon: DollarSign, delta: null, iconColor: 'text-slate-400',  valueColor: 'text-slate-100'  },
  { label: 'Spent',  value: 'RON 550', Icon: CreditCard, delta: -3,   iconColor: 'text-orange-400', valueColor: 'text-orange-400' },
  { label: 'Saved',  value: 'RON 120', Icon: PiggyBank,  delta: +18,  iconColor: 'text-green-400',  valueColor: 'text-green-400'  },
]

export default function QuickStats({ loading }) {
  if (loading) {
    return (
      <div className="grid grid-cols-3 gap-3">
        {[1, 2, 3].map((i) => (
          <div key={i} className="glass-card rounded-2xl p-3">
            <Sk className="h-5 w-5 rounded mx-auto mb-2" />
            <Sk className="h-5 w-16 mx-auto mb-1" />
            <Sk className="h-3 w-12 mx-auto" />
          </div>
        ))}
      </div>
    )
  }

  return (
    <div className="grid grid-cols-3 gap-3">
      {STATS.map(({ label, value, Icon, delta, iconColor, valueColor }) => (
        <div
          key={label}
          className="glass-card rounded-2xl p-3 text-center hover:border-purple-500/30"
        >
          <div className="flex justify-center mb-1.5">
            <Icon size={18} className={iconColor} />
          </div>
          <p className={`text-sm font-black ${valueColor}`}>{value}</p>
          <p className="text-xs text-slate-500 font-medium mt-0.5">{label}</p>
          {delta !== null && (
            <div className={`flex items-center justify-center gap-0.5 mt-1 text-xs font-bold ${delta >= 0 ? 'text-green-400' : 'text-red-400'}`}>
              {delta >= 0 ? <ArrowUpRight size={11} /> : <ArrowDownRight size={11} />}
              {Math.abs(delta)}%
            </div>
          )}
        </div>
      ))}
    </div>
  )
}
