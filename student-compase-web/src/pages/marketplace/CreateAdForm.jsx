import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  AlertCircle,
  ArrowLeft,
  CheckCircle2,
  ImagePlus,
  Loader2,
  Phone,
  Sparkles,
  Trash2,
  UploadCloud,
} from 'lucide-react'
import { marketplaceApi } from '../../api/marketplaceApi'
import { useAuth } from '../../hooks/useAuth'

const categories = [
  { value: 'BOOKS_NOTES', label: 'Books' },
  { value: 'ELECTRONICS', label: 'Tech' },
  { value: 'DORM_APPLIANCES', label: 'Dorm' },
  { value: 'CLOTHING', label: 'Clothing' },
  { value: 'OTHER', label: 'Other' },
]

const conditions = [
  { value: 'NEW', label: 'New' },
  { value: 'LIKE_NEW', label: 'Like New' },
  { value: 'GOOD', label: 'Good' },
  { value: 'FAIR', label: 'Fair' },
]

const initialForm = {
  title: '',
  description: '',
  price: '',
  category: 'BOOKS_NOTES',
  itemCondition: 'GOOD',
  contactPhone: '',
}

const inputClass =
  'w-full rounded-2xl border border-white/10 bg-slate-950/45 px-4 py-3.5 text-sm text-slate-100 outline-none transition placeholder:text-slate-500 focus:border-purple-400/80 focus:ring-2 focus:ring-purple-500/30'

function PillSelector({ label, options, value, onChange }) {
  return (
    <div>
      <p className="mb-3 text-sm font-black text-slate-300">{label}</p>
      <div className="flex flex-wrap gap-2">
        {options.map((option) => {
          const active = option.value === value
          return (
            <button
              key={option.value}
              type="button"
              onClick={() => onChange(option.value)}
              className={`rounded-full border px-4 py-2 text-xs font-black uppercase tracking-wide transition ${
                active
                  ? 'border-purple-300 bg-purple-500 text-white shadow-[0_0_20px_rgba(168,85,247,0.35)]'
                  : 'border-white/10 bg-white/5 text-slate-400 hover:border-purple-300/40 hover:text-slate-100'
              }`}
            >
              {option.label}
            </button>
          )
        })}
      </div>
    </div>
  )
}

function ConfettiBurst() {
  return (
    <div className="pointer-events-none fixed inset-0 z-[70] overflow-hidden">
      {Array.from({ length: 28 }, (_, index) => (
        <span
          key={index}
          className="confetti-piece"
          style={{
            left: `${8 + ((index * 11) % 84)}%`,
            animationDelay: `${(index % 8) * 0.08}s`,
            background: ['#A855F7', '#22D3EE', '#34D399', '#FBBF24'][index % 4],
          }}
        />
      ))}
    </div>
  )
}

