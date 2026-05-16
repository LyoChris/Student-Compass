import { TrendingUp } from 'lucide-react'

function Sk({ className }) {
  return <div className={`skeleton ${className}`} />
}

export default function HealthGauge({ percentage, loading }) {
  if (loading) {
    return (
      <div className="glass-card rounded-3xl p-5">
        <div className="flex items-center justify-between mb-4">
          <Sk className="h-3.5 w-32" />
          <Sk className="h-6 w-20 rounded-full" />
        </div>
        <div className="flex items-center gap-6">
          <Sk className="w-28 h-28 rounded-full flex-shrink-0" />
          <div className="flex-1 space-y-3">
            <Sk className="h-7 w-28" />
            <Sk className="h-4 w-44" />
            <Sk className="h-4 w-36" />
          </div>
        </div>
      </div>
    )
  }

  const radius        = 44
  const circumference = 2 * Math.PI * radius
  const offset        = circumference - (percentage / 100) * circumference

  const { gaugeColor, badgeColor, label } =
    percentage >= 75
      ? { gaugeColor: '#22C55E', badgeColor: 'bg-green-500/20 text-green-400',   label: 'Good Job!'    }
      : percentage >= 50
      ? { gaugeColor: '#F59E0B', badgeColor: 'bg-yellow-500/20 text-yellow-400', label: 'Stay on track' }
      : { gaugeColor: '#EF4444', badgeColor: 'bg-red-500/20 text-red-400',       label: 'Be careful!'  }

  return (
    <div className="glass-card rounded-3xl p-5 hover:border-purple-500/30">
      <div className="flex items-center justify-between mb-4">
        <p className="text-slate-400 text-sm font-semibold">Financial Health</p>
        <span className={`text-xs font-bold px-2.5 py-1 rounded-full ${badgeColor}`}>
          This week
        </span>
      </div>
      <div className="flex items-center gap-6">
        <div className="relative w-28 h-28 flex-shrink-0">
          <svg
            width="112"
            height="112"
            viewBox="0 0 112 112"
            style={{ transform: 'rotate(-90deg)' }}
          >
            <circle
              cx="56" cy="56" r={radius}
              fill="none"
              stroke="rgba(255,255,255,0.07)"
              strokeWidth="10"
            />
            <circle
              cx="56" cy="56" r={radius}
              fill="none"
              stroke={gaugeColor}
              strokeWidth="10"
              strokeLinecap="round"
              strokeDasharray={circumference}
              strokeDashoffset={offset}
              style={{ transition: 'stroke-dashoffset 1.4s cubic-bezier(0.4,0,0.2,1)' }}
            />
          </svg>
          <div className="absolute inset-0 flex flex-col items-center justify-center">
            <span className="text-2xl font-black text-slate-100 leading-none">{percentage}%</span>
          </div>
        </div>

        <div className="flex-1 min-w-0">
          <p className="text-xl font-black text-slate-100 leading-tight">{label}</p>
          <p className="text-slate-400 text-sm mt-1.5 leading-relaxed">
            You have <span className="text-green-400 font-bold">RON 250</span> left this week.
          </p>
          <div className="flex items-center gap-1.5 mt-3">
            <TrendingUp size={13} className="text-green-400" />
            <span className="text-xs text-green-400 font-semibold">+12% vs last week</span>
          </div>
        </div>
      </div>
    </div>
  )
}
