import { useState, useEffect, useCallback } from 'react'
import { Link } from 'react-router-dom'
import {
  Building2, Home, Key, Train,
  ChefHat, School, Truck, Utensils,
  Repeat, RefreshCw, Calendar, X,
  Plus, Trash2, AlertCircle, Pencil,
  CheckCircle2, DollarSign, User,
  TrendingUp, Package, Lock, ArrowRight,
} from 'lucide-react'
import AppShell from '../../components/layout/AppShell'
import { useAuth } from '../../hooks/useAuth'
import { profileApi } from '../../api/profileApi'

// ── Option metadata (must match backend enums) ──────────────────────────────
const LIVING_OPTS = [
  { value: 'DORMITORY', label: 'Dormitory',  icon: Building2, desc: 'University dorm'  },
  { value: 'RENT',      label: 'Renting',    icon: Home,      desc: 'Private apartment'},
  { value: 'OWN_HOME',  label: 'Own Home',   icon: Key,       desc: "Parents' house"  },
  { value: 'COMMUTER',  label: 'Commuter',   icon: Train,     desc: 'Daily commute'   },
]
const EATING_OPTS = [
  { value: 'COOKING',    label: 'Cooking',    icon: ChefHat,  desc: 'I cook at home'     },
  { value: 'CANTEEN',    label: 'Canteen',    icon: School,   desc: 'Uni cafeteria'       },
  { value: 'DELIVERY',   label: 'Delivery',   icon: Truck,    desc: 'Food delivery'       },
  { value: 'EATING_OUT', label: 'Eating Out', icon: Utensils, desc: 'Restaurants / cafes' },
]
const PACKAGE_OPTS = [
  { value: 'WEEKLY',    label: 'Weekly',    icon: Repeat,    desc: 'Every week'    },
  { value: 'BI_WEEKLY', label: 'Bi-weekly', icon: RefreshCw, desc: 'Every 2 weeks' },
  { value: 'MONTHLY',   label: 'Monthly',   icon: Calendar,  desc: 'Once a month'  },
  { value: 'NONE',      label: 'None',      icon: X,         desc: 'No packages'   },
]

const label = (opts, value) => opts.find((o) => o.value === value)?.label ?? value

// ── Shared selectable card ───────────────────────────────────────────────────
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

// ── Skeleton card ────────────────────────────────────────────────────────────
function SkeletonCard({ rows = 3 }) {
  return (
    <div className="glass-card rounded-3xl p-5 space-y-3">
      <div className="skeleton h-4 w-32 rounded-lg" />
      {Array.from({ length: rows }).map((_, i) => (
        <div key={i} className="skeleton h-12 rounded-2xl" />
      ))}
    </div>
  )
}

