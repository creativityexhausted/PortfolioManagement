import { Moon, Sun } from "lucide-react";

export const ThemeToggle = ({ theme, onToggle }) => (
  <button
    type="button"
    onClick={onToggle}
    className="pm-surface group inline-flex items-center gap-2 rounded-full border border-white/15 bg-slate-900/60 px-3 py-1.5 text-xs text-slate-200 transition hover:border-cyan-300/60"
    aria-label="Toggle theme"
  >
    {theme === "dark" ? <Moon className="h-3.5 w-3.5" /> : <Sun className="h-3.5 w-3.5" />}
    {theme === "dark" ? "Switch to Light" : "Switch to Dark"}
  </button>
);
