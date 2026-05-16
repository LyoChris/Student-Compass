import api from './axiosInstance'

export const marketplaceApi = {
  search:       (params)          => api.get('/api/v1/marketplace', { params }),
  getById:      (id)              => api.get(`/api/v1/marketplace/${id}`),
  create:       (payload)         => api.post('/api/v1/marketplace', payload),

  // Owner endpoints
  getMyItems:   (params)          => api.get('/api/v1/marketplace/me', { params }),
  updateItem:   (id, payload)     => api.put(`/api/v1/marketplace/${id}`, payload),
  changeStatus: (id, status)      => api.patch(`/api/v1/marketplace/${id}/status`, null, { params: { status } }),
  deleteItem:   (id)              => api.delete(`/api/v1/marketplace/${id}`),
  boostItem:    (id)              => api.patch(`/api/v1/marketplace/${id}/boost`),
}
