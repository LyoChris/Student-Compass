import { useState, useRef, useEffect, useCallback } from 'react'
import { X, FileSpreadsheet, UploadCloud, CheckCircle2, AlertCircle, Loader2, Trash2 } from 'lucide-react'
import { budgetApi } from '../../api/budgetApi'

export default function UploadStatementModal({ budget, onClose, onRefresh, showToast }) {
  const [file,      setFile]      = useState(null)
  const [dragOver,  setDragOver]  = useState(false)
  const [uploading, setUploading] = useState(false)
  const [result,    setResult]    = useState(null)   // parsed transactions array
  const [error,     setError]     = useState('')
  const inputRef = useRef(null)

  useEffect(() => {
    const h = (e) => { if (e.key === 'Escape' && !uploading) onClose() }
    document.addEventListener('keydown', h)
    return () => document.removeEventListener('keydown', h)
  }, [uploading, onClose])

  const pickFile = (f) => {
    if (!f) return
    if (!f.name.toLowerCase().endsWith('.csv')) {
      setError('Only .csv files are supported.')
      return
    }
    setFile(f)
    setError('')
    setResult(null)
  }

  const handleDrop = useCallback((e) => {
    e.preventDefault()
    setDragOver(false)
    const f = e.dataTransfer.files?.[0]
    if (f) pickFile(f)
  }, [])

  const handleUpload = async () => {
    if (!file || uploading) return
    setUploading(true)
    setError('')
    setResult(null)
    try {
      const { data } = await budgetApi.uploadStatement(budget.budgetId, file)
      const rows    = Array.isArray(data) ? data : []
      const persisted = rows.filter((r) => r.persisted).length
      setResult(rows)
      showToast(`${persisted} transaction${persisted !== 1 ? 's' : ''} imported successfully.`)
      onRefresh()
    } catch (err) {
      setError(err.response?.data?.message ?? 'Could not parse the bank statement. Please check the file format.')
    } finally {
      setUploading(false)
    }
  }

  const persistedCount = result?.filter((r) => r.persisted).length ?? 0
  const skippedCount   = result?.filter((r) => !r.persisted).length ?? 0

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4" role="dialog" aria-modal="true">
      <div className="absolute inset-0 bg-slate-950/80 backdrop-blur-sm" onClick={!uploading ? onClose : undefined} />

      <div className="relative z-10 w-full max-w-md glass-card rounded-3xl shadow-2xl flex flex-col max-h-[90vh]">
        {/* Header */}
        <div className="flex items-center justify-between px-6 pt-5 pb-4 border-b border-white/8 flex-shrink-0">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-xl bg-purple-500/20 flex items-center justify-center">
              <FileSpreadsheet size={16} className="text-purple-400" />
            </div>
            <h2 className="text-lg font-black text-slate-100">Upload Statement</h2>
          </div>
          <button
            onClick={onClose}
            disabled={uploading}
            className="w-8 h-8 flex items-center justify-center rounded-xl text-slate-500 hover:text-slate-300 hover:bg-white/5 disabled:opacity-40"
          >
            <X size={16} />
          </button>
        </div>

        <div className="overflow-y-auto flex-1 px-6 py-5 space-y-4">
          {/* Error */}
          {error && (
            <div className="flex items-start gap-2 rounded-2xl border border-rose-400/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-200">
              <AlertCircle size={14} className="mt-0.5 flex-shrink-0" />
              {error}
            </div>
          )}

          {/* Drop zone */}
          {!result && (
            <button
              type="button"
              onClick={() => inputRef.current?.click()}
              onDragEnter={(e) => { e.preventDefault(); setDragOver(true) }}
              onDragOver={(e) => e.preventDefault()}
              onDragLeave={() => setDragOver(false)}
              onDrop={handleDrop}
              className={`w-full flex flex-col items-center justify-center rounded-3xl border border-dashed p-10 text-center transition-colors
                ${dragOver
                  ? 'border-purple-400 bg-purple-500/15'
                  : file
                  ? 'border-emerald-400/50 bg-emerald-500/10'
                  : 'border-white/15 bg-slate-950/35 hover:border-purple-400/40 hover:bg-purple-500/8'
                }`}
            >
              {file ? (
                <>
                  <FileSpreadsheet size={36} className="text-emerald-400 mb-3" />
                  <span className="text-sm font-black text-emerald-300 break-all">{file.name}</span>
                  <span className="text-xs text-slate-500 mt-1">{(file.size / 1024).toFixed(1)} KB — ready to upload</span>
                </>
              ) : (
                <>
                  <UploadCloud size={36} className="text-purple-400 mb-3" />
                  <span className="text-sm font-black text-slate-100">Drag & drop your statement here</span>
                  <span className="text-xs text-slate-500 mt-1">Revolut / ING CSV export — or tap to browse</span>
                </>
              )}
            </button>
          )}

          <input
            ref={inputRef}
            type="file"
            accept=".csv"
            className="hidden"
            onChange={(e) => pickFile(e.target.files?.[0])}
          />

          {/* Clear file */}
          {file && !result && (
            <button
              type="button"
              onClick={() => { setFile(null); setError('') }}
              className="flex items-center gap-1.5 text-xs text-slate-500 hover:text-rose-400 mx-auto"
            >
              <Trash2 size={12} />
              Remove file
            </button>
          )}

          {/* Result summary */}
          {result && (
            <div className="space-y-3">
              <div className="flex gap-3">
                <div className="flex-1 glass-card rounded-2xl px-4 py-3 text-center border-emerald-500/30 border">
                  <p className="text-2xl font-black text-emerald-400">{persistedCount}</p>
                  <p className="text-xs text-slate-400 font-semibold mt-0.5">Imported</p>
                </div>
                <div className="flex-1 glass-card rounded-2xl px-4 py-3 text-center border-amber-500/30 border">
                  <p className="text-2xl font-black text-amber-400">{skippedCount}</p>
                  <p className="text-xs text-slate-400 font-semibold mt-0.5">Skipped</p>
                </div>
              </div>

              {skippedCount > 0 && (
                <p className="text-xs text-amber-400/80 text-center leading-relaxed">
                  {skippedCount} row{skippedCount !== 1 ? 's were' : ' was'} skipped because the detected category does not exist in this budget.
                  Add the missing categories and re-upload to capture them.
                </p>
              )}

              <div className="divide-y divide-slate-700/50 max-h-48 overflow-y-auto">
                {result.map((row, i) => (
                  <div key={i} className="flex items-center justify-between py-2.5 gap-3">
                    <div className="flex items-center gap-2 min-w-0">
                      {row.persisted
                        ? <CheckCircle2 size={13} className="text-emerald-400 flex-shrink-0" />
                        : <AlertCircle  size={13} className="text-amber-400  flex-shrink-0" />
                      }
                      <span className="text-xs text-slate-300 truncate">{row.description}</span>
                    </div>
                    <div className="text-right flex-shrink-0">
                      <p className="text-xs font-bold text-rose-300">−{Number(row.amount ?? 0).toFixed(2)} RON</p>
                      <p className="text-[10px] text-slate-500">{row.detectedCategory}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="flex gap-3 px-6 py-4 border-t border-white/8 flex-shrink-0">
          <button
            onClick={onClose}
            disabled={uploading}
            className="flex-1 rounded-2xl border border-slate-600 px-4 py-2.5 text-sm font-bold text-slate-300
                       hover:border-slate-500 hover:bg-white/5 disabled:opacity-40 disabled:cursor-not-allowed"
          >
            {result ? 'Close' : 'Cancel'}
          </button>

          {!result && (
            <button
              onClick={handleUpload}
              disabled={!file || uploading}
              className="flex-1 flex items-center justify-center gap-2 rounded-2xl bg-purple-500 hover:bg-purple-400
                         px-4 py-2.5 text-sm font-bold text-white shadow-[0_0_20px_rgba(168,85,247,0.3)]
                         disabled:opacity-60 disabled:cursor-not-allowed"
            >
              {uploading ? (
                <>
                  <Loader2 size={15} className="animate-spin" />
                  Parsing & auto-categorizing…
                </>
              ) : (
                <>
                  <UploadCloud size={15} />
                  Upload Statement
                </>
              )}
            </button>
          )}
        </div>
      </div>
    </div>
  )
}
