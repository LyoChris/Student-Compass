import api from './axiosInstance'

export const profileApi = {
  getProfile:    (userId) => api.get(`/api/v1/profiles/${userId}`),
  upsertProfile: (userId, payload) => api.put(`/api/v1/profiles/${userId}`, payload),
}
