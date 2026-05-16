import { useState, useEffect } from 'react'
import { MapContainer, TileLayer, useMapEvents, useMap } from 'react-leaflet'
import 'leaflet/dist/leaflet.css'
import { X, MapPin, Loader2, ChevronRight } from 'lucide-react'
import { radarApi } from '../../api/radarApi'

const DARK_TILES   = 'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png'
const ATTRIBUTION  = '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> &copy; <a href="https://carto.com/attributions">CARTO</a>'
const CATEGORIES   = ['FOOD', 'HOME', 'SOCIAL', 'TECH', 'OTHER']
const DEFAULT_CENTER = [44.4268, 26.1025]

// Tracks map centre while user drags
function CenterTracker({ onChange }) {
  useMapEvents({
    moveend(e) {
      const { lat, lng } = e.target.getCenter()
      onChange({ lat, lng })
    },
  })
  return null
}

// Flies to a new position when `target` changes
function FlyTo({ target }) {
  const map = useMap()
  useEffect(() => {
    if (target) map.flyTo([target.lat, target.lng], 15, { animate: true, duration: 1 })
  }, [target, map])
  return null
}

// Forces Leaflet to recalculate its size after the container renders
function SizeInvalidator() {
  const map = useMap()
  useEffect(() => {
    // Small delay ensures the flex layout has settled before Leaflet reads clientHeight
    const t = setTimeout(() => map.invalidateSize(), 50)
    return () => clearTimeout(t)
  }, [map])
  return null
}

const INPUT = 'w-full rounded-2xl bg-slate-800/60 border border-white/10 px-4 py-3 text-slate-100 text-sm placeholder-slate-500 focus:outline-none focus:border-purple-500/50'
const LABEL = 'block text-xs font-bold text-slate-400 mb-1.5 uppercase tracking-wider'

