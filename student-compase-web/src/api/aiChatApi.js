import api from './axiosInstance'

export const aiChatApi = {
  // Persistent chat — saves history, no source field
  getHistory:    (limit = 20) => api.get('/api/v1/ai/chat/history', { params: { limit } }),
  sendMessage:   (message)    => api.post('/api/v1/ai/chat', { message }),

  // Stateless chat — Multi-LLM HA endpoint, returns { reply, source }
  statelessChat: (message)    => api.post('/api/v1/recommendations/chat', { message }),
}
