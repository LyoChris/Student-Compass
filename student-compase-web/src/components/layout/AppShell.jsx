import Sidebar from './Sidebar'
import MobileNav from './MobileNav'
import InstallBanner from '../common/InstallBanner'

export default function AppShell({ children }) {
  return (
    <div className="flex min-h-screen bg-slate-900 overflow-x-hidden">
      {/* Desktop sidebar — hidden on mobile */}
      <Sidebar />

      {/* Mobile: fixed top header + slide-in drawer */}
      <MobileNav />

      {/* Main content area */}
      <main className="flex-1 md:ml-64 w-full overflow-x-hidden">
        {/*
          pt-14: clears the fixed mobile header (h-14 = 56px)
          md:pt-0: desktop has no fixed header, sidebar is beside it
        */}
        <div className="pt-14 md:pt-0">
          {children}
        </div>
      </main>

      <InstallBanner />
    </div>
  )
}
