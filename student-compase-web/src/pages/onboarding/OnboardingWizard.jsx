import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Building2, Home, Key, Train,
  ChefHat, School, Truck, Utensils,
  Repeat, RefreshCw, Calendar, X,
  Plus, Trash2, AlertCircle,
  ArrowLeft, ArrowRight, DollarSign,
  CheckCircle2, Sparkles,
} from 'lucide-react'
import { useAuth } from '../../hooks/useAuth'
import { profileApi } from '../../api/profileApi'

const STEPS = ['The Money', 'The Lifestyle', 'Review']

const LIVING_OPTS = [
  { value: 'DORMITORY', label: 'Dormitory',  icon: Building2, desc: 'University dorm'   },
  { value: 'RENT',      label: 'Renting',    icon: Home,      desc: 'Private apartment' },
  { value: 'OWN_HOME',  label: 'Own Home',   icon: Key,       desc: "Parents' house"   },
  { value: 'COMMUTER',  label: 'Commuter',   icon: Train,     desc: 'Daily commute'    },
]

const EATING_OPTS = [
  { value: 'COOKING',    label: 'Cooking',    icon: ChefHat,  desc: 'I cook at home'       },
  { value: 'CANTEEN',    label: 'Canteen',    icon: School,   desc: 'Uni cafeteria'         },
  { value: 'DELIVERY',   label: 'Delivery',   icon: Truck,    desc: 'Food delivery'         },
  { value: 'EATING_OUT', label: 'Eating Out', icon: Utensils, desc: 'Restaurants / cafes'  },
]

const PACKAGE_OPTS = [
  { value: 'WEEKLY',    label: 'Weekly',    icon: Repeat,    desc: 'Every week'    },
  { value: 'BI_WEEKLY', label: 'Bi-weekly', icon: RefreshCw, desc: 'Every 2 weeks' },
  { value: 'MONTHLY',   label: 'Monthly',   icon: Calendar,  desc: 'Once a month'  },
  { value: 'NONE',      label: 'None',      icon: X,         desc: 'No packages'   },
]

function OptionCard({ option, selected, onSelect }) {
  const Icon = option.icon
  return (
    <button
      type="button"
      onClick={() => onSelect(option.value)}
      className={`flex flex-col items-center gap-2 p-4 rounded-2xl border-2 text-center transition-all active:scale-95 ${
        selected
          ? 'border-purple-500 bg-purple-500/15'
          : 'border-white/10 bg-slate-800/60 hover:border-white/25 hover:bg-slate-800'
      }`}
    >
      <div className={`w-10 h-10 rounded-xl flex items-center justify-center ${
        selected ? 'bg-purple-500/25' : 'bg-white/8'
      }`}>
        <Icon size={20} className={selected ? 'text-purple-400' : 'text-slate-400'} />
      </div>
      <span className={`text-sm font-bold leading-tight ${selected ? 'text-purple-300' : 'text-slate-300'}`}>
        {option.label}
      </span>
      <span className="text-xs text-slate-500 leading-tight">{option.desc}</span>
    </button>
  )
}

const INPUT_CLS =
  'w-full px-4 py-3.5 rounded-2xl bg-slate-800 border border-white/10 text-slate-100 placeholder-slate-600 text-sm outline-none focus:border-purple-500/60 focus:ring-2 focus:ring-purple-500/15 transition-all'

