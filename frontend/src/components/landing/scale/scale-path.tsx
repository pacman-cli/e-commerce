
"use client"

import { motion, MotionValue, useMotionValueEvent, useTransform } from "framer-motion"
import { useRef, useState } from "react"

interface ScalePathProps {
  scrollYProgress: MotionValue<number>
}

// Path matching the node positions (50, 200, 350, 500, 650, 800)
const PATH_D = `
  M 200 60
  C 200 110, 250 160, 200 210
  C 150 260, 100 310, 200 360
  C 300 410, 350 460, 200 510
  C 50 560, 100 610, 200 660
  C 300 710, 250 760, 200 810
`

// Path segments for burst animations
const PATH_SEGMENTS = [
  "M 200 60 C 200 110, 250 160, 200 210",
  "M 200 210 C 150 260, 100 310, 200 360",
  "M 200 360 C 300 410, 350 460, 200 510",
  "M 200 510 C 50 560, 100 610, 200 660",
  "M 200 660 C 300 710, 250 760, 200 810",
]

// Burst particle
function BurstParticle({ delay, pathD, size = 4 }: { delay: number; pathD: string; size?: number }) {
  return (
    <circle r={size} fill="hsl(var(--primary))">
      <animateMotion
        dur="1.2s"
        begin={`${delay}s`}
        fill="freeze"
        path={pathD}
      />
      <animate
        attributeName="opacity"
        values="0;0.6;0.6;0"
        dur="1.2s"
        begin={`${delay}s`}
        fill="freeze"
      />
    </circle>
  )
}

export function ScalePath({ scrollYProgress }: ScalePathProps) {
  const pathLength = 1000
  const strokeDashoffset = useTransform(scrollYProgress, [0, 1], [pathLength, 0])

  const [bursts, setBursts] = useState<{ id: number; segment: string }[]>([])
  const triggeredRef = useRef<Set<number>>(new Set())

  const thresholds = [0.15, 0.28, 0.42, 0.56, 0.70, 0.84]

  useMotionValueEvent(scrollYProgress, "change", (latest) => {
    thresholds.forEach((threshold, index) => {
      if (!triggeredRef.current.has(index) && latest >= threshold && latest < threshold + 0.06) {
        triggeredRef.current.add(index)

        const segmentIndex = Math.min(index, PATH_SEGMENTS.length - 1)
        const newBurst = {
          id: Date.now() + index,
          segment: PATH_SEGMENTS[segmentIndex],
        }

        setBursts(prev => [...prev, newBurst])

        setTimeout(() => {
          setBursts(prev => prev.filter(b => b.id !== newBurst.id))
          triggeredRef.current.delete(index)
        }, 2000)
      }
    })
  })

  return (
    <svg
      className="absolute left-1/2 top-0 h-[900px] w-[400px] -translate-x-1/2 pointer-events-none"
      viewBox="0 0 400 870"
      fill="none"
      preserveAspectRatio="xMidYMin meet"
    >
      {/* Background path */}
      <path
        d={PATH_D}
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        className="text-border/30"
        fill="none"
      />

      {/* Animated path */}
      <motion.path
        d={PATH_D}
        stroke="url(#pathGradient)"
        strokeWidth="2"
        strokeLinecap="round"
        fill="none"
        style={{
          pathLength: 1,
          strokeDasharray: pathLength,
          strokeDashoffset,
        }}
      />

      {/* Node dots on path */}
      {[60, 210, 360, 510, 660, 810].map((y, i) => (
        <circle
          key={i}
          cx="200"
          cy={y}
          r="5"
          className="fill-background stroke-primary/40"
          strokeWidth="2"
        />
      ))}

      {/* Event Bursts */}
      {bursts.map((burst) => (
        <g key={burst.id}>
          {[0, 0.1, 0.2, 0.3].map((delay, i) => (
            <BurstParticle
              key={`${burst.id}-${i}`}
              delay={delay}
              pathD={burst.segment}
              size={4 - i * 0.6}
            />
          ))}
        </g>
      ))}

      <defs>
        <linearGradient id="pathGradient" x1="0%" y1="0%" x2="0%" y2="100%">
          <stop offset="0%" stopColor="hsl(var(--primary))" stopOpacity="0.5" />
          <stop offset="50%" stopColor="hsl(var(--primary))" stopOpacity="1" />
          <stop offset="100%" stopColor="hsl(var(--primary))" stopOpacity="0.3" />
        </linearGradient>
      </defs>
    </svg>
  )
}
