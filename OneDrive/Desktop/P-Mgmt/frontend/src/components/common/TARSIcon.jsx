/* TARS monolith — narrow 1:3 proportions, sensor visor, segmented panels */
export function TARSIcon({ className = "h-4 w-4" }) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className={className}
      aria-hidden="true"
    >
      {/* Tall narrow monolith body — 1:2.5 aspect ratio like real TARS */}
      <rect x="7.5" y="0.5" width="9" height="23" rx="1.75" fill="currentColor" />

      {/* Sensor visor housing — the most distinctive TARS feature */}
      <rect x="7.5" y="3.5" width="9" height="4" rx="0.5" fill="black" fillOpacity="0.50" />
      {/* Aperture slit — bright scanner line inside the dark visor */}
      <rect x="8.5" y="5.2" width="7" height="0.9" rx="0.45" fill="white" fillOpacity="0.40" />

      {/* Upper panel joint */}
      <rect x="7.5" y="12.5" width="9" height="0.8" fill="black" fillOpacity="0.35" />
      {/* Lower panel joint */}
      <rect x="7.5" y="18.5" width="9" height="0.8" fill="black" fillOpacity="0.25" />

      {/* Side hinge notches at upper joint — suggest the folding mechanism */}
      <rect x="7.5" y="12" width="1.5" height="1.8" rx="0.3" fill="black" fillOpacity="0.45" />
      <rect x="15" y="12" width="1.5" height="1.8" rx="0.3" fill="black" fillOpacity="0.45" />

      {/* Vertical centre seam */}
      <rect x="11.75" y="0.5" width="0.75" height="23" fill="black" fillOpacity="0.13" />
    </svg>
  );
}

export default TARSIcon;
