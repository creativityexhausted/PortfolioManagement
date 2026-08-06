import { motion } from "framer-motion";
import { ArrowUpRight, Sparkles } from "lucide-react";

export const WelcomeScreen = ({ onPromptClick, prompts }) => {
  const greeting = new Date().getHours() < 12 ? "Good Morning" : new Date().getHours() < 18 ? "Good Afternoon" : "Good Evening";

  return (
    <div className="flex h-full flex-col items-center justify-center px-6 text-center">
      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        className="max-w-3xl"
      >
        <div className="mx-auto mb-5 inline-flex rounded-full border border-cyan-300/40 bg-cyan-500/10 px-3 py-1 text-xs text-cyan-200">
          <Sparkles className="mr-2 h-4 w-4" /> TARS
        </div>

        <h2 className="text-4xl font-semibold tracking-tight text-slate-100 sm:text-5xl">{greeting}</h2>
        <p className="mt-3 text-base text-slate-300 sm:text-lg">How can I help you with your investments today?</p>

        <div className="mt-8 grid gap-2 text-left sm:grid-cols-2">
          {prompts.slice(0, 6).map((item) => (
            <button
              key={item.label}
              type="button"
              onClick={() => onPromptClick(item.prompt)}
              className="group rounded-xl border border-white/10 bg-slate-900/60 p-3 text-sm text-slate-200 transition hover:border-cyan-300/60 hover:bg-cyan-500/10"
            >
              <div className="flex items-center justify-between gap-3">
                <span>{item.label}</span>
                <ArrowUpRight className="h-4 w-4 text-slate-500 transition group-hover:text-cyan-200" />
              </div>
            </button>
          ))}
        </div>
      </motion.div>
    </div>
  );
};
