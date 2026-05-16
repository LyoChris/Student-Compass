import api from './axiosInstance'

export const accountApi = {
  /** GET /api/v1/account/me → UserProfileResponse */
  getMe: () =>
    api.get('/api/v1/account/me'),
}
