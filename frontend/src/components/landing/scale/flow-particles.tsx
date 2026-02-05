
"use client"

// Flow particles for ambient animation
const PATH_D = `M 200 60 C 200 110, 250 160, 200 210 C 150 260, 100 310, 200 360 C 300 410, 350 460, 200 510 C 50 560, 100 610, 200 660 C 300 710, 250 760, 200 810`

export function FlowParticles() {
  return (
    <svg
      className="absolute left-1/2 top-0 h-[900px] w-[400px] -translate-x-1/2 pointer-events-none opacity-30"
      viewBox="0 0 400 870"
      fill="none"
      preserveAspectRatio="xMidYMin meet"
    >
      {/* Particle 1 */}
      <circle r="2.5" fill="hsl(var(--primary))">
        <animateMotion
          dur="10s"
          repeatCount="indefinite"
          path={PATH_D}
        />
        <animate
          attributeName="opacity"
          values="0;0.5;0.5;0"
          dur="10s"
          repeatCount="indefinite"
        />
      </circle>

      {/* Particle 2 */}
      <circle r="2" fill="hsl(var(--primary))">
        <animateMotion
          dur="12s"
          repeatCount="indefinite"
          begin="3s"
          path={PATH_D}
        />
        <animate
          attributeName="opacity"
          values="0;0.4;0.4;0"
          dur="12s"
          repeatCount="indefinite"
          begin="3s"
        />
      </circle>

      {/* Particle 3 */}
      <circle r="2" fill="hsl(var(--primary))">
        <animateMotion
          dur="14s"
          repeatCount="indefinite"
          begin="6s"
          path={PATH_D}
        />
        <animate
          attributeName="opacity"
          values="0;0.35;0.35;0"
          dur="14s"
          repeatCount="indefinite"
          begin="6s"
        />
      </circle>
    </svg>
  )
}
