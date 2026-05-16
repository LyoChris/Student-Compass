import api from './axiosInstance'

export const budgetApi = {
  // ── Monthly budget ──────────────────────────────────────────────────────────
  getCurrentBudget: (month, year) =>
    api.get('/api/v1/budgets/current', { params: { month, year } }),

  // ── Categories ──────────────────────────────────────────────────────────────
  adjustCategory: (budgetId, categoryName, newAllocation) =>
    api.put(`/api/v1/budgets/${budgetId}/categories`, {
      categoryName,
      newAllocation,
    }),

  deleteCategory: (budgetId, categoryName) =>
    api.delete(`/api/v1/budgets/${budgetId}/categories/${encodeURIComponent(categoryName)}`),

  // ── Transactions ────────────────────────────────────────────────────────────
  logTransaction: (budgetId, categoryName, amount, description) =>
    api.post('/api/v1/budgets/transactions', {
      budgetId,
      categoryName,
      amount,
      description,
    }),

  // ── CSV upload ──────────────────────────────────────────────────────────────
  uploadStatement: (budgetId, file) => {
    const form = new FormData()
    form.append('file', file)
    return api.post(`/api/v1/budgets/${budgetId}/upload-statement`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
}
