/**
 * Atlas brand mark — a faceted, upward-pointing arrow/peak glyph.
 * Renders as a single-color SVG (uses `currentColor`) so it can be recolored
 * via the parent's `text-*` className, and an optional dark rounded "app icon"
 * container variant for use in nav bars / favicons.
 */
export function AtlasMark({ className = "h-6 w-6" }) {
  return (
    <svg
      viewBox="0 0 64 64"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className={className}
      aria-hidden="true"
    >
      {/* Left ascending pair of bars */}
      <rect x="-7" y="-23" width="14" height="46" rx="7" fill="currentColor" transform="translate(17,40) rotate(-45)" />
      <rect x="-7" y="-17" width="14" height="34" rx="7" fill="currentColor" opacity="0.9" transform="translate(17,50) rotate(-45)" />
      {/* Right ascending pair of bars (mirrored) */}
      <rect x="-7" y="-23" width="14" height="46" rx="7" fill="currentColor" transform="translate(47,40) rotate(45)" />
      <rect x="-7" y="-17" width="14" height="34" rx="7" fill="currentColor" opacity="0.9" transform="translate(47,50) rotate(45)" />
      {/* Vertical facet split beneath the peak */}
      <rect x="29" y="12" width="6" height="34" rx="3" fill="black" fillOpacity="0.55" />
    </svg>
  );
}

export function AtlasLogo({ iconClassName = "h-5 w-5", boxClassName = "h-9 w-9", showBox = true }) {
  if (!showBox) return <AtlasMark className={iconClassName} />;
  return (
    <div
      className={`flex items-center justify-center rounded-xl bg-gradient-to-b from-[#173327] to-[#0f1f18] border border-white/10 shadow-inner text-primary ${boxClassName}`}
    >
      <AtlasMark className={iconClassName} />
    </div>
  );
}

export default AtlasLogo;
