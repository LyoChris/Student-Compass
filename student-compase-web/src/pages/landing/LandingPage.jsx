import { ArrowRight, Zap, ShoppingBag, MapPin, TrendingUp, Star, Download, Sparkles, Users, BadgeCheck } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useInstallPrompt } from '../../hooks/useInstallPrompt'

const BENTO_FEATURES = [
  {
    title: 'AI Financial Coach',
    desc: 'Ask anything — budgets, spending habits, saving tips. Powered by AI trained for student life.',
    icon: Zap,
    accent: 'text-purple-400',
    bg: 'bg-purple-500/10 border-purple-500/20',
    span: 'col-span-2',
  },
  {
    title: 'Campus Marketplace',
    desc: 'Buy & sell textbooks, electronics, gear. No fees.',
    icon: ShoppingBag,
    accent: 'text-blue-400',
    bg: 'bg-blue-500/10 border-blue-500/20',
    span: 'col-span-1',
  },
  {
    title: 'Radar Deals',
    desc: 'Nearby student discounts — cantina, print shops, cafes.',
    icon: MapPin,
    accent: 'text-green-400',
    bg: 'bg-green-500/10 border-green-500/20',
    span: 'col-span-1',
  },
  {
    title: 'Financial Health',
    desc: 'Weekly spending gauge, RON budget tracker, and smart alerts before you overspend.',
    icon: TrendingUp,
    accent: 'text-orange-400',
    bg: 'bg-orange-500/10 border-orange-500/20',
    span: 'col-span-2',
  },
]

const PARTNERS = [
  'Universitatea Bucuresti', 'Politehnica Bucuresti',
  'UAIC Iasi', 'UBB Cluj-Napoca', 'ASE Bucuresti', 'UVT Timisoara',
]

const STATS = [
  { value: '10k+', label: 'Students',        Icon: Users      },
  { value: 'RON',  label: 'Primary Currency', Icon: BadgeCheck },
  { value: '4.9',  label: 'App Rating',       Icon: Star       },
]

