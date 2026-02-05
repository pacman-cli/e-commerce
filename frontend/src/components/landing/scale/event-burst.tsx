
"use client"

import { motion, MotionValue, useMotionValueEvent } from "framer-motion"
import { useState } from "react"

interface EventBurstProps {
  scrollYProgress: MotionValue<number>
  activateAt: number
  pathSegment: string // SVG path segment for this burst
}

// Individual burst particle
function BurstParticle({ delay, pathD }: { delay: number; pathD: string }) {
  return (
    <motion.circle
      r="4"
      fill="hsl(var(--primary))"
      initial={{ opacity: 0 }}
      animate={{
        opacity: [0, 0.8, 0.8, 0],
      }}
      transition={{
        duration: 1.5,
        delay,
        ease: "easeOut",
      }}
    >
      <animateMotion
        dur="1.5s"
        begin={`${delay}s`}
        fill="freeze"
        path={pathD}
      />
    </motion.circle>
  )
}

export function EventBurst({ scrollYProgress, activateAt, pathSegment }: EventBurstProps) {
  const [triggered, setTriggered] = useState(false)
  const [showBurst, setShowBurst] = useState(false)

  // Monitor scroll progress and trigger burst when threshold is crossed
  useMotionValueEvent(scrollYProgress, "change", (latest) => {
    if (!triggered && latest >= activateAt && latest < activateAt + 0.05) {
      setTriggered(true)
      setShowBurst(true)

      // Reset after animation completes
      setTimeout(() => {
        setShowBurst(false)
        setTriggered(false)
      }, 2000)
    }
  })

  if (!showBurst) return null

  return (
    <g className="event-burst">
      {/* Multiple particles with staggered timing */}
      <BurstParticle delay={0} pathD={pathSegment} />
      <BurstParticle delay={0.1} pathD={pathSegment} />
      <BurstParticle delay={0.2} pathD={pathSegment} />
      <BurstParticle delay={0.3} pathD={pathSegment} />
      <BurstParticle delay={0.4} pathD={pathSegment} />
    </g>
  )
}
