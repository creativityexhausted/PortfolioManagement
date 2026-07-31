/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      colors: {
        fintech: {
          navy: "#020617",
          slate: "#0f172a",
          cyan: "#22d3ee",
          indigo: "#6366f1",
          emerald: "#10b981",
        },
      },
      boxShadow: {
        glow: "0 0 0 1px rgba(34,211,238,0.2), 0 8px 24px rgba(2,6,23,0.45)",
      },
    },
  },
  plugins: [],
}

