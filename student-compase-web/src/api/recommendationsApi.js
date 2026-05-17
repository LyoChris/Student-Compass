import api from './axiosInstance'

export const recommendationsApi = {
  // params is optional — pass { category: 'FOOD' } to filter by category
  getRecommendations: (params = {}) =>
    api.get('/api/v1/recommendations', { params }),
}
