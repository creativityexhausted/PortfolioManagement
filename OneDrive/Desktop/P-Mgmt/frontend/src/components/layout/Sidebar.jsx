import { AnimatePresence, motion } from "framer-motion";
import {
  Bot,
  ChevronRight,
  ClipboardList,
  Flame,
  LayoutDashboard,
  LogOut,
  Menu,
  MessageSquare,
  Newspaper,
  PieChart,
  Settings,
  Star,
  TrendingUp,
  Wallet,
  X,
} from "lucide-react";
import { NavLink } from "react-router-dom";
import { useMemo } from "react";
import { formatDateTime } from "../../utils/formatters";

export const Sidebar = ({
  sessions = [],
  collapsed,
  onToggle,
  onOpenAssistant,
  onOpenChatSession,
  onLogout,
}) => {
  const navItems = useMemo(
    () => [
      { to: "/", label: "Dashboard", icon: LayoutDashboard },
      { to: "/holdings", label: "Holdings", icon: PieChart },
      { to: "/watchlist", label: "Watchlist", icon: Star },
      { to: "/transactions", label: "Transactions", icon: ClipboardList },
      { to: "/market", label: "Market & News", icon: Newspaper },
      { to: "/settings", label: "Settings", icon: Settings },
    ],
    [],
  );

  const recentSessions = useMemo(() => [...sessions].slice(0, 4), [sessions]);

  return (
    <>
      {/* Mobile Menu Toggle Button */}
      <button
        type="button"
        className="fixed left-4 top-4 z-50 rounded-lg border border-outline-variant bg-surface-container p-2 text-on-surface shadow-lg lg:hidden"
        onClick={onToggle}
        aria-label="Toggle sidebar"
      >
        {collapsed ? <Menu className="h-5 w-5" /> : <X className="h-5 w-5" />}
      </button>

      <AnimatePresence>
        {!collapsed && (
          <motion.aside
            initial={{ x: -260, opacity: 0 }}
            animate={{ x: 0, opacity: 1 }}
            exit={{ x: -260, opacity: 0 }}
            transition={{ duration: 0.2 }}
            className="fixed inset-y-0 left-0 z-50 flex h-screen w-64 flex-col border-r border-outline-variant bg-surface px-sm py-md text-on-surface shadow-2xl lg:static lg:z-30"
          >
            {/* Header / Brand */}
            <div className="mb-md px-sm">
              <div className="flex items-center justify-between">
                <div>
                  <h1 className="font-headline-md text-2xl font-bold tracking-tight text-primary">
                    ProTrader
                  </h1>
                  <p className="font-label-caps text-xs text-on-surface-variant opacity-70">
                    Premium Account
                  </p>
                </div>
                <button
                  type="button"
                  className="rounded-md p-1 text-on-surface-variant hover:text-on-surface lg:hidden"
                  onClick={onToggle}
                  aria-label="Close sidebar"
                >
                  <X className="h-5 w-5" />
                </button>
              </div>
            </div>

            {/* AI Assistant Quick Trigger */}
            <div className="mb-sm px-xs">
              <button
                type="button"
                onClick={onOpenAssistant}
                className="flex w-full items-center justify-center gap-2 rounded-lg bg-primary py-2.5 px-4 font-bold text-on-primary shadow-md transition-all hover:brightness-110 active:scale-95"
              >
                <Bot className="h-4 w-4" />
                Ask AI Assistant
              </button>
            </div>

            {/* Nav Menu */}
            <nav className="flex-1 space-y-1 overflow-y-auto custom-scrollbar pr-1">
              {navItems.map((item) => {
                const Icon = item.icon;
                return (
                  <NavLink
                    key={item.to}
                    to={item.to}
                    className={({ isActive }) =>
                      `flex items-center justify-between rounded-lg p-sm font-label-caps text-body-sm transition-all duration-200 cursor-pointer ${
                        isActive
                          ? "border-r-2 border-primary bg-surface-container-high/80 text-primary font-bold shadow-sm"
                          : "text-on-surface-variant hover:bg-secondary-container/50 hover:text-on-surface"
                      }`
                    }
                  >
                    <span className="flex items-center gap-sm">
                      <Icon className="h-4 w-4 shrink-0" />
                      <span>{item.label}</span>
                    </span>
                    <ChevronRight className="h-3.5 w-3.5 opacity-40" />
                  </NavLink>
                );
              })}

              {/* AI Conversations Sub-menu */}
              <div className="pt-md">
                <p className="px-xs pb-xs font-label-caps text-[11px] uppercase tracking-wider text-on-surface-variant opacity-70">
                  Recent AI Chats
                </p>
                {recentSessions.length === 0 ? (
                  <p className="px-xs text-xs text-on-surface-variant/60 italic">
                    No conversations yet
                  </p>
                ) : (
                  recentSessions.map((session) => (
                    <button
                      key={session.id}
                      type="button"
                      onClick={() => onOpenChatSession(session.id)}
                      className="group flex w-full items-center justify-between rounded-lg px-xs py-1.5 text-left text-xs text-on-surface-variant hover:bg-surface-container-high hover:text-on-surface"
                    >
                      <span className="truncate max-w-[140px] font-medium">
                        {session.title}
                      </span>
                      <MessageSquare className="h-3.5 w-3.5 shrink-0 opacity-40 group-hover:opacity-100 group-hover:text-primary" />
                    </button>
                  ))
                )}
              </div>
            </nav>

            {/* Bottom Upgrade Card & Logout */}
            <div className="mt-md space-y-sm">
              <div className="glass-panel p-sm rounded-xl">
                <div className="flex items-center gap-2 mb-2">
                  <Flame className="h-4 w-4 text-primary" />
                  <span className="text-xs font-bold text-on-surface">Pro Trader Plus</span>
                </div>
                <button
                  type="button"
                  onClick={onOpenAssistant}
                  className="w-full py-1.5 bg-surface-container-high text-primary font-bold text-xs rounded-lg hover:bg-surface-bright transition-all"
                >
                  Upgrade Active
                </button>
              </div>

              <button
                type="button"
                onClick={onLogout}
                className="flex w-full items-center justify-center gap-2 rounded-lg border border-error/20 bg-error/10 py-2 text-xs font-medium text-error hover:bg-error/20 transition-colors"
              >
                <LogOut className="h-3.5 w-3.5" />
                Sign Out
              </button>
            </div>
          </motion.aside>
        )}
      </AnimatePresence>
    </>
  );
};
