import { useState, useEffect, useCallback } from 'react'
import { flushSync } from 'react-dom'
import { authApi } from '../api/authApi'
import { accountApi } from '../api/accountApi'
import { AuthContext } from './AuthContextValue'

const TOKEN_KEY = 'stufi_access_token'
const USER_KEY  = 'stufi_user'

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try {
      const raw = localStorage.getItem(USER_KEY)
      return raw ? JSON.parse(raw) : null
    } catch {
      return null
    }
  })
  const [token, setToken]           = useState(() => localStorage.getItem(TOKEN_KEY))
  const [loading, setLoading]       = useState(false)
  const [initialized, setInit]      = useState(false)

  // On mount: validate stored token against /account/me
  useEffect(() => {
    const validate = async () => {
      if (token) {
        try {
          const { data } = await accountApi.getMe()
          setUser(data)
          localStorage.setItem(USER_KEY, JSON.stringify(data))
        } catch {
          localStorage.removeItem(TOKEN_KEY)
          localStorage.removeItem(USER_KEY)
          setToken(null)
          setUser(null)
        }
      }
      setInit(true)
    }
    validate()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const _save = useCallback((accessToken, profile) => {
    localStorage.setItem(TOKEN_KEY, accessToken)
    localStorage.setItem(USER_KEY, JSON.stringify(profile))
    flushSync(() => {
      setToken(accessToken)
      setUser(profile)
    })
  }, [])

  const login = useCallback(async (email, password) => {
    setLoading(true)
    try {
      const { data } = await authApi.login(email, password)
      _save(data.accessToken, data.user)
      return { success: true }
    } catch (err) {
      const msg = err.response?.data?.message || 'Login failed. Please try again.'
      return { success: false, error: msg }
    } finally {
      setLoading(false)
    }
  }, [_save])

  const register = useCallback(async (formData) => {
    setLoading(true)
    try {
      const { data } = await authApi.register(formData)
      _save(data.accessToken, data.user)
      return { success: true }
    } catch (err) {
      const msg = err.response?.data?.message || 'Registration failed. Please try again.'
      return { success: false, error: msg }
    } finally {
      setLoading(false)
    }
  }, [_save])

  const logout = useCallback(async () => {
    try {
      await authApi.logout()
    } catch {
      // silently continue — local cleanup is what matters
    } finally {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
      setToken(null)
      setUser(null)
    }
  }, [])

  return (
    <AuthContext.Provider
      value={{ user, token, loading, initialized, login, register, logout }}
    >
      {children}
    </AuthContext.Provider>
  )
}
