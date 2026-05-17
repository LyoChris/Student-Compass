import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  AlertCircle,
  ArrowLeft,
  ArrowRight,
  Building2,
  Calendar,
  CheckCircle2,
  ChefHat,
  Home,
  KeyRound,
  Loader2,
  Package,
  Pizza,
  Plus,
  ReceiptText,
  Rocket,
  School,
  Sparkles,
  Trash2,
  Train,
  Utensils,
  WalletCards,
  X,
} from 'lucide-react'
import { useAuth } from '../../hooks/useAuth'
import { profileApi } from '../../api/profileApi'

const STEPS = ['Budget', 'Lifestyle', 'Launch']

const livingOptions = [
  { value: 'DORMITORY', label: 'Dormitory', description: 'Campus life, shared bills', icon: Building2 },
  { value: 'RENT', label: 'Rent', description: 'Apartment or room', icon: Home },
  { value: 'OWN_HOME', label: 'Own Home', description: 'Family place or owned', icon: KeyRound },
  { value: 'COMMUTER', label: 'Commuter', description: 'Train, bus, repeat', icon: Train },
]

const eatingOptions = [
  { value: 'COOKING', label: 'Cooking', description: 'Groceries and meal prep', icon: ChefHat },
  { value: 'CANTEEN', label: 'Canteen', description: 'Student cafeteria', icon: School },
  { value: 'DELIVERY', label: 'Delivery', description: 'Apps at midnight', icon: Pizza },
  { value: 'EATING_OUT', label: 'Eating Out', description: 'Restaurants and cafes', icon: Utensils },
]

const packageOptions = [
  { value: 'WEEKLY', label: 'Weekly', description: 'Home supplies every week', icon: Package },
  { value: 'BI_WEEKLY', label: 'Bi-weekly', description: 'Every two weeks', icon: Calendar },
  { value: 'MONTHLY', label: 'Monthly', description: 'Monthly restock', icon: ReceiptText },
  { value: 'NONE', label: 'None', description: 'Independent mode', icon: X },
]

const optionGroups = [
  { field: 'livingArea', title: 'Living Area', options: livingOptions },
  { field: 'eatingHabit', title: 'Eating Habit', options: eatingOptions },
  { field: 'homePackageFrequency', title: 'Home Packages', options: packageOptions },
]

const inputClass =
  'w-full rounded-2xl border border-white/10 bg-slate-950/40 px-4 py-3.5 text-sm text-slate-100 outline-none transition placeholder:text-slate-500 focus:border-purple-400/80 focus:ring-2 focus:ring-purple-500/30'

function SelectableTile({ option, selected, onSelect }) {
  const Icon = option.icon

  return (
    <button
      type="button"
      onClick={() => onSelect(option.value)}
      className={`group min-h-32 rounded-3xl border p-4 text-left transition active:scale-[0.98] ${
        selected
          ? 'border-purple-400 bg-purple-500/15 shadow-[0_0_22px_rgba(168,85,247,0.25)]'
          : 'border-white/10 bg-slate-800/55 hover:border-purple-300/40 hover:bg-slate-800/80'
      }`}
    >
      <div className="flex items-start justify-between gap-3">
        <div className={`flex h-11 w-11 items-center justify-center rounded-2xl ${selected ? 'bg-purple-500 text-white' : 'bg-white/5 text-slate-400 group-hover:text-purple-200'}`}>
          <Icon size={21} />
        </div>
        {selected && <CheckCircle2 size={19} className="text-purple-200" />}
      </div>
      <p className={`mt-4 text-sm font-black ${selected ? 'text-purple-100' : 'text-slate-100'}`}>{option.label}</p>
      <p className="mt-1 text-xs leading-5 text-slate-500">{option.description}</p>
    </button>
  )
}

function findLabel(options, value) {
  return options.find((option) => option.value === value)?.label || '-'
}

function toMoney(value) {
  const number = Number(value)
  return Number.isFinite(number) ? `${number.toFixed(2)} RON` : '0.00 RON'
}

