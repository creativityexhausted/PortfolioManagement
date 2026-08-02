/** @type {import('tailwindcss').Config} */
export default {
  darkMode: "class",
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      colors: {
        "on-tertiary": "#283044",
        "secondary-fixed-dim": "#b9c7e0",
        "on-secondary-fixed": "#0d1c2f",
        "background": "#101415",
        "on-secondary-container": "#abb9d2",
        "primary": "#4be277",
        "on-primary": "#003915",
        "on-error": "#690005",
        "primary-container": "#22c55e",
        "on-surface-variant": "#bccbb9",
        "tertiary-fixed-dim": "#bec6e0",
        "surface-bright": "#363a3b",
        "surface-container": "#1d2022",
        "tertiary-fixed": "#dae2fd",
        "on-background": "#e0e3e5",
        "secondary-fixed": "#d5e3fd",
        "inverse-surface": "#e0e3e5",
        "on-tertiary-fixed-variant": "#3f465c",
        "on-secondary-fixed-variant": "#3a485c",
        "on-primary-fixed": "#002109",
        "surface-variant": "#323537",
        "on-primary-container": "#004b1e",
        "on-surface": "#e0e3e5",
        "inverse-primary": "#006e2f",
        "surface-dim": "#101415",
        "on-secondary": "#233144",
        "outline-variant": "#3d4a3d",
        "inverse-on-surface": "#2d3133",
        "secondary-container": "#3c4a5e",
        "on-tertiary-container": "#383f54",
        "tertiary-container": "#a4abc4",
        "on-error-container": "#ffdad6",
        "surface-container-highest": "#323537",
        "error-container": "#93000a",
        "surface-container-low": "#191c1e",
        "secondary": "#b9c7e0",
        "error": "#ffb4ab",
        "primary-fixed-dim": "#4ae176",
        "surface": "#101415",
        "surface-tint": "#4ae176",
        "surface-container-high": "#272a2c",
        "outline": "#869585",
        "on-tertiary-fixed": "#131b2e",
        "primary-fixed": "#6bff8f",
        "on-primary-fixed-variant": "#005321",
        "tertiary": "#bfc6e0",
        "surface-container-lowest": "#0b0f10"
      },
      borderRadius: {
        DEFAULT: "0.25rem",
        lg: "0.5rem",
        xl: "0.75rem",
        full: "9999px"
      },
      spacing: {
        sm: "16px",
        xs: "8px",
        base: "4px",
        md: "24px",
        xl: "64px",
        gutter: "20px",
        "margin-safe": "32px",
        lg: "40px"
      },
      fontFamily: {
        sans: ["Inter", "sans-serif"],
        "label-caps": ["Inter", "sans-serif"],
        "stat-lg": ["Inter", "sans-serif"],
        "body-sm": ["Inter", "sans-serif"],
        "body-base": ["Inter", "sans-serif"],
        "headline-md": ["Inter", "sans-serif"],
        "display-lg": ["Inter", "sans-serif"]
      }
    }
  },
  plugins: []
}
