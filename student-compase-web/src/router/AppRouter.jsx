import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from '../contexts/AuthContext'
import ProtectedRoute from '../components/layout/ProtectedRoute'

// Public pages
import LandingPage  from '../pages/landing/LandingPage'
import LoginPage    from '../pages/auth/LoginPage'
import RegisterPage from '../pages/auth/RegisterPage'

// Protected app pages
import DashboardPage    from '../pages/dashboard/DashboardPage'
import MarketplacePage  from '../pages/marketplace/MarketplacePage'
import AIChatPage       from '../pages/chat/AIChatPage'
import RadarPage        from '../pages/radar/RadarPage'
import OnboardingWizard from '../pages/onboarding/OnboardingWizard'
import ProfilePage      from '../pages/profile/ProfilePage'

function Guard({ children }) {
  return <ProtectedRoute>{children}</ProtectedRoute>
}

export default function AppRouter() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          {/* Public */}
          <Route path="/"         element={<LandingPage />}  />
          <Route path="/login"    element={<LoginPage />}    />
          <Route path="/register" element={<RegisterPage />} />

          {/* Protected */}
          <Route path="/onboarding" element={<Guard><OnboardingWizard /></Guard>} />
          <Route path="/dashboard"  element={<Guard><DashboardPage /></Guard>}    />
          <Route path="/market"     element={<Guard><MarketplacePage /></Guard>}  />
          <Route path="/chat"       element={<Guard><AIChatPage /></Guard>}       />
          <Route path="/radar"      element={<Guard><RadarPage /></Guard>}        />
          <Route path="/profile"    element={<Guard><ProfilePage /></Guard>}      />

          {/* Fallback */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}
