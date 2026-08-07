import { motion } from "framer-motion";

export const LoadingDots = ({ className = "" }) => (
  <div className={`inline-flex items-center gap-1 ${className}`} aria-label="Loading">
    {[0, 1, 2].map((index) => (
      <motion.span
        key={index}
        className="h-1.5 w-1.5 rounded-full bg-cyan-300"
        animate={{ opacity: [0.2, 1, 0.2], y: [0, -2, 0] }}
        transition={{ duration: 0.9, repeat: Infinity, delay: index * 0.15 }}
      />
    ))}
  </div>
);
