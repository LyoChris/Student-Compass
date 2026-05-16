import { useState, useEffect, useCallback, useRef } from 'react'
import { MapContainer, TileLayer, CircleMarker, Tooltip, useMapEvents, useMap } from 'react-leaflet'
import 'leaflet/dist/leaflet.css'
import {
  Plus, Map as MapIcon, List, Loader2, RefreshCw,
  AlertCircle, ThumbsUp, ThumbsDown, MapPin,
} from 'lucide-react'
import AppShell from '../../components/layout/AppShell'
import { radarApi } from '../../api/radarApi'
import DealBottomSheet from './DealBottomSheet'
import DealFullDetailsModal from './DealFullDetailsModal'
import CreateDealModal from './CreateDealModal'

// ── Constants ─────────────────────────────────────────────────────────────────
const DARK_TILES   = 'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png'
const ATTRIBUTION  = '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> &copy; <a href="https://carto.com/attributions">CARTO</a>'
const DEFAULT_CENTER = [44.4268, 26.1025]
const RADIUS_KM    = 5

const CATEGORIES = ['ALL', 'FOOD', 'HOME', 'SOCIAL', 'TECH', 'OTHER']

const CATEGORY_META = {
  ALL:    { color: '#A855F7', bg: 'bg-purple-500/15', text: 'text-purple-300', border: 'border-purple-500/30' },
  FOOD:   { color: '#F59E0B', bg: 'bg-amber-500/15',  text: 'text-amber-300',  border: 'border-amber-500/30'  },
  HOME:   { color: '#60A5FA', bg: 'bg-blue-500/15',   text: 'text-blue-300',   border: 'border-blue-500/30'   },
  SOCIAL: { color: '#F472B6', bg: 'bg-pink-500/15',   text: 'text-pink-300',   border: 'border-pink-500/30'   },
  TECH:   { color: '#22D3EE', bg: 'bg-cyan-500/15',   text: 'text-cyan-300',   border: 'border-cyan-500/30'   },
  OTHER:  { color: '#A855F7', bg: 'bg-purple-500/15', text: 'text-purple-300', border: 'border-purple-500/30' },
}


function timeLeft(expiresAt) {
  if (!expiresAt) return ''
  const diff = new Date(expiresAt) - Date.now()
  if (diff <= 0) return 'Expired'
  const h = Math.floor(diff / 3600000)
  const m = Math.floor((diff % 3600000) / 60000)
  if (h >= 24) return `${Math.floor(h / 24)}d left`
  return h > 0 ? `${h}h left` : `${m}m left`
}

// ── Map sub-components ────────────────────────────────────────────────────────
function MapCenterTracker({ onCenter }) {
  useMapEvents({
    moveend(e) {
      const { lat, lng } = e.target.getCenter()
      onCenter({ lat, lng })
    },
  })
  return null
}

function FlyTo({ target }) {
  const map = useMap()
  useEffect(() => {
    if (target) map.flyTo([target.lat, target.lng], 14, { animate: true, duration: 1.2 })
  }, [target, map])
  return null
}

// ── Deal list card ────────────────────────────────────────────────────────────
function DealListCard({ deal, onSelect }) {
  const meta  = CATEGORY_META[deal.category] ?? CATEGORY_META.OTHER
  const ttl   = timeLeft(deal.expiresAt)
  const score = Number(deal.upvotes ?? 0) - Number(deal.downvotes ?? 0)

  return (
    <button
      onClick={() => onSelect(deal)}
      className="glass-card rounded-2xl p-4 text-left w-full hover:border-white/20 active:scale-[0.99] transition-all"
    >
      <div className="flex items-start gap-3">
        <div
          className="w-3 h-3 rounded-full flex-shrink-0 mt-1.5"
          style={{ background: meta.color, boxShadow: `0 0 8px ${meta.color}` }}
        />
        <div className="flex-1 min-w-0">
          <div className="flex items-center justify-between gap-2 mb-0.5">
            <span className={`text-[0.65rem] font-black uppercase tracking-wider ${meta.text}`}>
              {deal.category}
            </span>
            <span className={`text-xs font-bold ${ttl === 'Expired' ? 'text-rose-400' : 'text-emerald-400'}`}>
              {ttl}
            </span>
          </div>
          <h3 className="text-sm font-black text-slate-100 truncate">{deal.title}</h3>
          <div className="flex items-center gap-3 mt-1.5 text-xs text-slate-500">
            <span className="flex items-center gap-1 text-emerald-400">
              <ThumbsUp size={11} /> {deal.upvotes ?? 0}
            </span>
            <span className="flex items-center gap-1 text-rose-400">
              <ThumbsDown size={11} /> {deal.downvotes ?? 0}
            </span>
            <span className={`font-bold ${score >= 0 ? 'text-emerald-400' : 'text-rose-400'}`}>
              {score >= 0 ? '+' : ''}{score}
            </span>
          </div>
        </div>
      </div>
    </button>
  )
}

