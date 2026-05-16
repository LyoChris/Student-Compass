import api from './axiosInstance'

export const authApi = {
  /** POST /api/v1/auth/login → AuthResponse */
  login: (email, password) =>
    api.post('/api/v1/auth/login', { email, password }),

  /** POST /api/v1/auth/register → AuthResponse */
  register: (data) =>
    api.post('/api/v1/auth/register', data),

  /** POST /api/v1/auth/logout  (CSRF protected) */
  logout: () =>
    api.post('/api/v1/auth/logout'),

  /** POST /api/v1/auth/refresh → RefreshResponse  (CSRF protected) */
  refresh: () =>
    api.post('/api/v1/auth/refresh'),
}
