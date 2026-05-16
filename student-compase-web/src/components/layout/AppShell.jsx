import Sidebar from './Sidebar'
import BottomNav from './BottomNav'
import InstallBanner from '../common/InstallBanner'

export default function AppShell({ children }) {
  return (
    <div className="flex h-screen bg-slate-900 overflow-hidden">
      <Sidebar />
      <main className="flex-1 md:ml-64 overflow-y-auto">
        {children}
      </main>
      <BottomNav />
      <InstallBanner />
    </div>
  )
}
