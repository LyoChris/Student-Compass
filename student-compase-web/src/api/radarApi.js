import api from './axiosInstance'

export const radarApi = {
  getDeals:   (params)           => api.get('/api/v1/radar/deals', { params }),
  getDeal:    (dealId)           => api.get(`/api/v1/radar/deals/${dealId}`),
  createDeal: (payload)          => api.post('/api/v1/radar/deals', payload),
  vote:       (dealId, voteType) => api.post(`/api/v1/radar/deals/${dealId}/vote`, { voteType }),
  addComment: (dealId, content)  => api.post(`/api/v1/radar/deals/${dealId}/comments`, { content }),
}
