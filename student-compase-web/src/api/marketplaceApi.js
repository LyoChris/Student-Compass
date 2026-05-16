import api from './axiosInstance'

export const marketplaceApi = {
  search: (params) => api.get('/api/v1/marketplace', { params }),
  getById: (id) => api.get(`/api/v1/marketplace/${id}`),
  create: (payload) => api.post('/api/v1/marketplace', payload),
}