export default function LandingPage() {
  const { canInstall, promptInstall } = useInstallPrompt()

  return (
    <div className="min-h-screen bg-slate-900 overflow-x-hidden">
      <div className="fixed inset-0 pointer-events-none overflow-hidden">
        <div className="absolute -top-60 -right-60 w-[600px] h-[600px] rounded-full bg-purple-500/8 blur-3xl" />
        <div className="absolute top-1/2 -left-60 w-96 h-96 rounded-full bg-blue-500/8 blur-3xl" />
        <div className="absolute bottom-0 right-1/4 w-80 h-80 rounded-full bg-green-500/6 blur-3xl" />
      </div>

      {/* Navigation */}
      <header className="relative z-10 flex items-center justify-between px-6 py-5 max-w-6xl mx-auto">
        <div className="flex items-center">
          <img src="/logo.png" alt="StuFi" className="h-12 w-auto object-contain" draggable="false" />
        </div>
        <div className="flex items-center gap-3">
          {canInstall && (
            <button onClick={promptInstall} className="hidden sm:flex items-center gap-2 px-4 py-2 rounded-xl bg-purple-500/15 text-purple-400 text-sm font-semibold border border-purple-500/30 hover:bg-purple-500/25">
              <Download size={15} />
              Install App
            </button>
          )}
          <Link to="/login" className="px-5 py-2.5 rounded-xl bg-slate-800 text-slate-200 text-sm font-semibold border border-white/10 hover:bg-slate-700">
            Login
          </Link>
        </div>
      </header>

      {/* Hero */}
      <main className="relative z-10 max-w-6xl mx-auto px-6 pt-10 pb-24">
        <div className="flex justify-center mb-8">
          <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-purple-500/10 border border-purple-500/25 text-purple-400 text-sm font-semibold">
            <Sparkles size={14} />
            AI-Powered · Mobile-First · Made for Romania
          </div>
        </div>

        <div className="text-center mb-8">
          <h1 className="text-5xl sm:text-7xl font-black text-slate-100 leading-none tracking-tighter">
            Navigate Student Life,
            <br />
            <span className="text-transparent" style={{ backgroundImage: 'linear-gradient(135deg, #A855F7, #6366F1, #3B82F6)', backgroundClip: 'text', WebkitBackgroundClip: 'text' }}>
              Master Your Money.
            </span>
          </h1>
          <p className="mt-5 text-slate-400 text-lg sm:text-xl max-w-2xl mx-auto leading-relaxed">
            AI Financial Coach, Campus Marketplace, Local Deals.
            <br className="hidden sm:block" />
            Built for Romanian university students. Install now — works offline.
          </p>
        </div>

        <div className="flex flex-col sm:flex-row items-center justify-center gap-4 mb-16">
          <Link
            to="/register"
            className="flex items-center gap-2.5 px-8 py-4 rounded-2xl text-white font-bold text-base hover:-translate-y-0.5 active:scale-95"
            style={{ background: 'linear-gradient(135deg, #A855F7, #7C3AED)', boxShadow: '0 8px 30px rgba(168, 85, 247, 0.4)' }}
          >
            Get Started Free <ArrowRight size={18} />
          </Link>
          {canInstall ? (
            <button onClick={promptInstall} className="flex items-center gap-2.5 px-8 py-4 rounded-2xl bg-slate-800 text-slate-300 font-bold text-base border border-white/10 hover:bg-slate-700 hover:-translate-y-0.5">
              <Download size={18} /> Add to Home Screen
            </button>
          ) : (
            <Link to="/login" className="flex items-center gap-2.5 px-8 py-4 rounded-2xl bg-slate-800 text-slate-300 font-bold text-base border border-white/10 hover:bg-slate-700 hover:-translate-y-0.5">
              I have an account
            </Link>
          )}
        </div>

        {/* Bento Grid */}
        <div className="grid grid-cols-3 gap-4">
          {BENTO_FEATURES.map((feat) => {
            const Icon = feat.icon
            const isWide = feat.span === 'col-span-2'
            return (
              <div
                key={feat.title}
                className={`${isWide ? 'col-span-3 sm:col-span-2' : 'col-span-3 sm:col-span-1'} glass-card ${feat.bg} rounded-3xl p-6 hover:border-white/20 hover:-translate-y-1 cursor-default`}
              >
                <div className="mb-4">
                  <div className="w-12 h-12 rounded-2xl bg-white/8 flex items-center justify-center">
                    <Icon size={22} className={feat.accent} />
                  </div>
                </div>
                <h3 className="text-slate-100 font-bold text-lg leading-tight mb-1">{feat.title}</h3>
                <p className="text-slate-400 text-sm leading-relaxed">{feat.desc}</p>
              </div>
            )
          })}

          <div className="col-span-3 glass-card rounded-3xl p-6 hover:-translate-y-1">
            <div className="grid grid-cols-3 gap-6 text-center">
              {STATS.map(({ value, label, Icon }) => (
                <div key={label}>
                  <div className="flex justify-center mb-2"><Icon size={20} className="text-purple-400" /></div>
                  <p className="text-2xl font-black text-slate-100">{value}</p>
                  <p className="text-xs text-slate-500 font-medium mt-0.5">{label}</p>
                </div>
              ))}
            </div>
          </div>
        </div>
      </main>

      {/* Footer */}
      <footer className="relative z-10 border-t border-white/8 py-14 px-6">
        <div className="max-w-6xl mx-auto">
          <p className="text-center text-slate-400 text-sm font-semibold mb-8">
            Trusted by students at top Romanian universities
          </p>
          <div className="flex flex-wrap items-center justify-center gap-3 mb-12">
            {PARTNERS.map((partner) => (
              <div key={partner} className="px-4 py-2 rounded-2xl glass-card text-slate-400 text-sm font-medium hover:border-purple-500/30 hover:text-slate-300">
                {partner}
              </div>
            ))}
          </div>
          <div className="flex flex-col sm:flex-row items-center justify-between gap-4 pt-8 border-t border-white/8">
            <div className="flex items-center gap-2.5">
              <img src="/logo.png" alt="StuFi" className="h-8 w-auto object-contain opacity-60" draggable="false" />
              <span className="text-slate-500 text-sm">2026 StuFi. All rights reserved.</span>
            </div>
            <div className="flex items-center gap-6 text-slate-600 text-sm">
              {['Privacy', 'Terms', 'Contact', 'About'].map((link) => (
                <a key={link} href="#" className="hover:text-slate-400">{link}</a>
              ))}
            </div>
          </div>
        </div>
      </footer>
    </div>
  )
}
