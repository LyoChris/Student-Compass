import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from '../contexts/AuthContext'
import ProtectedRoute from '../components/layout/ProtectedRoute'
import AppShell from '../components/layout/AppShell'

// Public pages
import LandingPage  from '../pages/landing/LandingPage'
import LoginPage    from '../pages/auth/LoginPage'
import RegisterPage from '../pages/auth/RegisterPage'

// Protected app pages
import DashboardPage    from '../pages/dashboard/DashboardPage'
import MarketplacePage  from '../pages/marketplace/MarketplacePage'
import CreateAdForm     from '../pages/marketplace/CreateAdForm'
import MarketplaceItemDetails from '../pages/marketplace/MarketplaceItemDetails'
import AIChatPage       from '../pages/chat/AIChatPage'
import RadarPage        from '../pages/radar/RadarPage'
import OnboardingWizard from '../pages/onboarding/OnboardingWizard'
import ProfilePage      from '../pages/profile/ProfilePage'
import BudgetDashboard             from '../pages/budget/BudgetDashboard'
import MyListingsDashboard          from '../pages/marketplace/MyListingsDashboard'
import AiCategoryRecommendations   from '../pages/recommendations/AiCategoryRecommendations'

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
          <Route path="/marketplace" element={<Guard><MarketplacePage /></Guard>} />
          <Route path="/marketplace/me"   element={<Guard><MyListingsDashboard /></Guard>}                         />
          <Route path="/marketplace/sell" element={<Guard><AppShellRoute><CreateAdForm /></AppShellRoute></Guard>} />
          <Route path="/marketplace/:id" element={<Guard><AppShellRoute><MarketplaceItemDetails /></AppShellRoute></Guard>} />
          <Route path="/chat"       element={<Guard><AIChatPage /></Guard>}       />
          <Route path="/radar"      element={<Guard><RadarPage /></Guard>}        />
          <Route path="/profile"    element={<Guard><ProfilePage /></Guard>}      />
          <Route path="/budget"           element={<Guard><BudgetDashboard /></Guard>}                  />
          <Route path="/recommendations" element={<Guard><AiCategoryRecommendations /></Guard>}       />

          {/* Fallback */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}

function AppShellRoute({ children }) {
  return <AppShell>{children}</AppShell>
}
