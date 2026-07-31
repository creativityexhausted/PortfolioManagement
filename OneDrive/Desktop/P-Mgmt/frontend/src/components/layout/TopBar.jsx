import { ChevronDown, LayoutPanelLeft, Sparkles } from "lucide-react";
import { ThemeToggle } from "../common/ThemeToggle";

export const TopBar = ({
  username,
  sectionTitle,
  portfolios,
  selectedPortfolioId,
  onPortfolioChange,
  onToggleSidebar,
  onOpenAssistant,
  theme,
  onThemeToggle,
}) => (
  <header className="sticky top-0 z-20 border-b border-white/10 bg-slate-950/70 px-4 py-3 backdrop-blur-xl">
    <div className="flex flex-wrap items-center justify-between gap-3">
      <div className="flex items-center gap-3">
        <button
          type="button"
          className="rounded-lg border border-white/10 bg-slate-900/70 p-2 text-slate-300"
          onClick={onToggleSidebar}
          aria-label="Toggle sidebar"
        >
          <LayoutPanelLeft className="h-4 w-4" />
        </button>
        <div>
          <p className="text-[11px] uppercase tracking-[0.25em] text-cyan-300/70">Portfolio Intelligence</p>
          <p className="text-sm font-medium text-slate-100">{sectionTitle} | {username || "Investor"}</p>
        </div>
      </div>

      <div className="flex items-center gap-2">
        <label className="relative">
          <ChevronDown className="pointer-events-none absolute right-2 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-slate-400" />
          <select
            value={selectedPortfolioId ?? "all"}
            onChange={(event) => {
              const value = event.target.value;
              onPortfolioChange(value === "all" ? null : Number(value));
            }}
            className="appearance-none rounded-full border border-white/10 bg-slate-900/70 px-3 py-1.5 pr-7 text-xs text-slate-100"
            aria-label="Select portfolio"
          >
            <option value="all">All portfolios</option>
            {portfolios.map((portfolio) => (
              <option key={portfolio.id} value={portfolio.id}>
                {portfolio.name}
              </option>
            ))}
          </select>
        </label>

        <button
          type="button"
          onClick={onOpenAssistant}
          className="rounded-full border border-cyan-300/40 bg-cyan-500/10 p-2 text-cyan-200"
          aria-label="Open AI assistant"
        >
          <Sparkles className="h-4 w-4" />
        </button>

        <ThemeToggle theme={theme} onToggle={onThemeToggle} />
      </div>
    </div>
  </header>
);