export default function CreateDealModal({ onClose, onCreated }) {
  const [step, setStep]     = useState(1)
  const [coords, setCoords] = useState({ lat: DEFAULT_CENTER[0], lng: DEFAULT_CENTER[1] })
  const [gpsTarget, setGpsTarget]   = useState(null)
  const [gpsLoading, setGpsLoading] = useState(false)
  const [gpsError, setGpsError]     = useState(null)

  const [form, setForm] = useState({
    title: '', description: '', category: 'FOOD', hoursToLive: 6,
  })
  const [submitting, setSubmitting] = useState(false)
  const [error, setError]           = useState(null)

  function handleGPS() {
    if (!navigator.geolocation) { setGpsError('Geolocation not supported'); return }
    setGpsLoading(true)
    setGpsError(null)
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        const t = { lat: pos.coords.latitude, lng: pos.coords.longitude }
        setCoords(t)
        setGpsTarget(t)
        setGpsLoading(false)
      },
      () => { setGpsError('Location access denied'); setGpsLoading(false) },
      { timeout: 8000 }
    )
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setSubmitting(true)
    setError(null)
    try {
      const expiresAt = new Date(Date.now() + Number(form.hoursToLive) * 3_600_000).toISOString()
      await radarApi.createDeal({
        title:       form.title.trim(),
        description: form.description.trim() || null,
        category:    form.category,
        latitude:    coords.lat,
        longitude:   coords.lng,
        expiresAt,
      })
      onCreated?.()
      onClose()
    } catch (err) {
      const msg = err.response?.data?.message ?? err.message
      setError(
        err.response?.status === 403
          ? 'Your trust score is too low to post deals (minimum 30 required).'
          : msg || 'Failed to create deal.'
      )
      setSubmitting(false)
    }
  }

  return (
    // z-[600] ensures this sits above Leaflet's tooltip pane (z-700 internally, but that
    // is scoped inside the Leaflet container's own stacking context)
    <div className="fixed inset-0 flex flex-col bg-slate-900/98 backdrop-blur-xl overflow-hidden" style={{ zIndex: 1100 }}>

      {/* ── Header ──────────────────────────────────────────────────── */}
      <div className="flex items-center justify-between px-5 pt-6 pb-4 border-b border-white/10 flex-shrink-0">
        <div>
          <p className="text-xs font-bold text-purple-400 uppercase tracking-widest mb-0.5">
            Step {step} of 2
          </p>
          <h2 className="text-xl font-black text-slate-100">
            {step === 1 ? 'Pin the Location' : 'Deal Details'}
          </h2>
        </div>
        <button
          onClick={onClose}
          className="w-9 h-9 rounded-xl bg-slate-800/60 flex items-center justify-center text-slate-400 hover:text-slate-100"
        >
          <X size={18} />
        </button>
      </div>

      {/* ── Progress bar ────────────────────────────────────────────── */}
      <div className="h-1 bg-slate-800 flex-shrink-0">
        <div
          className="h-full bg-purple-500 transition-all duration-500"
          style={{ width: step === 1 ? '50%' : '100%' }}
        />
      </div>

      {/* ── Step 1: Map ─────────────────────────────────────────────── */}
      {step === 1 && (
        <div className="flex flex-col flex-1 overflow-hidden">

          {/* Instruction + GPS button */}
          <div className="flex-shrink-0 px-5 py-3 space-y-2">
            <p className="text-sm text-slate-400">
              Drag the map to position the pin on your deal's location.
            </p>
            <button
              onClick={handleGPS}
              disabled={gpsLoading}
              className="flex items-center gap-2 rounded-2xl border border-purple-500/40 bg-purple-500/15 px-4 py-2.5 text-sm font-bold text-purple-300 hover:bg-purple-500/25 disabled:opacity-50"
            >
              {gpsLoading
                ? <Loader2 size={15} className="animate-spin" />
                : <MapPin size={15} />
              }
              Use My Current GPS Location
            </button>
            {gpsError && <p className="text-xs text-rose-400">{gpsError}</p>}
          </div>

          {/* Map area — flex-1 with overflow:hidden so Leaflet gets a real px height */}
          <div className="flex-1 relative overflow-hidden">
            {/* Map fills the container via absolute positioning */}
            <div className="absolute inset-0">
              <MapContainer
                center={DEFAULT_CENTER}
                zoom={14}
                style={{ width: '100%', height: '100%' }}
                zoomControl={false}
              >
                <TileLayer url={DARK_TILES} attribution={ATTRIBUTION} />
                <CenterTracker onChange={setCoords} />
                <FlyTo target={gpsTarget} />
                <SizeInvalidator />
              </MapContainer>
            </div>

            {/* Fixed centre pin — z-index 1000 clears all Leaflet internal panes */}
            <div
              className="pointer-events-none absolute inset-0 flex items-center justify-center"
              style={{ zIndex: 1000 }}
            >
              <div className="relative flex flex-col items-center" style={{ marginTop: '-24px' }}>
                {/* Circle */}
                <div
                  className="w-5 h-5 rounded-full border-2 border-white"
                  style={{
                    background: '#A855F7',
                    boxShadow: '0 0 0 4px rgba(168,85,247,0.25), 0 0 16px rgba(168,85,247,0.8)',
                  }}
                />
                {/* Stem */}
                <div
                  className="w-0.5 bg-purple-400"
                  style={{ height: '18px', opacity: 0.7 }}
                />
                {/* Shadow dot */}
                <div className="w-2 h-1 rounded-full bg-black/30" style={{ marginTop: '2px' }} />
              </div>
            </div>
          </div>

          {/* Footer: coords + confirm */}
          <div className="flex-shrink-0 px-5 py-4 border-t border-white/10 bg-slate-900/80 flex items-center justify-between gap-3">
            <p className="text-xs text-slate-500 font-mono truncate">
              {coords.lat.toFixed(5)},&nbsp;{coords.lng.toFixed(5)}
            </p>
            <button
              onClick={() => setStep(2)}
              className="flex-shrink-0 flex items-center gap-2 rounded-2xl bg-purple-500 px-5 py-2.5 text-sm font-black text-white shadow-[0_0_20px_rgba(168,85,247,0.4)] hover:bg-purple-400"
            >
              Confirm Location <ChevronRight size={16} />
            </button>
          </div>
        </div>
      )}

      {/* ── Step 2: Form ────────────────────────────────────────────── */}
      {step === 2 && (
        <form onSubmit={handleSubmit} className="flex flex-col flex-1 overflow-hidden">

          <div className="flex-1 overflow-y-auto px-5 py-5 space-y-5">

            {/* Location summary */}
            <div className="flex items-center gap-2 rounded-2xl bg-slate-800/40 border border-white/10 px-4 py-3">
              <div className="w-7 h-7 rounded-xl bg-purple-500/20 flex items-center justify-center flex-shrink-0">
                <MapPin size={14} className="text-purple-400" />
              </div>
              <p className="text-xs text-slate-400 font-mono flex-1 truncate">
                {coords.lat.toFixed(5)},&nbsp;{coords.lng.toFixed(5)}
              </p>
              <button
                type="button"
                onClick={() => setStep(1)}
                className="text-xs text-purple-400 font-bold hover:text-purple-300"
              >
                Change
              </button>
            </div>

            {/* Title */}
            <div>
              <label className={LABEL}>Title *</label>
              <input
                className={INPUT}
                type="text"
                placeholder="e.g. 50% off at Campus Cafe"
                value={form.title}
                onChange={(e) => setForm(f => ({ ...f, title: e.target.value }))}
                required
                maxLength={120}
              />
            </div>

            {/* Description */}
            <div>
              <label className={LABEL}>Description</label>
              <textarea
                className={`${INPUT} resize-none`}
                rows={3}
                placeholder="Tell other students about this deal…"
                value={form.description}
                onChange={(e) => setForm(f => ({ ...f, description: e.target.value }))}
                maxLength={500}
              />
            </div>

            {/* Category */}
            <div>
              <label className={LABEL}>Category</label>
              <select
                className={INPUT}
                value={form.category}
                onChange={(e) => setForm(f => ({ ...f, category: e.target.value }))}
              >
                {CATEGORIES.map(c => (
                  <option key={c} value={c}>{c}</option>
                ))}
              </select>
            </div>

            {/* Hours to Live */}
            <div>
              <label className={LABEL}>
                Hours to Live —{' '}
                <span className="text-purple-400">{form.hoursToLive}h</span>
              </label>
              <input
                type="range"
                min={1}
                max={72}
                value={form.hoursToLive}
                onChange={(e) => setForm(f => ({ ...f, hoursToLive: Number(e.target.value) }))}
                className="w-full accent-purple-500"
              />
              <div className="flex justify-between text-xs text-slate-600 mt-1">
                <span>1h</span><span>72h</span>
              </div>
            </div>

            {error && (
              <div className="rounded-2xl bg-rose-500/15 border border-rose-500/30 px-4 py-3 text-sm text-rose-300">
                {error}
              </div>
            )}
          </div>

          {/* Submit */}
          <div className="flex-shrink-0 px-5 py-4 border-t border-white/10 bg-slate-900/80">
            <button
              type="submit"
              disabled={submitting || !form.title.trim()}
              className="w-full flex items-center justify-center gap-2 rounded-2xl bg-purple-500 py-3.5 text-sm font-black text-white shadow-[0_0_24px_rgba(168,85,247,0.4)] hover:bg-purple-400 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {submitting
                ? <Loader2 size={16} className="animate-spin" />
                : <MapPin size={16} />
              }
              {submitting ? 'Dropping Pin…' : 'Drop the Pin'}
            </button>
          </div>
        </form>
      )}
    </div>
  )
}
