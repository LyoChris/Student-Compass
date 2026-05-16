import { useState, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Eye, EyeOff, AlertCircle, ChevronDown, CheckCircle2, Zap, ShoppingBag, MapPin, DollarSign, CreditCard, PiggyBank } from 'lucide-react'
import { useAuth } from '../../hooks/useAuth'
import { useCatalog } from '../../hooks/useCatalog'

function InputField({ label, children }) {
  return (
    <div>
      <label className="block text-sm font-semibold text-slate-300 mb-2">{label}</label>
      {children}
    </div>
  )
}

const INPUT_CLS =
  'w-full px-4 py-3.5 rounded-2xl bg-slate-800 border border-white/10 text-slate-100 placeholder-slate-600 text-sm outline-none focus:border-purple-500/60 focus:ring-2 focus:ring-purple-500/15 transition-all'

const SELECT_CLS =
  'w-full px-4 py-3.5 pr-10 rounded-2xl bg-slate-800 border border-white/10 text-sm outline-none focus:border-purple-500/60 focus:ring-2 focus:ring-purple-500/15 transition-all appearance-none disabled:opacity-40 disabled:cursor-not-allowed'

const FEATURE_ICONS = [
  { Icon: DollarSign, color: 'text-purple-400' },
  { Icon: ShoppingBag, color: 'text-blue-400' },
  { Icon: MapPin,      color: 'text-green-400' },
  { Icon: Zap,         color: 'text-yellow-400' },
  { Icon: CreditCard,  color: 'text-orange-400' },
  { Icon: PiggyBank,   color: 'text-pink-400' },
]

