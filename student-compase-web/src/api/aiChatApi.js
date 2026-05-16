import api from './axiosInstance'

export const aiChatApi = {
  getHistory:  (limit = 20) => api.get('/api/v1/ai/chat/history', { params: { limit } }),
  sendMessage: (message)    => api.post('/api/v1/ai/chat', { message }),
}
