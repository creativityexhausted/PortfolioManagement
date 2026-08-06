import { AnimatePresence, motion } from "framer-motion";
import {
  Bot,
  ClipboardList,
  Flame,
  LayoutDashboard,
  LogOut,
  Menu,
  MessageSquare,
  Newspaper,
  PanelLeftClose,
  PanelLeftOpen,
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
import { AtlasLogo } from "../common/AtlasLogo";

export const Sidebar = ({
  sessions = [],
  collapsed,
  onToggle,
  rail = false,
  onToggleRail = () => {},
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
        className="fixed left-4 top-4 z-50 rounded-lg glass-pill p-2 text-on-surface shadow-lg lg:hidden"
        onClick={onToggle}
        aria-label="Toggle sidebar"
      >
        {collapsed ? <Menu className="h-5 w-5" /> : <X className="h-5 w-5" />}
      </button>

      <AnimatePresence>
        {!collapsed && (
          <motion.aside
            initial={{ x: -260, opacity: 0 }}
            animate={{ x: 0, opacity: 1, width: rail ? 84 : 256 }}
            exit={{ x: -260, opacity: 0 }}
            transition={{ duration: 0.25, ease: [0.32, 0.72, 0, 1] }}
            className="fixed inset-y-0 left-0 z-50 flex h-screen flex-col glass-surface border-r border-white/5 px-sm py-md text-on-surface shadow-2xl lg:static lg:z-30"
          >
            {/* Header / Brand */}
            <div className="mb-md px-sm">
              <div className={`flex items-center ${rail ? "flex-col gap-sm" : "justify-between"}`}>
                <div className={`flex items-center gap-2.5 ${rail ? "flex-col" : ""}`}>
                  <AtlasLogo iconClassName="h-5 w-5" boxClassName="h-9 w-9" />
                  {!rail && (
                    <div>
                      <h1 className="font-headline-md text-xl font-bold tracking-tight text-on-surface">
                        Atlas
                      </h1>
                      <p className="font-label-caps text-[10px] text-on-surface-variant opacity-70 -mt-0.5">
                        Premium Account
                      </p>
                    </div>
                  )}
                </div>

                {/* Minimize / Expand Rail Toggle (desktop only) — sits inline in the header, never clipped */}
                <button
                  type="button"
                  onClick={onToggleRail}
                  title={rail ? "Expand sidebar" : "Minimize sidebar"}
                  className={`hidden lg:flex h-7 w-7 shrink-0 items-center justify-center rounded-full glass-btn text-on-surface-variant hover:text-primary transition-all ${rail ? "" : ""}`}
                >
                  {rail ? <PanelLeftOpen className="h-4 w-4" /> : <PanelLeftClose className="h-4 w-4" />}
                </button>

                {!rail && (
                  <button
                    type="button"
                    className="rounded-md p-1 text-on-surface-variant hover:text-on-surface lg:hidden"
                    onClick={onToggle}
                    aria-label="Close sidebar"
                  >
                    <X className="h-5 w-5" />
                  </button>
                )}
              </div>
            </div>

            {/* AI Assistant Quick Trigger */}
            <div className="mb-sm px-xs">
              <button
                type="button"
                onClick={onOpenAssistant}
                title="Ask AI Assistant"
                className="flex w-full items-center justify-center gap-2 rounded-xl bg-primary py-2.5 px-4 font-bold text-on-primary shadow-[0_4px_16px_rgba(74,222,128,0.25)] transition-all hover:brightness-110 hover:shadow-[0_6px_20px_rgba(74,222,128,0.35)] active:scale-95"
              >
                <Bot className="h-4 w-4 shrink-0" />
                {!rail && <span className="leading-none">Ask TARS</span>}
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
                    title={rail ? item.label : undefined}
                    className={({ isActive }) =>
                      `flex items-center rounded-xl p-sm font-label-caps text-body-sm transition-all duration-200 cursor-pointer ${
                        rail ? "justify-center" : "justify-between"
                      } ${
                        isActive
                          ? "glass-pill text-primary font-bold shadow-sm"
                          : "text-on-surface-variant hover:bg-white/[0.04] hover:text-on-surface"
                      }`
                    }
                  >
                    <span className="flex items-center gap-sm">
                      <Icon className="h-4 w-4 shrink-0" />
                      {!rail && <span>{item.label}</span>}
                    </span>
                  </NavLink>
                );
              })}

              {/* AI Conversations Sub-menu */}
              {!rail && (
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
                        className="group flex w-full items-center justify-between rounded-xl px-xs py-1.5 text-left text-xs text-on-surface-variant hover:bg-white/[0.04] hover:text-on-surface transition-colors"
                      >
                        <span className="truncate max-w-[140px] font-medium">
                          {session.title}
                        </span>
                        <MessageSquare className="h-3.5 w-3.5 shrink-0 opacity-40 group-hover:opacity-100 group-hover:text-primary" />
                      </button>
                    ))
                  )}
                </div>
              )}
            </nav>

            {/* Bottom Upgrade Card & Logout */}
            <div className="mt-md space-y-sm">
              {!rail && (
                <div className="glass-panel p-sm rounded-2xl">
                  <div className="flex items-center gap-2 mb-2">
                    <Flame className="h-4 w-4 text-primary" />
                    <span className="text-xs font-bold text-on-surface">Atlas Plus</span>
                  </div>
                  <button
                    type="button"
                    onClick={onOpenAssistant}
                    className="w-full py-1.5 glass-pill text-primary font-bold text-xs rounded-lg hover:brightness-125 transition-all"
                  >
                    Upgrade Active
                  </button>
                </div>
              )}

              <button
                type="button"
                onClick={onLogout}
                title={rail ? "Sign Out" : undefined}
                className="flex w-full items-center justify-center gap-2 rounded-xl border border-error/20 bg-error/10 py-2 text-xs font-medium text-error hover:bg-error/20 transition-colors"
              >
                <LogOut className="h-3.5 w-3.5 shrink-0" />
                {!rail && "Sign Out"}
              </button>
            </div>
          </motion.aside>
        )}
      </AnimatePresence>
    </>
  );
};