export default function OnboardingWizard() {
  const navigate = useNavigate()
  const { user } = useAuth()
  const [step, setStep] = useState(0)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [form, setForm] = useState({
    monthlyBudget: '',
    fixedExpenses: [{ name: 'Rent', amount: '' }],
    livingArea: '',
    eatingHabit: '',
    homePackageFrequency: '',
  })

  const fixedExpenses = useMemo(
    () =>
      form.fixedExpenses
        .filter((expense) => expense.name.trim() && Number(expense.amount) > 0)
        .map((expense) => ({ name: expense.name.trim(), amount: Number(Number(expense.amount).toFixed(2)) })),
    [form.fixedExpenses],
  )

  const monthlyBudget = Number(form.monthlyBudget)
  const expensesTotal = fixedExpenses.reduce((total, expense) => total + expense.amount, 0)
  const remainingBudget = Number.isFinite(monthlyBudget) ? monthlyBudget - expensesTotal : 0

  const canContinue = useMemo(() => {
    if (step === 0) {
      const budgetIsValid = Number(form.monthlyBudget) > 0
      const expensesAreValid = form.fixedExpenses.every((expense) => {
        const name = expense.name.trim()
        const amount = Number(expense.amount)
        return (!name && !expense.amount) || (name && Number.isFinite(amount) && amount > 0)
      })
      return budgetIsValid && expensesAreValid
    }

    if (step === 1) {
      return Boolean(form.livingArea && form.eatingHabit && form.homePackageFrequency)
    }

    return true
  }, [form, step])

  const setBudget = (value) => {
    const normalized = Number(value) < 0 ? '0' : value
    setForm((current) => ({ ...current, monthlyBudget: normalized }))
  }

  const addExpense = () => {
    setForm((current) => ({
      ...current,
      fixedExpenses: [...current.fixedExpenses, { name: '', amount: '' }],
    }))
  }

  const removeExpense = (index) => {
    setForm((current) => ({
      ...current,
      fixedExpenses: current.fixedExpenses.filter((_, currentIndex) => currentIndex !== index),
    }))
  }

  const updateExpense = (index, field, value) => {
    const normalized = field === 'amount' && Number(value) < 0 ? '0' : value
    setForm((current) => ({
      ...current,
      fixedExpenses: current.fixedExpenses.map((expense, currentIndex) =>
        currentIndex === index ? { ...expense, [field]: normalized } : expense,
      ),
    }))
  }

  const updateChoice = (field, value) => {
    setForm((current) => ({ ...current, [field]: value }))
  }

  const goNext = () => {
    if (!canContinue) return
    setError('')
    setStep((current) => Math.min(current + 1, STEPS.length - 1))
  }

  const saveProfile = async () => {
    const userId = user?.id
    if (!userId) {
      setError('Your session is missing a user ID. Please sign in again.')
      return
    }

    setSaving(true)
    setError('')

    try {
      await profileApi.upsertProfile(userId, {
        monthlyBudget: Number(Number(form.monthlyBudget).toFixed(2)),
        livingArea: form.livingArea,
        eatingHabit: form.eatingHabit,
        homePackageFrequency: form.homePackageFrequency,
        fixedExpenses,
      })
      navigate('/dashboard', { replace: true })
    } catch (err) {
      setError(err.response?.data?.message || 'Could not save your onboarding profile. Please try again.')
    } finally {
      setSaving(false)
    }
  }

  const reviewCards = [
    { label: 'Budget', value: toMoney(form.monthlyBudget), className: 'sm:col-span-2' },
    { label: 'Fixed Expenses', value: `${fixedExpenses.length} items`, className: '' },
    { label: 'Remaining', value: toMoney(remainingBudget), className: remainingBudget < 0 ? 'text-red-200' : 'text-emerald-200' },
    { label: 'Living Area', value: findLabel(livingOptions, form.livingArea), className: '' },
    { label: 'Eating Habit', value: findLabel(eatingOptions, form.eatingHabit), className: '' },
    { label: 'Home Packages', value: findLabel(packageOptions, form.homePackageFrequency), className: 'sm:col-span-2' },
  ]

  return (
    <main className="min-h-screen overflow-hidden bg-[#0F172A] px-4 py-6 text-slate-100 sm:px-6 lg:px-8">
      <div className="pointer-events-none fixed inset-0">
        <div className="absolute right-[-12rem] top-[-18rem] h-[36rem] w-[36rem] rounded-full bg-purple-500/20 blur-3xl" />
        <div className="absolute bottom-[-18rem] left-[-12rem] h-[34rem] w-[34rem] rounded-full bg-cyan-500/10 blur-3xl" />
      </div>

      <div className="relative mx-auto flex min-h-[calc(100vh-3rem)] w-full max-w-5xl flex-col justify-center">
        <header className="mb-7 flex flex-wrap items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <img src="/logo.png" alt="StuFi" className="h-11 w-auto object-contain" draggable="false" />
            <div>
              <p className="text-xs font-semibold text-slate-500">Profile onboarding</p>
            </div>
          </div>
          <p className="rounded-full border border-white/10 bg-white/5 px-4 py-2 text-sm font-bold text-slate-300">
            Step {step + 1} of {STEPS.length}
          </p>
        </header>

        <div className="mb-8 grid grid-cols-3 gap-2">
          {STEPS.map((label, index) => (
            <div key={label}>
              <div className="h-2 overflow-hidden rounded-full bg-slate-800">
                <div
                  className="h-full rounded-full bg-purple-500 transition-all duration-500"
                  style={{ width: index <= step ? '100%' : '0%' }}
                />
              </div>
              <p className={`mt-2 text-xs font-black uppercase tracking-[0.18em] ${index <= step ? 'text-purple-200' : 'text-slate-600'}`}>
                {label}
              </p>
            </div>
          ))}
        </div>

        <section className="glass-card rounded-[2rem] p-5 shadow-2xl shadow-black/20 sm:p-8">
          <div className="mb-7 flex flex-wrap items-end justify-between gap-4">
            <div>
              <div className="mb-3 inline-flex items-center gap-2 rounded-full border border-purple-400/20 bg-purple-500/10 px-3 py-1.5 text-xs font-black uppercase tracking-[0.2em] text-purple-200">
                <Sparkles size={13} />
                {STEPS[step]}
              </div>
              <h1 className="text-2xl font-black tracking-tight sm:text-4xl">
                {step === 0 && 'Budget and fixed expenses'}
                {step === 1 && 'Student lifestyle signal'}
                {step === 2 && 'Review and launch'}
              </h1>
            </div>
            {step === 0 && (
              <div className="rounded-3xl border border-white/10 bg-slate-950/30 px-5 py-4 text-right">
                <p className="text-xs font-bold uppercase tracking-[0.18em] text-slate-500">Left after expenses</p>
                <p className={`mt-1 text-2xl font-black ${remainingBudget < 0 ? 'text-red-300' : 'text-emerald-300'}`}>{toMoney(remainingBudget)}</p>
              </div>
            )}
          </div>

          {step === 0 && (
            <div className="grid gap-5 lg:grid-cols-[0.8fr_1.2fr]">
              <div className="rounded-[1.5rem] border border-white/10 bg-slate-950/30 p-5">
                <label className="mb-2 block text-sm font-black text-slate-300">Monthly Budget</label>
                <div className="relative">
                  <WalletCards size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-purple-300" />
                  <input
                    type="number"
                    min="0"
                    step="0.01"
                    inputMode="decimal"
                    value={form.monthlyBudget}
                    onChange={(event) => setBudget(event.target.value)}
                    placeholder="1500.00"
                    className={`${inputClass} pl-12 pr-16`}
                  />
                  <span className="absolute right-4 top-1/2 -translate-y-1/2 text-sm font-black text-slate-500">RON</span>
                </div>
                {Number(form.monthlyBudget) < 0 && <p className="mt-2 text-xs text-red-300">Budget cannot be negative.</p>}
              </div>

              <div className="rounded-[1.5rem] border border-white/10 bg-slate-950/30 p-5">
                <div className="mb-4 flex items-center justify-between gap-3">
                  <div>
                    <p className="text-sm font-black text-slate-300">Fixed Expenses</p>
                    <p className="text-xs text-slate-500">Rent, subscriptions, bus pass, anything recurring.</p>
                  </div>
                  <button
                    type="button"
                    onClick={addExpense}
                    className="flex shrink-0 items-center gap-2 rounded-2xl border border-purple-400/30 bg-purple-500/10 px-3 py-2 text-xs font-black text-purple-200 hover:bg-purple-500/20"
                  >
                    <Plus size={15} />
                    Add Expense
                  </button>
                </div>

                <div className="space-y-3">
                  {form.fixedExpenses.map((expense, index) => (
                    <div key={index} className="grid gap-2 sm:grid-cols-[1fr_9rem_2.75rem]">
                      <input
                        type="text"
                        value={expense.name}
                        onChange={(event) => updateExpense(index, 'name', event.target.value)}
                        placeholder="Rent"
                        className={inputClass}
                      />
                      <input
                        type="number"
                        min="0"
                        step="0.01"
                        inputMode="decimal"
                        value={expense.amount}
                        onChange={(event) => updateExpense(index, 'amount', event.target.value)}
                        placeholder="250"
                        className={inputClass}
                      />
                      <button
                        type="button"
                        onClick={() => removeExpense(index)}
                        className="flex h-12 items-center justify-center rounded-2xl border border-white/10 text-slate-500 hover:border-red-400/30 hover:bg-red-500/10 hover:text-red-300"
                        aria-label="Remove expense"
                      >
                        <Trash2 size={17} />
                      </button>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}

          {step === 1 && (
            <div className="space-y-7">
              {optionGroups.map((group) => (
                <div key={group.field}>
                  <h2 className="mb-3 text-sm font-black text-slate-300">{group.title}</h2>
                  <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
                    {group.options.map((option) => (
                      <SelectableTile
                        key={option.value}
                        option={option}
                        selected={form[group.field] === option.value}
                        onSelect={(value) => updateChoice(group.field, value)}
                      />
                    ))}
                  </div>
                </div>
              ))}
            </div>
          )}

          {step === 2 && (
            <div className="space-y-5">
              {error && (
                <div className="flex gap-3 rounded-2xl border border-red-400/30 bg-red-500/10 p-4 text-sm text-red-200">
                  <AlertCircle size={18} className="mt-0.5 shrink-0" />
                  <span>{error}</span>
                </div>
              )}

              <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
                {reviewCards.map((card) => (
                  <div key={card.label} className={`rounded-[1.5rem] border border-white/10 bg-slate-950/35 p-5 ${card.className}`}>
                    <p className="text-xs font-black uppercase tracking-[0.18em] text-slate-500">{card.label}</p>
                    <p className="mt-3 text-xl font-black text-slate-100">{card.value}</p>
                  </div>
                ))}
              </div>

              {fixedExpenses.length > 0 && (
                <div className="rounded-[1.5rem] border border-white/10 bg-slate-950/35 p-5">
                  <p className="mb-3 text-xs font-black uppercase tracking-[0.18em] text-slate-500">Expense Breakdown</p>
                  <div className="grid gap-2 sm:grid-cols-2">
                    {fixedExpenses.map((expense) => (
                      <div key={`${expense.name}-${expense.amount}`} className="flex items-center justify-between rounded-2xl bg-white/5 px-4 py-3 text-sm">
                        <span className="font-semibold text-slate-300">{expense.name}</span>
                        <span className="font-black text-slate-100">{toMoney(expense.amount)}</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}

          <footer className="mt-8 flex flex-col-reverse gap-3 sm:flex-row">
            {step > 0 && (
              <button
                type="button"
                onClick={() => setStep((current) => current - 1)}
                className="flex items-center justify-center gap-2 rounded-2xl border border-white/10 px-5 py-3.5 text-sm font-black text-slate-300 hover:bg-white/5"
              >
                <ArrowLeft size={17} />
                Back
              </button>
            )}

            {step < STEPS.length - 1 ? (
              <button
                type="button"
                onClick={goNext}
                disabled={!canContinue}
                className="flex flex-1 items-center justify-center gap-2 rounded-2xl bg-purple-500 px-5 py-3.5 text-sm font-black text-white shadow-[0_0_30px_rgba(168,85,247,0.35)] hover:bg-purple-400 disabled:cursor-not-allowed disabled:opacity-40 disabled:shadow-none"
              >
                Continue
                <ArrowRight size={17} />
              </button>
            ) : (
              <button
                type="button"
                onClick={saveProfile}
                disabled={saving}
                className="flex flex-1 items-center justify-center gap-2 rounded-2xl bg-purple-500 px-5 py-3.5 text-sm font-black text-white shadow-[0_0_40px_rgba(168,85,247,0.55)] hover:bg-purple-400 disabled:cursor-not-allowed disabled:opacity-60"
              >
                {saving ? <Loader2 size={18} className="animate-spin" /> : <Rocket size={18} />}
                {saving ? 'Launching...' : 'Launch My Dashboard'}
              </button>
            )}
          </footer>
        </section>
      </div>
    </main>
  )
}