// ── Main component ────────────────────────────────────────────────────────────
export default function RadarPage() {
  const [deals, setDeals]               = useState([])
  const [loading, setLoading]           = useState(true)
  const [error, setError]               = useState(null)
  const [activeCategory, setCategory]   = useState('ALL')
  const [viewMode, setViewMode]         = useState('map')
  const [gpsTarget, setGpsTarget]       = useState(null)
  const [mapCenter, setMapCenter]       = useState({ lat: DEFAULT_CENTER[0], lng: DEFAULT_CENTER[1] })
  const [selectedDeal, setSelectedDeal] = useState(null)
  const [fullDeal, setFullDeal]         = useState(null)
  const [showCreate, setShowCreate]     = useState(false)
  const fetchRef = useRef(0)

  const fetchDeals = useCallback(async (center) => {
    const id = ++fetchRef.current
    setError(null)
    try {
      const params = { lat: center.lat, lng: center.lng, radiusKm: RADIUS_KM }
      const res = await radarApi.getDeals(params)
      if (id !== fetchRef.current) return
      setDeals(res.data ?? [])
    } catch (e) {
      if (id !== fetchRef.current) return
      setError('Could not load deals.')
    } finally {
      if (id === fetchRef.current) setLoading(false)
    }
  }, [])

  // Initial load: try geolocation, fall back to Bucharest
  useEffect(() => {
    setLoading(true)
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (pos) => {
          const t = { lat: pos.coords.latitude, lng: pos.coords.longitude }
          setGpsTarget(t)
          setMapCenter(t)
          fetchDeals(t)
        },
        () => fetchDeals({ lat: DEFAULT_CENTER[0], lng: DEFAULT_CENTER[1] }),
        { timeout: 5000 }
      )
    } else {
      fetchDeals({ lat: DEFAULT_CENTER[0], lng: DEFAULT_CENTER[1] })
    }
  }, [fetchDeals])

  const visibleDeals = activeCategory === 'ALL'
    ? deals
    : deals.filter(d => d.category === activeCategory)

  const meta = CATEGORY_META[activeCategory] ?? CATEGORY_META.ALL

  return (
    <AppShell>
      <div className="flex flex-col" style={{ height: 'calc(100dvh - 0px)' }}>
        {/* Top bar */}
        <div className="px-4 pt-6 pb-3 flex items-center justify-between flex-shrink-0">
          <div>
            <h1 className="text-2xl font-black text-slate-100 tracking-tight">Radar</h1>
            <p className="text-slate-500 text-xs">Crowdsourced student deals near you</p>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={() => { setLoading(true); fetchDeals(mapCenter) }}
              className="w-9 h-9 rounded-xl bg-slate-800/60 flex items-center justify-center text-slate-400 hover:text-slate-100"
              aria-label="Refresh"
            >
              {loading ? <Loader2 size={16} className="animate-spin" /> : <RefreshCw size={16} />}
            </button>
            {/* Map / List toggle */}
            <div className="flex rounded-xl border border-white/10 overflow-hidden">
              <button
                onClick={() => setViewMode('map')}
                className={`flex items-center gap-1.5 px-3 py-2 text-xs font-bold transition-all ${viewMode === 'map' ? 'bg-purple-500/25 text-purple-300' : 'text-slate-500 hover:text-slate-300'}`}
              >
                <MapIcon size={13} /> Map
              </button>
              <button
                onClick={() => setViewMode('list')}
                className={`flex items-center gap-1.5 px-3 py-2 text-xs font-bold transition-all ${viewMode === 'list' ? 'bg-purple-500/25 text-purple-300' : 'text-slate-500 hover:text-slate-300'}`}
              >
                <List size={13} /> List
              </button>
            </div>
          </div>
        </div>

        {/* Category pills */}
        <div className="px-4 pb-3 flex-shrink-0">
          <div className="flex gap-2 overflow-x-auto pb-1 scrollbar-none">
            {CATEGORIES.map(cat => {
              const m = CATEGORY_META[cat]
              const active = cat === activeCategory
              return (
                <button
                  key={cat}
                  onClick={() => setCategory(cat)}
                  className={`flex-shrink-0 rounded-full border px-4 py-1.5 text-xs font-black uppercase tracking-wide transition-all ${
                    active
                      ? `${m.bg} ${m.text} ${m.border}`
                      : 'bg-slate-800/40 border-white/10 text-slate-500 hover:text-slate-300'
                  }`}
                >
                  {cat}
                </button>
              )
            })}
          </div>
        </div>

        {/* Main content area (map or list) */}
        <div className="flex-1 relative min-h-0">
          {/* ── MAP VIEW ── */}
          {viewMode === 'map' && (
            <div className="absolute inset-0">
              {loading && (
                <div className="absolute inset-0 z-10 flex items-center justify-center bg-slate-900/60 backdrop-blur-sm">
                  <Loader2 size={28} className="animate-spin text-purple-400" />
                </div>
              )}
              <MapContainer
                center={DEFAULT_CENTER}
                zoom={13}
                style={{ width: '100%', height: '100%' }}
                zoomControl={false}
              >
                <TileLayer url={DARK_TILES} attribution={ATTRIBUTION} />
                <MapCenterTracker onCenter={setMapCenter} />
                <FlyTo target={gpsTarget} />
                {visibleDeals.map(deal => {
                  const color = CATEGORY_META[deal.category]?.color ?? '#A855F7'
                  const ttl   = timeLeft(deal.expiresAt)
                  const score = Number(deal.upvotes ?? 0) - Number(deal.downvotes ?? 0)
                  return (
                    <CircleMarker
                      key={deal.id}
                      center={[deal.latitude, deal.longitude]}
                      radius={11}
                      pathOptions={{
                        fillColor:   color,
                        fillOpacity: 0.92,
                        color:       'rgba(255,255,255,0.35)',
                        weight:      2,
                      }}
                      eventHandlers={{ click: () => setFullDeal(deal) }}
                    >
                      <Tooltip direction="top" offset={[0, -14]} opacity={1} className="radar-tooltip">
                        <div style={{ fontFamily: 'inherit' }}>
                          <div style={{ fontSize: '10px', fontWeight: 800, color, textTransform: 'uppercase', letterSpacing: '0.06em', marginBottom: '2px' }}>
                            {deal.category}
                          </div>
                          <div style={{ fontSize: '12px', fontWeight: 700, color: '#f1f5f9', maxWidth: '160px', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                            {deal.title}
                          </div>
                          <div style={{ display: 'flex', gap: '8px', marginTop: '3px', fontSize: '11px' }}>
                            <span style={{ color: ttl === 'Expired' ? '#f87171' : '#86efac', fontWeight: 600 }}>{ttl}</span>
                            <span style={{ color: score >= 0 ? '#86efac' : '#f87171', fontWeight: 700 }}>{score >= 0 ? '+' : ''}{score}</span>
                          </div>
                        </div>
                      </Tooltip>
                    </CircleMarker>
                  )
                })}
              </MapContainer>

              {/* Deal count overlay — z-[800] clears all Leaflet internal panes */}
              <div className="absolute top-3 left-3" style={{ zIndex: 800 }}>
                <div className="rounded-xl bg-slate-900/80 backdrop-blur border border-white/10 px-3 py-1.5 text-xs font-bold text-slate-300">
                  {visibleDeals.length} deal{visibleDeals.length !== 1 ? 's' : ''}
                  {activeCategory !== 'ALL' && ` · ${activeCategory}`}
                </div>
              </div>

              {error && (
                <div className="absolute top-3 right-3 flex items-center gap-2 rounded-xl bg-rose-900/80 border border-rose-500/30 px-3 py-2 text-xs font-bold text-rose-300 backdrop-blur" style={{ zIndex: 800 }}>
                  <AlertCircle size={13} /> {error}
                </div>
              )}
            </div>
          )}

          {/* ── LIST VIEW ── */}
          {viewMode === 'list' && (
            <div className="absolute inset-0 overflow-y-auto px-4 py-2 pb-28 md:pb-8 space-y-3">
              {loading && (
                <div className="flex items-center justify-center py-16">
                  <Loader2 size={24} className="animate-spin text-purple-400" />
                </div>
              )}
              {!loading && error && (
                <div className="flex flex-col items-center gap-2 py-16 text-center">
                  <AlertCircle size={24} className="text-rose-400" />
                  <p className="text-sm text-rose-300">{error}</p>
                </div>
              )}
              {!loading && !error && visibleDeals.length === 0 && (
                <div className="flex flex-col items-center gap-3 py-16 text-center">
                  <div className="w-14 h-14 rounded-2xl bg-slate-800/60 flex items-center justify-center">
                    <MapPin size={24} className="text-slate-500" />
                  </div>
                  <p className="text-slate-400 font-semibold text-sm">No active deals nearby</p>
                  <p className="text-slate-600 text-xs">Be the first to drop a pin in this area!</p>
                </div>
              )}
              {!loading && visibleDeals.map(deal => (
                <DealListCard
                  key={deal.id}
                  deal={deal}
                  onSelect={setSelectedDeal}
                />
              ))}
            </div>
          )}

          {/* FAB */}
          <button
            onClick={() => setShowCreate(true)}
            className="absolute bottom-6 right-5 flex items-center gap-2 rounded-full bg-purple-500 px-5 py-3.5 text-sm font-black text-white shadow-[0_0_28px_rgba(168,85,247,0.55)] hover:bg-purple-400 active:scale-95 transition-all"
          style={{ zIndex: 800 }}
          >
            <Plus size={18} strokeWidth={3} />
            Add Deal
          </button>
        </div>
      </div>

      {/* Bottom sheet (map pin click or list card click) */}
      {selectedDeal && (
        <DealBottomSheet
          deal={selectedDeal}
          onClose={() => setSelectedDeal(null)}
          onOpenDetails={(d) => setFullDeal(d)}
          onVoted={() => fetchDeals(mapCenter)}
        />
      )}

      {/* Full details modal */}
      {fullDeal && (
        <DealFullDetailsModal
          deal={fullDeal}
          onClose={() => setFullDeal(null)}
        />
      )}

      {/* Create deal modal */}
      {showCreate && (
        <CreateDealModal
          onClose={() => setShowCreate(false)}
          onCreated={() => fetchDeals(mapCenter)}
        />
      )}
    </AppShell>
  )
}