export default function CreateAdForm() {
  const navigate = useNavigate()
  const { user } = useAuth()
  const inputRef = useRef(null)
  const [form, setForm] = useState(initialForm)
  const [files, setFiles] = useState([])
  const [dragActive, setDragActive] = useState(false)
  const [status, setStatus] = useState('idle')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState(false)

  const previews = useMemo(
    () => files.map((file) => ({ file, url: URL.createObjectURL(file) })),
    [files],
  )

  useEffect(() => {
    return () => {
      previews.forEach((preview) => URL.revokeObjectURL(preview.url))
    }
  }, [previews])

  const updateField = (event) => {
    const { name, value } = event.target
    setForm((current) => ({
      ...current,
      [name]: name === 'price' && Number(value) < 0 ? '0' : value,
    }))
    if (error) setError('')
  }

  const addFiles = (selectedFiles) => {
    const imageFiles = Array.from(selectedFiles).filter((file) => file.type.startsWith('image/'))
    setFiles((current) => [...current, ...imageFiles].slice(0, 3))
    if (error) setError('')
  }

  const removeFile = (index) => {
    setFiles((current) => current.filter((_, currentIndex) => currentIndex !== index))
  }

  const uploadImages = async () => {
    if (files.length === 0) return []

    const cloudName = import.meta.env.VITE_CLOUDINARY_CLOUD_NAME
    const uploadPreset = import.meta.env.VITE_CLOUDINARY_UPLOAD_PRESET

    if (!cloudName || !uploadPreset) {
      throw new Error('Cloudinary env vars lipsesc: VITE_CLOUDINARY_CLOUD_NAME si VITE_CLOUDINARY_UPLOAD_PRESET.')
    }

    const uploads = files.map(async (file) => {
      const body = new FormData()
      body.append('file', file)
      body.append('upload_preset', uploadPreset)

      const response = await fetch(`https://api.cloudinary.com/v1_1/${cloudName}/image/upload`, {
        method: 'POST',
        body,
      })

      if (!response.ok) {
        throw new Error('Upload-ul catre Cloudinary a esuat.')
      }

      const data = await response.json()
      return data.secure_url
    })

    return Promise.all(uploads)
  }

  const submitAd = async (event) => {
    event.preventDefault()

    if (!user?.id) {
      setError('Nu am gasit userId-ul tau. Te rog autentifica-te din nou.')
      return
    }

    if (Number(form.price) <= 0) {
      setError('Pretul trebuie sa fie mai mare decat 0.')
      return
    }

    setError('')

    try {
      setStatus(files.length ? 'uploading' : 'publishing')
      const imageUrls = await uploadImages()

      setStatus('publishing')
      await marketplaceApi.create({
        sellerId: user.id,
        title: form.title.trim(),
        description: form.description.trim(),
        price: Number(Number(form.price).toFixed(2)),
        category: form.category,
        itemCondition: form.itemCondition,
        tags: [],
        imageUrls,
        contactPhone: form.contactPhone.trim() || undefined,
      })

      setSuccess(true)
      setStatus('success')
      window.setTimeout(() => navigate('/marketplace', { replace: true }), 950)
    } catch (err) {
      setStatus('idle')
      setError(err.response?.data?.message || err.message || 'Nu am putut publica anuntul. Incearca din nou.')
    }
  }

  const busy = status === 'uploading' || status === 'publishing'

  return (
    <div className="min-h-screen bg-[#0F172A] px-4 pb-28 pt-5 text-slate-100 md:pb-10 lg:px-6">
      {success && <ConfettiBurst />}

      <div className="pointer-events-none fixed inset-0">
        <div className="absolute right-[-14rem] top-[-16rem] h-[34rem] w-[34rem] rounded-full bg-purple-500/20 blur-3xl" />
        <div className="absolute bottom-[-18rem] left-[-10rem] h-[32rem] w-[32rem] rounded-full bg-cyan-500/10 blur-3xl" />
      </div>

      <div className="relative mx-auto max-w-3xl">
        <button
          type="button"
          onClick={() => navigate('/marketplace')}
          className="mb-5 flex items-center gap-2 rounded-2xl border border-white/10 px-3 py-2 text-sm font-black text-slate-400 hover:bg-white/5 hover:text-slate-100"
        >
          <ArrowLeft size={17} />
          Back to Market
        </button>

        <header className="mb-5">
          <div className="mb-3 inline-flex items-center gap-2 rounded-full border border-purple-400/20 bg-purple-500/10 px-3 py-1.5 text-xs font-black uppercase tracking-[0.2em] text-purple-200">
            <Sparkles size={13} />
            Sell Item
          </div>
          <h1 className="text-3xl font-black tracking-tight sm:text-5xl">Create your campus ad</h1>
          <p className="mt-2 text-sm leading-6 text-slate-400">
            Upload up to 3 images, set a fair RON price, and publish straight to StuFi Marketplace.
          </p>
        </header>

        {success && (
          <div className="mb-4 flex items-center gap-3 rounded-2xl border border-emerald-300/30 bg-emerald-500/10 p-4 text-sm font-bold text-emerald-200">
            <CheckCircle2 size={19} />
            Listing published. Redirecting to marketplace...
          </div>
        )}

        {error && (
          <div className="mb-4 flex gap-3 rounded-2xl border border-red-400/30 bg-red-500/10 p-4 text-sm text-red-200">
            <AlertCircle size={18} className="mt-0.5 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={submitAd} className="glass-card space-y-6 rounded-[2rem] p-5 shadow-2xl shadow-black/20 sm:p-7">
          <div className="grid gap-4 sm:grid-cols-[1fr_11rem]">
            <label className="block">
              <span className="mb-2 block text-sm font-black text-slate-300">Title</span>
              <input
                name="title"
                value={form.title}
                onChange={updateField}
                required
                maxLength={100}
                placeholder="Math 1 Course Notes"
                className={inputClass}
              />
            </label>

            <label className="block">
              <span className="mb-2 block text-sm font-black text-slate-300">Price</span>
              <div className="relative">
                <input
                  name="price"
                  type="number"
                  min="0.01"
                  step="0.01"
                  inputMode="decimal"
                  value={form.price}
                  onChange={updateField}
                  required
                  placeholder="45"
                  className={`${inputClass} pr-14`}
                />
                <span className="absolute right-4 top-1/2 -translate-y-1/2 text-xs font-black text-slate-500">RON</span>
              </div>
            </label>
          </div>

          <label className="block">
            <span className="mb-2 block text-sm font-black text-slate-300">Description</span>
            <textarea
              name="description"
              value={form.description}
              onChange={updateField}
              required
              rows={5}
              maxLength={2000}
              placeholder="Tell students what it is, condition, pickup area, and why it is useful."
              className={`${inputClass} resize-none`}
            />
          </label>

          <label className="block">
            <span className="mb-2 block text-sm font-black text-slate-300">Contact phone (optional)</span>
            <div className="relative">
              <input
                name="contactPhone"
                type="tel"
                value={form.contactPhone}
                onChange={updateField}
                placeholder="+40 712 345 678"
                className={`${inputClass} pl-11`}
              />
              <Phone size={16} className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500" />
            </div>
            <p className="mt-2 text-xs text-slate-500">
              Leave blank to automatically use your registered account phone number.
            </p>
          </label>

          <PillSelector
            label="Category"
            options={categories}
            value={form.category}
            onChange={(value) => setForm((current) => ({ ...current, category: value }))}
          />

          <PillSelector
            label="Condition"
            options={conditions}
            value={form.itemCondition}
            onChange={(value) => setForm((current) => ({ ...current, itemCondition: value }))}
          />

          <div>
            <p className="mb-3 text-sm font-black text-slate-300">Photos</p>
            <button
              type="button"
              onClick={() => inputRef.current?.click()}
              onDragEnter={(event) => {
                event.preventDefault()
                setDragActive(true)
              }}
              onDragOver={(event) => event.preventDefault()}
              onDragLeave={() => setDragActive(false)}
              onDrop={(event) => {
                event.preventDefault()
                setDragActive(false)
                addFiles(event.dataTransfer.files)
              }}
              className={`flex w-full flex-col items-center justify-center rounded-[1.5rem] border border-dashed p-8 text-center transition ${
                dragActive
                  ? 'border-purple-300 bg-purple-500/15'
                  : 'border-white/15 bg-slate-950/35 hover:border-purple-300/40 hover:bg-purple-500/10'
              }`}
            >
              <div className="mb-3 flex h-14 w-14 items-center justify-center rounded-3xl bg-purple-500/15 text-purple-200">
                <UploadCloud size={27} />
              </div>
              <span className="text-sm font-black text-slate-100">Drop images here or tap to upload</span>
              <span className="mt-1 text-xs text-slate-500">Max 3 images, uploaded directly to Cloudinary</span>
            </button>
            <input
              ref={inputRef}
              type="file"
              accept="image/*"
              multiple
              className="hidden"
              onChange={(event) => addFiles(event.target.files)}
            />

            <div className="mt-3 grid grid-cols-3 gap-3">
              {previews.map((preview, index) => (
                <div key={`${preview.file.name}-${index}`} className="relative aspect-square overflow-hidden rounded-2xl border border-white/10 bg-slate-950/40">
                  <img src={preview.url} alt="" className="h-full w-full object-cover" />
                  <button
                    type="button"
                    onClick={() => removeFile(index)}
                    className="absolute right-2 top-2 flex h-8 w-8 items-center justify-center rounded-xl bg-slate-950/80 text-red-300 backdrop-blur hover:bg-red-500 hover:text-white"
                    aria-label="Remove image"
                  >
                    <Trash2 size={15} />
                  </button>
                </div>
              ))}
              {files.length < 3 && (
                <button
                  type="button"
                  onClick={() => inputRef.current?.click()}
                  className="flex aspect-square items-center justify-center rounded-2xl border border-dashed border-white/10 bg-white/5 text-slate-500 hover:border-purple-300/40 hover:text-purple-200"
                  aria-label="Add image"
                >
                  <ImagePlus size={24} />
                </button>
              )}
            </div>
          </div>

          <button
            type="submit"
            disabled={busy}
            className="flex w-full items-center justify-center gap-2 rounded-2xl bg-purple-500 px-5 py-4 text-sm font-black text-white shadow-[0_0_34px_rgba(168,85,247,0.45)] hover:bg-purple-400 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {busy && <Loader2 size={18} className="animate-spin" />}
            {status === 'uploading' ? 'Uploading images...' : status === 'publishing' ? 'Publishing...' : 'Publish'}
          </button>
        </form>
      </div>
    </div>
  )
}
