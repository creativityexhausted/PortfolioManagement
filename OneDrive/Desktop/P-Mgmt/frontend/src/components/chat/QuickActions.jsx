import { motion } from "framer-motion";

export const QuickActions = ({ actions, onSend, disabled }) => (
  <div className="flex gap-2 overflow-x-auto pb-2">
    {actions.map((action, index) => (
      <motion.button
        key={action.label}
        type="button"
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: index * 0.03 }}
        onClick={() => onSend(action.prompt)}
        disabled={disabled}
        className="whitespace-nowrap rounded-full border border-white/15 bg-white/5 px-3 py-1.5 text-xs text-slate-200 transition hover:border-cyan-300/70 hover:bg-cyan-500/10 disabled:cursor-not-allowed disabled:opacity-50"
      >
        {action.label}
      </motion.button>
    ))}
  </div>
);
