import { DollarSign, CreditCard, PiggyBank } from 'lucide-react'

function Sk({ className }) {
  return <div className={`skeleton ${className}`} />
}

function fmt(n) {
  return new Intl.NumberFormat('ro-RO', { minimumFractionDigits: 0, maximumFractionDigits: 0 }).format(Number(n ?? 0))
}

export default function QuickStats({ budget, loading }) {
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

  const stats = [
    {
      label:      'Budget',
      value:      budget?.totalAllocated  ?? 0,
      Icon:       DollarSign,
      iconColor:  'text-slate-400',
      valueColor: 'text-slate-100',
    },
    {
      label:      'Spent',
      value:      budget?.totalSpent      ?? 0,
      Icon:       CreditCard,
      iconColor:  'text-orange-400',
      valueColor: 'text-orange-400',
    },
    {
      label:      'Remaining',
      value:      budget?.totalRemaining  ?? 0,
      Icon:       PiggyBank,
      iconColor:  'text-green-400',
      valueColor: 'text-green-400',
    },
  ]

  return (
    <div className="grid grid-cols-3 gap-3">
      {stats.map(({ label, value, Icon, iconColor, valueColor }) => (
        <div key={label} className="glass-card rounded-2xl p-3 text-center hover:border-purple-500/30">
          <div className="flex justify-center mb-1.5">
            <Icon size={18} className={iconColor} />
          </div>
          <p className={`text-sm font-black ${valueColor}`}>{fmt(value)}</p>
          <p className="text-[0.65rem] text-slate-600 font-medium">RON</p>
          <p className="text-xs text-slate-500 font-medium mt-0.5">{label}</p>
        </div>
      ))}
    </div>
  )
}
