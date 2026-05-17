import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Eye, EyeOff, AlertCircle, TrendingUp, ShoppingBag, MapPin, Zap } from 'lucide-react'
import { useAuth } from '../../hooks/useAuth'

const FEATURES = [
  { icon: Zap,         label: 'AI Financial Coach', color: 'text-purple-400' },
  { icon: ShoppingBag, label: 'Campus Marketplace',  color: 'text-blue-400'   },
  { icon: MapPin,      label: 'Radar Deals',          color: 'text-green-400'  },
  { icon: TrendingUp,  label: 'Financial Health',     color: 'text-orange-400' },
]

export default function LoginPage() {
  const navigate = useNavigate()
  const { login, loading } = useAuth()

  const [form, setForm]         = useState({ email: '', password: '' })
  const [showPass, setShowPass] = useState(false)
  const [error, setError]       = useState('')

  const handleChange = (e) => {
    setForm((f) => ({ ...f, [e.target.name]: e.target.value }))
    if (error) setError('')
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    const result = await login(form.email, form.password)
    if (result.success) {
      navigate('/dashboard')
    } else {
      setError(result.error)
    }
  }

  return (
    <div className="min-h-screen bg-slate-900 flex overflow-hidden">
      {/* Left decorative panel (desktop) */}
      <div className="hidden lg:flex flex-1 relative overflow-hidden items-center justify-center p-12">
        <div
          className="absolute inset-0"
          style={{
            background: 'linear-gradient(135deg, rgba(168,85,247,0.12) 0%, transparent 60%, rgba(59,130,246,0.08) 100%)',
          }}
        />
        <div className="absolute -bottom-40 -left-40 w-80 h-80 rounded-full bg-purple-500/15 blur-3xl" />
        <div className="absolute -top-40 right-0 w-64 h-64 rounded-full bg-blue-500/10 blur-3xl" />

        <div className="relative z-10 text-center max-w-sm">
          <div className="flex justify-center mb-8">
            <img
              src="/logo.png"
              alt="StuFi"
              className="h-36 w-auto object-contain drop-shadow-2xl"
              draggable="false"
            />
          </div>
          <h2 className="text-4xl font-black text-slate-100 leading-tight tracking-tight">
            Your money,<br />your rules.
          </h2>
          <p className="mt-4 text-slate-400 leading-relaxed">
            StuFi gives Romanian university students the tools to navigate finances with confidence.
          </p>
          <div className="mt-8 grid grid-cols-2 gap-3">
            {FEATURES.map(({ icon: Icon, label, color }) => (
              <div key={label} className="glass-card rounded-2xl p-3 flex items-center gap-2.5">
                <Icon size={16} className={color} />
                <span className="text-slate-300 text-xs font-semibold">{label}</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Right form panel */}
      <div className="flex-1 flex items-center justify-center p-6">
        <div className="w-full max-w-sm">
          <Link to="/" className="inline-flex items-center mb-10">
            <img
              src="/logo.png"
              alt="StuFi"
              className="h-12 w-auto object-contain"
              draggable="false"
            />
          </Link>

          <h1 className="text-3xl font-black text-slate-100 mb-1.5 tracking-tight">Welcome back</h1>
          <p className="text-slate-400 text-sm mb-8">Sign in to your student account.</p>

          {error && (
            <div className="flex items-start gap-3 p-4 rounded-2xl bg-red-500/10 border border-red-500/30 mb-6">
              <AlertCircle size={18} className="text-red-400 flex-shrink-0 mt-0.5" />
              <p className="text-red-400 text-sm leading-relaxed">{error}</p>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-semibold text-slate-300 mb-2">Email address</label>
              <input
                type="email"
                name="email"
                value={form.email}
                onChange={handleChange}
                required
                placeholder="ana.popescu@student.ro"
                className="w-full px-4 py-3.5 rounded-2xl bg-slate-800 border border-white/10 text-slate-100 placeholder-slate-600 text-sm outline-none focus:border-purple-500/60 focus:ring-2 focus:ring-purple-500/15"
              />
            </div>

            <div>
              <label className="block text-sm font-semibold text-slate-300 mb-2">Password</label>
              <div className="relative">
                <input
                  type={showPass ? 'text' : 'password'}
                  name="password"
                  value={form.password}
                  onChange={handleChange}
                  required
                  placeholder="••••••••"
                  className="w-full px-4 py-3.5 pr-12 rounded-2xl bg-slate-800 border border-white/10 text-slate-100 placeholder-slate-600 text-sm outline-none focus:border-purple-500/60 focus:ring-2 focus:ring-purple-500/15"
                />
                <button
                  type="button"
                  onClick={() => setShowPass((v) => !v)}
                  className="absolute right-4 top-1/2 -translate-y-1/2 text-slate-500 hover:text-slate-300"
                  aria-label={showPass ? 'Hide password' : 'Show password'}
                >
                  {showPass ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-4 rounded-2xl text-white font-bold text-base mt-2 disabled:opacity-50 disabled:cursor-not-allowed hover:-translate-y-0.5 active:scale-95"
              style={{
                background: loading ? '#7C3AED' : 'linear-gradient(135deg, #A855F7, #7C3AED)',
                boxShadow: '0 8px 30px rgba(168, 85, 247, 0.35)',
              }}
            >
              {loading ? (
                <span className="flex items-center justify-center gap-2">
                  <span className="w-4 h-4 border-2 border-white/50 border-t-white rounded-full animate-spin" />
                  Signing in...
                </span>
              ) : 'Login'}
            </button>
          </form>

          <p className="mt-7 text-center text-slate-500 text-sm">
            Don&apos;t have an account?{' '}
            <Link to="/register" className="text-purple-400 font-bold hover:text-purple-300">
              Create one for free
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}
