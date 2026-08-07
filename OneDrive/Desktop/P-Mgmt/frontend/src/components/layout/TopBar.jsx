import { Bell, Search, User } from "lucide-react";

export const TopBar = ({
  username,
  sectionTitle,
  portfolios = [],
  selectedPortfolioId,
  onPortfolioChange,
  onOpenAssistant,
  searchQuery = "",
  onSearchChange = () => {},
}) => (
  <header className="sticky top-0 z-40 flex h-16 w-full items-center justify-between border-b border-white/5 glass-surface px-lg">
    {/* Left Search Bar & Section Title */}
    <div className="flex items-center gap-md flex-1">
      <div className="relative w-72 md:w-96">
        <Search className="absolute left-sm top-1/2 h-4 w-4 -translate-y-1/2 text-on-surface-variant" />
        <input
          type="text"
          value={searchQuery}
          onChange={(e) => onSearchChange(e.target.value)}
          placeholder="Search markets, stocks, indices..."
          className="glass-pill w-full rounded-full py-xs pl-xl pr-md text-body-sm text-on-surface placeholder:text-on-surface-variant/60 focus:outline-none focus:ring-1 focus:ring-primary/60"
        />
      </div>


    </div>

    {/* Right Icons & Executive Profile */}
    <div className="flex items-center gap-md">
      <div className="flex items-center gap-sm mr-sm">
        <button
          type="button"
          className="glass-btn p-2 rounded-full text-on-surface-variant hover:text-primary transition-colors"
          title="Notifications"
        >
          <Bell className="h-4.5 w-4.5" />
        </button>
      </div>

      <div className="flex items-center gap-sm border-l border-white/10 pl-md">
        <div className="text-right hidden lg:block">
          <p className="font-label-caps text-label-caps text-on-surface font-semibold">
            {username || "Alex Rivera"}
          </p>
          <p className="text-[10px] text-on-surface-variant uppercase tracking-widest">
            Executive Tier
          </p>
        </div>
        <div className="flex h-9 w-9 items-center justify-center rounded-full glass-pill text-primary font-bold">
          {username ? username.charAt(0).toUpperCase() : <User className="h-4 w-4" />}
        </div>
      </div>
    </div>
  </header>
);
