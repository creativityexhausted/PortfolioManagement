import { AnimatePresence, motion } from "framer-motion";
import {
  Bot,
  ChartNoAxesCombined,
  ChevronRight,
  ClipboardList,
  Gauge,
  Landmark,
  LineChart,
  Menu,
  MessageSquare,
  Newspaper,
  LogOut,
  Settings,
  Star,
  X,
} from "lucide-react";
import { NavLink } from "react-router-dom";
import { useMemo } from "react";
import { formatDateTime } from "../../utils/formatters";

export const Sidebar = ({
  sessions,
  collapsed,
  onToggle,
  onOpenAssistant,
  onOpenChatSession,
  onLogout,
}) => {
  const navItems = useMemo(
    () => [
      { to: "/", label: "Dashboard", icon: Gauge },
      { to: "/portfolio", label: "Portfolio", icon: Landmark },
      { to: "/holdings", label: "Holdings", icon: ChartNoAxesCombined },
      { to: "/transactions", label: "Transactions", icon: ClipboardList },
      { to: "/watchlist", label: "Watchlist", icon: Star },
      { to: "/market", label: "Market", icon: LineChart },
      { to: "/news", label: "News", icon: Newspaper },
      { to: "/settings", label: "Settings", icon: Settings },
    ],
    [],
  );

  const recentSessions = useMemo(() => [...sessions].slice(0, 5), [sessions]);

  return (
    <>
      <button
        type="button"
        className="fixed left-4 top-4 z-50 rounded-lg border border-white/10 bg-slate-900/80 p-2 text-slate-200 shadow-lg lg:hidden"
        onClick={onToggle}
        aria-label="Toggle sidebar"
      >
        {collapsed ? <Menu className="h-4 w-4" /> : <X className="h-4 w-4" />}
      </button>

      <AnimatePresence>
        {!collapsed && (
          <motion.aside
            initial={{ x: -40, opacity: 0 }}
            animate={{ x: 0, opacity: 1 }}
            exit={{ x: -40, opacity: 0 }}
            transition={{ duration: 0.25 }}
            className="pm-surface fixed inset-y-0 left-0 z-40 w-[20rem] border-r border-white/10 bg-slate-950/90 backdrop-blur-xl lg:static lg:z-10"
          >
            <div className="flex h-full flex-col gap-4 p-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-xs uppercase tracking-[0.3em] text-cyan-300/70">Portfolio System</p>
                  <h1 className="text-lg font-semibold text-slate-100">Investor Workspace</h1>
                </div>
                <button
                  type="button"
                  className="rounded-md border border-white/10 p-1.5 text-slate-400 hover:text-slate-100 lg:hidden"
                  onClick={onToggle}
                  aria-label="Close sidebar"
                >
                  <X className="h-4 w-4" />
                </button>
              </div>

              <button
                type="button"
                onClick={onOpenAssistant}
                className="group flex items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-emerald-400 to-emerald-500 px-4 py-2 text-sm font-medium text-emerald-950 shadow-lg shadow-emerald-900/30 transition hover:shadow-emerald-800/60"
              >
                <Bot className="h-4 w-4" />
                Ask AI Assistant
              </button>

              <nav className="space-y-1 rounded-xl border border-white/10 bg-slate-900/40 p-2">
                {navItems.map((item) => {
                  const Icon = item.icon;
                  return (
                    <NavLink
                      key={item.to}
                      to={item.to}
                      className={({ isActive }) =>
                        `flex items-center justify-between rounded-lg px-3 py-2 text-sm transition ${
                          isActive
                            ? "bg-cyan-500/20 text-cyan-100"
                            : "text-slate-300 hover:bg-white/10 hover:text-slate-100"
                        }`
                      }
                    >
                      <span className="inline-flex items-center gap-2">
                        <Icon className="h-4 w-4" />
                        {item.label}
                      </span>
                      <ChevronRight className="h-3.5 w-3.5 opacity-60" />
                    </NavLink>
                  );
                })}
              </nav>

              <div className="flex-1 space-y-2 overflow-y-auto pr-1">
                <h2 className="text-xs uppercase tracking-wide text-slate-500">Recent AI Conversations</h2>
                {recentSessions.length === 0 ? (
                  <p className="rounded-lg border border-dashed border-white/15 bg-slate-900/40 p-3 text-xs text-slate-500">
                    No AI conversation yet.
                  </p>
                ) : (
                  recentSessions.map((session) => (
                    <motion.button
                      layout
                      key={session.id}
                      type="button"
                      onClick={() => onOpenChatSession(session.id)}
                      className="group w-full rounded-xl border border-white/5 bg-slate-900/50 p-3 text-left transition hover:border-white/15"
                    >
                      <div className="flex items-start justify-between gap-2">
                        <div className="min-w-0">
                          <p className="truncate text-sm font-medium text-slate-100">{session.title}</p>
                          <p className="mt-1 text-[11px] text-slate-400">{formatDateTime(session.updatedAt)}</p>
                        </div>
                        <MessageSquare className="mt-0.5 h-4 w-4 shrink-0 text-slate-500" />
                      </div>
                    </motion.button>
                  ))
                )}
              </div>

              <button
                type="button"
                onClick={onLogout}
                className="mt-auto flex items-center gap-2 rounded-xl border border-rose-300/30 bg-rose-500/10 px-3 py-2 text-sm text-rose-200 hover:bg-rose-500/20"
              >
                <LogOut className="h-4 w-4" />
                Logout
              </button>
            </div>
          </motion.aside>
        )}
      </AnimatePresence>
    </>
  );
};