export default function OnboardingWizard() {
  const navigate = useNavigate()
  const { user } = useAuth()

  const [step, setSt]   = useState(0)
  const [saving, setSaving] = useState(false)
  const [error, setError]   = useState('')

  const [form, setForm] = useState({
    monthlyBudget: '',
    fixedExpenses: [],
    livingArea: '',
    eatingHabit: '',
    homePackageFrequency: '',
  })

  const addExpense = () =>
    setForm((f) => ({ ...f, fixedExpenses: [...f.fixedExpenses, { name: '', amount: '' }] }))

  const removeExpense = (i) =>
    setForm((f) => ({ ...f, fixedExpenses: f.fixedExpenses.filter((_, idx) => idx !== i) }))

  const updateExpense = (i, field, value) =>
    setForm((f) => ({
      ...f,
      fixedExpenses: f.fixedExpenses.map((e, idx) => (idx === i ? { ...e, [field]: value } : e)),
    }))

  const canNext = () => {
    if (step === 0) return !!form.monthlyBudget && Number(form.monthlyBudget) > 0
    if (step === 1) return !!form.livingArea && !!form.eatingHabit && !!form.homePackageFrequency
    return true
  }

  const handleSave = async () => {
    setSaving(true)
    setError('')
    try {
      const payload = {
        monthlyBudget: Number(form.monthlyBudget),
        livingArea: form.livingArea,
        eatingHabit: form.eatingHabit,
        homePackageFrequency: form.homePackageFrequency,
        fixedExpenses: form.fixedExpenses
          .filter((e) => e.name.trim() && e.amount)
          .map((e) => ({ name: e.name.trim(), amount: Number(e.amount) })),
      }
      await profileApi.upsertProfile(user.id, payload)
      navigate('/dashboard')
    } catch (err) {
      setError(err.response?.data?.message || 'Could not save profile. Please try again.')
    } finally {
      setSaving(false)
    }
  }

  const reviewRows = [
    { label: 'Monthly Budget',   value: `${form.monthlyBudget} RON` },
    { label: 'Fixed Expenses',   value: `${form.fixedExpenses.filter((e) => e.name && e.amount).length} items` },
    { label: 'Living Area',      value: LIVING_OPTS.find((o) => o.value === form.livingArea)?.label   || '-' },
    { label: 'Eating Habit',     value: EATING_OPTS.find((o) => o.value === form.eatingHabit)?.label  || '-' },
    { label: 'Home Packages',    value: PACKAGE_OPTS.find((o) => o.value === form.homePackageFrequency)?.label || '-' },
  ]

  const validExpenses = form.fixedExpenses.filter((e) => e.name.trim() && e.amount)

  return (
    <div className="min-h-screen bg-slate-900 flex flex-col items-center justify-center p-6">
      <div className="fixed inset-0 pointer-events-none overflow-hidden">
        <div className="absolute -top-40 -right-40 w-96 h-96 rounded-full bg-purple-500/10 blur-3xl" />
        <div className="absolute bottom-0 -left-40 w-80 h-80 rounded-full bg-blue-500/8 blur-3xl" />
      </div>

      <div className="relative z-10 w-full max-w-lg">
        {/* Header */}
        <div className="flex items-center justify-between mb-8">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-xl bg-purple-500 flex items-center justify-center shadow-lg shadow-purple-500/40">
              <span className="text-white font-black text-base">S</span>
            </div>
            <span className="text-xl font-black text-slate-100">StuFi</span>
          </div>
          <span className="text-sm text-slate-500 font-medium">Step {step + 1} of {STEPS.length}</span>
        </div>

        {/* Progress bar */}
        <div className="flex gap-2 mb-8">
          {STEPS.map((_, i) => (
            <div key={i} className="flex-1 h-1.5 rounded-full overflow-hidden bg-slate-800">
              <div
                className="h-full rounded-full bg-purple-500 transition-all duration-500"
                style={{ width: i <= step ? '100%' : '0%' }}
              />
            </div>
          ))}
        </div>

        {/* Card */}
        <div className="glass-card rounded-3xl p-6 md:p-8">
          {/* Step label + title */}
          <div className="mb-6">
            <div className="flex items-center gap-1.5 mb-2">
              <Sparkles size={14} className="text-purple-400" />
              <span className="text-xs font-bold text-purple-400 uppercase tracking-widest">
                {STEPS[step]}
              </span>
            </div>
            {step === 0 && <h2 className="text-2xl font-black text-slate-100">What's your monthly budget?</h2>}
            {step === 1 && <h2 className="text-2xl font-black text-slate-100">Tell us about your lifestyle</h2>}
            {step === 2 && <h2 className="text-2xl font-black text-slate-100">Everything look right?</h2>}
          </div>

          {/* ── STEP 0: Money ── */}
          {step === 0 && (
            <div className="space-y-6">
              <div>
                <label className="block text-sm font-semibold text-slate-300 mb-2">
                  Monthly Budget
                </label>
                <div className="relative">
                  <DollarSign size={16} className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500 pointer-events-none" />
                  <input
                    type="number"
                    min="1"
                    placeholder="e.g. 1500"
                    value={form.monthlyBudget}
                    onChange={(e) => setForm((f) => ({ ...f, monthlyBudget: e.target.value }))}
                    className="w-full pl-10 pr-16 py-3.5 rounded-2xl bg-slate-800 border border-white/10 text-slate-100 placeholder-slate-600 text-sm outline-none focus:border-purple-500/60 focus:ring-2 focus:ring-purple-500/15 transition-all"
                  />
                  <span className="absolute right-4 top-1/2 -translate-y-1/2 text-slate-500 text-sm font-bold pointer-events-none">
                    RON
                  </span>
                </div>
              </div>

              <div>
                <div className="flex items-center justify-between mb-3">
                  <label className="text-sm font-semibold text-slate-300">Fixed Monthly Expenses</label>
                  <span className="text-xs text-slate-500">Optional</span>
                </div>
                <div className="space-y-2">
                  {form.fixedExpenses.map((exp, i) => (
                    <div key={i} className="flex gap-2 items-center">
                      <input
                        type="text"
                        placeholder="e.g. Netflix"
                        value={exp.name}
                        onChange={(e) => updateExpense(i, 'name', e.target.value)}
                        className="flex-1 px-3.5 py-3 rounded-xl bg-slate-800 border border-white/10 text-slate-100 placeholder-slate-600 text-sm outline-none focus:border-purple-500/60 transition-all"
                      />
                      <input
                        type="number"
                        min="0"
                        placeholder="RON"
                        value={exp.amount}
                        onChange={(e) => updateExpense(i, 'amount', e.target.value)}
                        className="w-24 px-3.5 py-3 rounded-xl bg-slate-800 border border-white/10 text-slate-100 placeholder-slate-600 text-sm outline-none focus:border-purple-500/60 transition-all"
                      />
                      <button
                        type="button"
                        onClick={() => removeExpense(i)}
                        className="w-10 h-10 flex items-center justify-center flex-shrink-0 rounded-xl text-slate-500 hover:text-red-400 hover:bg-red-500/10 transition-all"
                      >
                        <Trash2 size={15} />
                      </button>
                    </div>
                  ))}
                </div>
                <button
                  type="button"
                  onClick={addExpense}
                  className="mt-3 flex items-center gap-2 text-sm font-semibold text-purple-400 hover:text-purple-300 transition-colors"
                >
                  <Plus size={16} />
                  Add Expense
                </button>
              </div>
            </div>
          )}

          {/* ── STEP 1: Lifestyle ── */}
          {step === 1 && (
            <div className="space-y-6">
              <div>
                <p className="text-sm font-semibold text-slate-300 mb-3">Where do you live?</p>
                <div className="grid grid-cols-2 gap-2">
                  {LIVING_OPTS.map((opt) => (
                    <OptionCard
                      key={opt.value}
                      option={opt}
                      selected={form.livingArea === opt.value}
                      onSelect={(v) => setForm((f) => ({ ...f, livingArea: v }))}
                    />
                  ))}
                </div>
              </div>

              <div>
                <p className="text-sm font-semibold text-slate-300 mb-3">How do you eat?</p>
                <div className="grid grid-cols-2 gap-2">
                  {EATING_OPTS.map((opt) => (
                    <OptionCard
                      key={opt.value}
                      option={opt}
                      selected={form.eatingHabit === opt.value}
                      onSelect={(v) => setForm((f) => ({ ...f, eatingHabit: v }))}
                    />
                  ))}
                </div>
              </div>

              <div>
                <p className="text-sm font-semibold text-slate-300 mb-3">Packages from home?</p>
                <div className="grid grid-cols-2 gap-2">
                  {PACKAGE_OPTS.map((opt) => (
                    <OptionCard
                      key={opt.value}
                      option={opt}
                      selected={form.homePackageFrequency === opt.value}
                      onSelect={(v) => setForm((f) => ({ ...f, homePackageFrequency: v }))}
                    />
                  ))}
                </div>
              </div>
            </div>
          )}

          {/* ── STEP 2: Review ── */}
          {step === 2 && (
            <div className="space-y-4">
              {error && (
                <div className="flex items-start gap-3 p-4 rounded-2xl bg-red-500/10 border border-red-500/30">
                  <AlertCircle size={17} className="text-red-400 flex-shrink-0 mt-0.5" />
                  <p className="text-red-400 text-sm leading-relaxed">{error}</p>
                </div>
              )}

              <div className="space-y-2">
                {reviewRows.map(({ label, value }) => (
                  <div
                    key={label}
                    className="flex items-center justify-between px-4 py-3.5 rounded-2xl bg-slate-800/60"
                  >
                    <span className="text-sm text-slate-400">{label}</span>
                    <span className="text-sm font-bold text-slate-100">{value}</span>
                  </div>
                ))}
              </div>

              {validExpenses.length > 0 && (
                <div className="px-4 py-3 rounded-2xl bg-slate-800/40 space-y-2">
                  <p className="text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">Expense breakdown</p>
                  {validExpenses.map((e, i) => (
                    <div key={i} className="flex justify-between text-sm">
                      <span className="text-slate-400">{e.name}</span>
                      <span className="text-slate-200 font-semibold">{e.amount} RON</span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* Navigation */}
          <div className="flex gap-3 mt-8">
            {step > 0 && (
              <button
                type="button"
                onClick={() => setSt((s) => s - 1)}
                className="flex items-center justify-center gap-2 px-6 py-3.5 rounded-2xl border border-white/10 text-slate-300 font-bold text-sm hover:bg-white/5 transition-all"
              >
                <ArrowLeft size={16} />
                Back
              </button>
            )}

            {step < STEPS.length - 1 ? (
              <button
                type="button"
                disabled={!canNext()}
                onClick={() => setSt((s) => s + 1)}
                className="flex-1 flex items-center justify-center gap-2 py-3.5 rounded-2xl text-white font-bold text-sm disabled:opacity-40 disabled:cursor-not-allowed hover:-translate-y-0.5 active:scale-95 transition-all"
                style={{
                  background: 'linear-gradient(135deg, #A855F7, #7C3AED)',
                  boxShadow: canNext() ? '0 8px 25px rgba(168, 85, 247, 0.35)' : 'none',
                }}
              >
                Continue
                <ArrowRight size={16} />
              </button>
            ) : (
              <button
                type="button"
                disabled={saving}
                onClick={handleSave}
                className="flex-1 flex items-center justify-center gap-2 py-3.5 rounded-2xl text-white font-bold text-sm disabled:opacity-50 hover:-translate-y-0.5 active:scale-95 transition-all"
                style={{
                  background: 'linear-gradient(135deg, #A855F7, #7C3AED)',
                  boxShadow: '0 8px 30px rgba(168, 85, 247, 0.45)',
                }}
              >
                {saving ? (
                  <>
                    <span className="w-4 h-4 border-2 border-white/50 border-t-white rounded-full animate-spin" />
                    Saving...
                  </>
                ) : (
                  <>
                    <CheckCircle2 size={16} />
                    Save Profile
                  </>
                )}
              </button>
            )}
          </div>
        </div>

        <p className="text-center text-slate-600 text-xs mt-6">
          You can update these settings anytime from your profile page.
        </p>
      </div>
    </div>
  )
}