// ── Main component ───────────────────────────────────────────────────────────
export default function ProfilePage() {
  const { user } = useAuth()

  const [profile,  setProfile]  = useState(null)
  const [loading,  setLoading]  = useState(true)
  const [notFound, setNotFound] = useState(false)
  const [editing,  setEditing]  = useState(false)
  const [saving,   setSaving]   = useState(false)
  const [error,    setError]    = useState('')

  const [form, setForm] = useState({
    monthlyBudget: '',
    fixedExpenses: [],
    livingArea: '',
    eatingHabit: '',
    homePackageFrequency: '',
  })

  const populateForm = useCallback((data) => {
    setForm({
      monthlyBudget:       String(data.monthlyBudget),
      fixedExpenses:       data.fixedExpenses.map((e) => ({ name: e.name, amount: String(e.amount) })),
      livingArea:          data.livingArea,
      eatingHabit:         data.eatingHabit,
      homePackageFrequency: data.homePackageFrequency,
    })
  }, [])

  useEffect(() => {
    if (!user?.id) return
    profileApi.getProfile(user.id)
      .then(({ data }) => {
        setProfile(data)
        populateForm(data)
      })
      .catch((err) => {
        if (err.response?.status === 404) setNotFound(true)
      })
      .finally(() => setLoading(false))
  }, [user?.id, populateForm])

  const addExpense = () =>
    setForm((f) => ({ ...f, fixedExpenses: [...f.fixedExpenses, { name: '', amount: '' }] }))

  const removeExpense = (i) =>
    setForm((f) => ({ ...f, fixedExpenses: f.fixedExpenses.filter((_, idx) => idx !== i) }))

  const updateExpense = (i, field, value) =>
    setForm((f) => ({
      ...f,
      fixedExpenses: f.fixedExpenses.map((e, idx) => (idx === i ? { ...e, [field]: value } : e)),
    }))

  const handleSave = async () => {
    setSaving(true)
    setError('')
    try {
      const payload = {
        monthlyBudget:       Number(form.monthlyBudget),
        livingArea:          form.livingArea,
        eatingHabit:         form.eatingHabit,
        homePackageFrequency: form.homePackageFrequency,
        fixedExpenses: form.fixedExpenses
          .filter((e) => e.name.trim() && e.amount)
          .map((e) => ({ name: e.name.trim(), amount: Number(e.amount) })),
      }
      const { data } = await profileApi.upsertProfile(user.id, payload)
      setProfile(data)
      populateForm(data)
      setEditing(false)
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to save. Please try again.')
    } finally {
      setSaving(false)
    }
  }

  const handleCancel = () => {
    if (profile) populateForm(profile)
    setEditing(false)
    setError('')
  }

  const initials = [user?.firstName?.[0], user?.lastName?.[0]].filter(Boolean).join('').toUpperCase() || '?'

  // ── Loading ──
  if (loading) {
    return (
      <AppShell>
        <div className="max-w-2xl mx-auto px-4 pt-6 pb-28 md:pb-8 space-y-4">
          <div className="skeleton h-8 w-40 rounded-xl" />
          <SkeletonCard rows={2} />
          <SkeletonCard rows={3} />
        </div>
      </AppShell>
    )
  }

  // ── No profile yet ──
  if (notFound) {
    return (
      <AppShell>
        <div className="max-w-2xl mx-auto px-4 pt-6 pb-28 md:pb-8 space-y-4">
          <h1 className="text-2xl font-black text-slate-100 tracking-tight">Profile</h1>
          <div className="glass-card rounded-3xl p-10 flex flex-col items-center text-center">
            <div className="w-16 h-16 rounded-2xl bg-purple-500/20 flex items-center justify-center mb-4">
              <User size={28} className="text-purple-400" />
            </div>
            <h2 className="text-xl font-black text-slate-100 mb-2">No profile yet</h2>
            <p className="text-slate-400 text-sm leading-relaxed max-w-xs mb-6">
              Complete a quick onboarding so StuFi can personalise your financial insights.
            </p>
            <Link
              to="/onboarding"
              className="flex items-center gap-2 px-6 py-3 rounded-2xl text-white font-bold text-sm hover:-translate-y-0.5 active:scale-95 transition-all"
              style={{ background: 'linear-gradient(135deg, #A855F7, #7C3AED)', boxShadow: '0 8px 25px rgba(168,85,247,0.4)' }}
            >
              Complete Onboarding
            </Link>
          </div>
        </div>
      </AppShell>
    )
  }

  // ── View mode ──
  if (!editing) {
    return (
      <AppShell>
        <div className="max-w-2xl mx-auto px-4 pt-6 pb-28 md:pb-8 space-y-4">
          {/* Header */}
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-2xl font-black text-slate-100 tracking-tight">Profile</h1>
              <p className="text-slate-500 text-sm">Your financial settings</p>
            </div>
            <button
              onClick={() => setEditing(true)}
              className="flex items-center gap-2 px-4 py-2 rounded-2xl glass-card text-purple-400 text-sm font-bold border-purple-500/30 hover:bg-purple-500/10 transition-all"
            >
              <Pencil size={14} />
              Edit
            </button>
          </div>

          {/* User card */}
          <div className="glass-card rounded-3xl p-5 flex items-center gap-4">
            <div className="w-14 h-14 rounded-2xl bg-purple-500/20 border border-purple-500/40 flex items-center justify-center flex-shrink-0">
              <span className="text-purple-400 font-black text-xl">{initials}</span>
            </div>
            <div className="min-w-0">
              <p className="text-lg font-black text-slate-100">
                {user?.firstName} {user?.lastName}
              </p>
              <p className="text-sm text-slate-500 truncate">{user?.email}</p>
              {user?.facultyName && (
                <p className="text-xs text-slate-600 mt-0.5 truncate">{user.facultyName}</p>
              )}
            </div>
          </div>

          {/* Budget */}
          <div className="glass-card rounded-3xl p-5">
            <div className="flex items-center gap-2 mb-4">
              <TrendingUp size={16} className="text-green-400" />
              <span className="text-sm font-bold text-slate-100">Monthly Income</span>
            </div>
            <p className="text-4xl font-black text-slate-100 mb-1">
              {profile?.monthlyBudget?.toLocaleString('ro-RO')}
              <span className="text-xl text-slate-400 ml-2">RON</span>
            </p>
            {profile?.fixedExpenses?.length > 0 && (
              <p className="text-xs text-slate-500 mt-1">
                Disposable after fixed expenses:{' '}
                <span className="text-purple-400 font-bold">
                  {(profile.monthlyBudget - profile.fixedExpenses.reduce((s, e) => s + e.amount, 0)).toLocaleString('ro-RO')} RON
                </span>
              </p>
            )}
          </div>

          {/* Fixed Monthly Expenses */}
          <div className="glass-card rounded-3xl p-5">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-2">
                <div className="w-7 h-7 rounded-xl bg-rose-500/15 flex items-center justify-center">
                  <Lock size={13} className="text-rose-400" />
                </div>
                <span className="text-sm font-bold text-slate-100">Fixed Monthly Expenses</span>
              </div>
              <button
                onClick={() => setEditing(true)}
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl glass-card text-[0.68rem] font-bold text-purple-400
                           border-purple-500/30 hover:bg-purple-500/10 transition-all"
              >
                <Pencil size={11} />
                Edit
              </button>
            </div>

            {profile?.fixedExpenses?.length > 0 ? (
              <>
                <div className="space-y-2">
                  {profile.fixedExpenses.map((e, i) => (
                    <div key={i} className="flex items-center justify-between px-4 py-3 rounded-2xl bg-slate-800/60 border border-white/5">
                      <div className="flex items-center gap-2.5">
                        <span className="w-2 h-2 rounded-full bg-rose-400/60 flex-shrink-0" />
                        <span className="text-sm text-slate-300">{e.name}</span>
                      </div>
                      <span className="text-sm font-black text-rose-300 tabular-nums">
                        {Number(e.amount).toLocaleString('ro-RO')} <span className="text-xs font-bold text-slate-500">RON</span>
                      </span>
                    </div>
                  ))}
                </div>

                {/* Total row */}
                <div className="flex items-center justify-between px-4 py-3 mt-2 rounded-2xl border border-rose-500/20"
                  style={{ background: 'rgba(244,63,94,0.07)' }}>
                  <span className="text-xs font-black text-slate-400 uppercase tracking-wider">Total Fixed</span>
                  <span className="text-sm font-black text-rose-400 tabular-nums">
                    {profile.fixedExpenses.reduce((s, e) => s + e.amount, 0).toLocaleString('ro-RO')}{' '}
                    <span className="text-xs font-bold text-slate-500">RON / mo</span>
                  </span>
                </div>

                <p className="text-[0.62rem] text-slate-600 mt-3 text-center leading-relaxed">
                  These are automatically deducted from your income before budget categories are calculated.
                </p>
              </>
            ) : (
              <div className="flex flex-col items-center py-4 gap-3 text-center">
                <p className="text-sm text-slate-500">No fixed expenses set yet.</p>
                <button
                  onClick={() => setEditing(true)}
                  className="flex items-center gap-1.5 text-sm font-bold text-purple-400 hover:text-purple-300 transition-colors"
                >
                  <Plus size={14} />
                  Add rent, subscriptions…
                  <ArrowRight size={13} />
                </button>
              </div>
            )}
          </div>

          {/* Lifestyle */}
          <div className="glass-card rounded-3xl p-5">
            <div className="flex items-center gap-2 mb-4">
              <Package size={16} className="text-blue-400" />
              <span className="text-sm font-bold text-slate-100">Lifestyle</span>
            </div>
            <div className="space-y-3">
              {[
                { label: 'Living',         value: label(LIVING_OPTS,  profile?.livingArea)          },
                { label: 'Eating habit',   value: label(EATING_OPTS,  profile?.eatingHabit)         },
                { label: 'Home packages',  value: label(PACKAGE_OPTS, profile?.homePackageFrequency) },
              ].map(({ label: lbl, value }) => (
                <div key={lbl} className="flex items-center justify-between px-4 py-3 rounded-2xl bg-slate-800/60">
                  <span className="text-sm text-slate-400">{lbl}</span>
                  <span className="text-sm font-bold text-slate-100">{value}</span>
                </div>
              ))}
            </div>
          </div>

          {/* Timestamps */}
          {profile?.updatedAt && (
            <p className="text-xs text-slate-600 text-center">
              Last updated {new Date(profile.updatedAt).toLocaleDateString('ro-RO', { day: '2-digit', month: 'long', year: 'numeric' })}
            </p>
          )}
        </div>
      </AppShell>
    )
  }

  // ── Edit mode ──
  return (
    <AppShell>
      <div className="max-w-2xl mx-auto px-4 pt-6 pb-28 md:pb-8 space-y-4">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-black text-slate-100 tracking-tight">Edit Profile</h1>
            <p className="text-slate-500 text-sm">Update your financial settings</p>
          </div>
          <button
            onClick={handleCancel}
            className="px-4 py-2 rounded-2xl glass-card text-slate-400 text-sm font-bold hover:bg-white/5 transition-all"
          >
            Cancel
          </button>
        </div>

        {error && (
          <div className="flex items-start gap-3 p-4 rounded-2xl bg-red-500/10 border border-red-500/30">
            <AlertCircle size={17} className="text-red-400 flex-shrink-0 mt-0.5" />
            <p className="text-red-400 text-sm leading-relaxed">{error}</p>
          </div>
        )}

        {/* Budget */}
        <div className="glass-card rounded-3xl p-5 space-y-5">
          <p className="text-sm font-bold text-slate-100">Monthly Budget</p>
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
            <span className="absolute right-4 top-1/2 -translate-y-1/2 text-slate-500 text-sm font-bold pointer-events-none">RON</span>
          </div>

          {/* Fixed expenses */}
          <div>
            <div className="flex items-center justify-between mb-3">
              <p className="text-sm font-semibold text-slate-300">Fixed Expenses</p>
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

        {/* Living area */}
        <div className="glass-card rounded-3xl p-5">
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

        {/* Eating habit */}
        <div className="glass-card rounded-3xl p-5">
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

        {/* Home packages */}
        <div className="glass-card rounded-3xl p-5">
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

        {/* Save button */}
        <button
          type="button"
          disabled={saving}
          onClick={handleSave}
          className="w-full flex items-center justify-center gap-2 py-4 rounded-2xl text-white font-bold text-base disabled:opacity-50 hover:-translate-y-0.5 active:scale-95 transition-all"
          style={{
            background: 'linear-gradient(135deg, #A855F7, #7C3AED)',
            boxShadow: '0 8px 30px rgba(168, 85, 247, 0.4)',
          }}
        >
          {saving ? (
            <>
              <span className="w-4 h-4 border-2 border-white/50 border-t-white rounded-full animate-spin" />
              Saving...
            </>
          ) : (
            <>
              <CheckCircle2 size={18} />
              Save Changes
            </>
          )}
        </button>
      </div>
    </AppShell>
  )
}
