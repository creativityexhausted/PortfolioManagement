import { Bell, ChevronDown, Search, Settings, Sparkles, User } from "lucide-react";

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
  <header className="sticky top-0 z-40 flex h-16 w-full items-center justify-between border-b border-outline-variant bg-surface-container/80 px-lg backdrop-blur-xl">
    {/* Left Search Bar & Section Title */}
    <div className="flex items-center gap-md flex-1">
      <div className="relative w-72 md:w-96">
        <Search className="absolute left-sm top-1/2 h-4 w-4 -translate-y-1/2 text-on-surface-variant" />
        <input
          type="text"
          value={searchQuery}
          onChange={(e) => onSearchChange(e.target.value)}
          placeholder="Search markets, stocks, indices..."
          className="w-full rounded-full bg-surface-dim border-none py-xs pl-xl pr-md text-body-sm text-on-surface placeholder:text-on-surface-variant/60 focus:outline-none focus:ring-1 focus:ring-primary"
        />
      </div>


    </div>

    {/* Right Icons & Executive Profile */}
    <div className="flex items-center gap-md">
      <div className="flex items-center gap-sm mr-sm">
        <button
          type="button"
          onClick={onOpenAssistant}
          className="p-1.5 text-on-surface-variant hover:text-primary transition-colors"
          title="AI Assistant"
        >
          <Sparkles className="h-5 w-5" />
        </button>
        <button
          type="button"
          className="p-1.5 text-on-surface-variant hover:text-primary transition-colors"
          title="Notifications"
        >
          <Bell className="h-5 w-5" />
        </button>
      </div>

      <div className="flex items-center gap-sm border-l border-outline-variant pl-md">
        <div className="text-right hidden lg:block">
          <p className="font-label-caps text-label-caps text-on-surface font-semibold">
            {username || "Alex Rivera"}
          </p>
          <p className="text-[10px] text-on-surface-variant uppercase tracking-widest">
            Executive Tier
          </p>
        </div>
        <div className="flex h-9 w-9 items-center justify-center rounded-full border border-outline-variant bg-surface-bright text-primary font-bold">
          {username ? username.charAt(0).toUpperCase() : <User className="h-4 w-4" />}
        </div>
      </div>
    </div>
  </header>
);