export default function RegisterPage() {
  const navigate = useNavigate()
  const { register, loading } = useAuth()
  const { cities, faculties, loadingCities, loadingFaculties, loadFaculties } = useCatalog()

  const [form, setForm] = useState({
    firstName: '', lastName: '', email: '',
    password: '', confirmPassword: '', cityId: '', facultyId: '',
  })
  const [showPass, setShowPass]       = useState(false)
  const [showConfirm, setShowConfirm] = useState(false)
  const [error, setError]             = useState('')

  useEffect(() => {
    setForm((f) => ({ ...f, facultyId: '' }))
    loadFaculties(form.cityId)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [form.cityId])

  const handleChange = (e) => {
    setForm((f) => ({ ...f, [e.target.name]: e.target.value }))
    if (error) setError('')
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (form.password !== form.confirmPassword) {
      setError('Passwords do not match. Please try again.')
      return
    }
    const result = await register(form)
    if (result.success) navigate('/onboarding')
    else setError(result.error)
  }

  const passwordStrength = (() => {
    const p = form.password
    if (!p) return null
    if (p.length < 8)  return { label: 'Too short', color: 'bg-red-500',    width: '25%'  }
    if (p.length < 12) return { label: 'Fair',       color: 'bg-yellow-500', width: '60%'  }
    return               { label: 'Strong',      color: 'bg-green-500',  width: '100%' }
  })()

  return (
    <div className="min-h-screen bg-slate-900 flex overflow-hidden">
      {/* Left decorative panel (desktop) */}
      <div className="hidden lg:flex flex-1 relative overflow-hidden items-center justify-center p-12">
        <div
          className="absolute inset-0"
          style={{ background: 'linear-gradient(135deg, rgba(99,102,241,0.12) 0%, transparent 60%, rgba(168,85,247,0.08) 100%)' }}
        />
        <div className="absolute -top-40 -left-40 w-80 h-80 rounded-full bg-blue-500/15 blur-3xl" />
        <div className="absolute -bottom-40 right-0 w-64 h-64 rounded-full bg-purple-500/10 blur-3xl" />

        <div className="relative z-10 text-center max-w-sm">
          <div
            className="w-24 h-24 rounded-3xl flex items-center justify-center mx-auto mb-8 shadow-2xl"
            style={{ background: 'linear-gradient(135deg, #6366F1, #A855F7)' }}
          >
            <span className="text-white font-black text-4xl">S</span>
          </div>
          <h2 className="text-4xl font-black text-slate-100 leading-tight tracking-tight">
            Join 10,000+<br />students.
          </h2>
          <p className="mt-4 text-slate-400 leading-relaxed">
            Take control of your finances with AI-powered insights built for Romanian university life.
          </p>
          <div className="mt-8 flex flex-wrap justify-center gap-3">
            {FEATURE_ICONS.map(({ Icon, color }, i) => (
              <div key={i} className="w-12 h-12 rounded-2xl glass-card flex items-center justify-center hover:-translate-y-1">
                <Icon size={20} className={color} />
              </div>
            ))}
          </div>
          <div className="mt-8 glass-card rounded-2xl p-4 text-left">
            {['Free forever for students', 'Bank-grade security (JWT + HttpOnly cookies)', 'Works offline as a PWA'].map((point) => (
              <div key={point} className="flex items-center gap-2.5 py-1.5">
                <CheckCircle2 size={15} className="text-green-400 flex-shrink-0" />
                <span className="text-slate-300 text-sm">{point}</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Right form panel */}
      <div className="flex-1 flex items-start justify-center p-6 overflow-y-auto">
        <div className="w-full max-w-sm py-8">
          <Link to="/" className="inline-flex items-center gap-2.5 mb-8">
            <div className="w-9 h-9 rounded-xl bg-purple-500 flex items-center justify-center shadow-lg shadow-purple-500/40">
              <span className="text-white font-black text-base">S</span>
            </div>
            <span className="text-xl font-black text-slate-100">StuFi</span>
          </Link>

          <h1 className="text-3xl font-black text-slate-100 mb-1.5 tracking-tight">Create account</h1>
          <p className="text-slate-400 text-sm mb-7">Your journey to financial freedom starts here.</p>

          {error && (
            <div className="flex items-start gap-3 p-4 rounded-2xl bg-red-500/10 border border-red-500/30 mb-6">
              <AlertCircle size={18} className="text-red-400 flex-shrink-0 mt-0.5" />
              <p className="text-red-400 text-sm leading-relaxed">{error}</p>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-2 gap-3">
              <InputField label="First name">
                <input type="text" name="firstName" value={form.firstName} onChange={handleChange} required placeholder="Ana" className={INPUT_CLS} />
              </InputField>
              <InputField label="Last name">
                <input type="text" name="lastName" value={form.lastName} onChange={handleChange} required placeholder="Popescu" className={INPUT_CLS} />
              </InputField>
            </div>

            <InputField label="Email address">
              <input type="email" name="email" value={form.email} onChange={handleChange} required placeholder="ana.popescu@student.ro" className={INPUT_CLS} />
            </InputField>

            <InputField label="City">
              <div className="relative">
                <select name="cityId" value={form.cityId} onChange={handleChange} disabled={loadingCities} className={`${SELECT_CLS} ${form.cityId ? 'text-slate-100' : 'text-slate-600'}`}>
                  <option value="">{loadingCities ? 'Loading cities...' : 'Select your city'}</option>
                  {cities.map((c) => (
                    <option key={c.id} value={c.id} className="bg-slate-800 text-slate-100">{c.name}</option>
                  ))}
                </select>
                <ChevronDown size={16} className="absolute right-4 top-1/2 -translate-y-1/2 text-slate-500 pointer-events-none" />
              </div>
            </InputField>

            <InputField label="Faculty">
              <div className="relative">
                <select name="facultyId" value={form.facultyId} onChange={handleChange} disabled={!form.cityId || loadingFaculties} className={`${SELECT_CLS} ${form.facultyId ? 'text-slate-100' : 'text-slate-600'}`}>
                  <option value="">{loadingFaculties ? 'Loading faculties...' : form.cityId ? 'Select your faculty' : 'Select a city first'}</option>
                  {faculties.map((f) => (
                    <option key={f.id} value={f.id} className="bg-slate-800 text-slate-100">{f.name}</option>
                  ))}
                </select>
                <ChevronDown size={16} className="absolute right-4 top-1/2 -translate-y-1/2 text-slate-500 pointer-events-none" />
              </div>
            </InputField>

            <InputField label="Password">
              <div className="relative">
                <input type={showPass ? 'text' : 'password'} name="password" value={form.password} onChange={handleChange} required minLength={8} placeholder="Min. 8 characters" className={`${INPUT_CLS} pr-12`} />
                <button type="button" onClick={() => setShowPass((v) => !v)} className="absolute right-4 top-1/2 -translate-y-1/2 text-slate-500 hover:text-slate-300">
                  {showPass ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
              {passwordStrength && (
                <div className="mt-2 space-y-1">
                  <div className="h-1 rounded-full bg-slate-700 overflow-hidden">
                    <div className={`h-full rounded-full transition-all ${passwordStrength.color}`} style={{ width: passwordStrength.width }} />
                  </div>
                  <p className="text-xs text-slate-500">{passwordStrength.label}</p>
                </div>
              )}
            </InputField>

            <InputField label="Confirm password">
              <div className="relative">
                <input type={showConfirm ? 'text' : 'password'} name="confirmPassword" value={form.confirmPassword} onChange={handleChange} required placeholder="Re-enter your password" className={`${INPUT_CLS} pr-12`} />
                <button type="button" onClick={() => setShowConfirm((v) => !v)} className="absolute right-4 top-1/2 -translate-y-1/2 text-slate-500 hover:text-slate-300">
                  {showConfirm ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
            </InputField>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-4 rounded-2xl text-white font-bold text-base mt-2 disabled:opacity-50 disabled:cursor-not-allowed hover:-translate-y-0.5 active:scale-95"
              style={{ background: 'linear-gradient(135deg, #A855F7, #7C3AED)', boxShadow: '0 8px 30px rgba(168, 85, 247, 0.35)' }}
            >
              {loading ? (
                <span className="flex items-center justify-center gap-2">
                  <span className="w-4 h-4 border-2 border-white/50 border-t-white rounded-full animate-spin" />
                  Creating account...
                </span>
              ) : 'Create Account'}
            </button>
          </form>

          <p className="mt-7 text-center text-slate-500 text-sm">
            Already have an account?{' '}
            <Link to="/login" className="text-purple-400 font-bold hover:text-purple-300">Sign in</Link>
          </p>
        </div>
      </div>
    </div>
  )
}
