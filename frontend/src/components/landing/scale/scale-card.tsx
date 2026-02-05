
"use client"

import { motion, MotionValue, useTransform } from "framer-motion"

interface ScaleCardProps {
  title: string
  description: string
  scrollYProgress: MotionValue<number>
  activateAt: number
  position: "left" | "right"
  yPosition: number // Pixel position from top
}

export function ScaleCard({
  title,
  description,
  scrollYProgress,
  activateAt,
  position,
  yPosition
}: ScaleCardProps) {
  const opacity = useTransform(
    scrollYProgress,
    [activateAt - 0.03, activateAt + 0.02, activateAt + 0.2],
    [0.15, 1, 0.5]
  )

  const x = useTransform(
    scrollYProgress,
    [activateAt - 0.05, activateAt + 0.02],
    [position === "left" ? -12 : 12, 0]
  )

  const borderOpacity = useTransform(
    scrollYProgress,
    [activateAt - 0.02, activateAt + 0.02, activateAt + 0.15],
    [0, 0.6, 0.15]
  )

  return (
    <motion.div
      style={{
        opacity,
        x,
        top: yPosition - 10, // Slight offset to align with node center
        // Position card further from center than node
        left: position === "right" ? "calc(50% + 140px)" : undefined,
        right: position === "left" ? "calc(50% + 140px)" : undefined,
      }}
      className="absolute w-48"
    >
      <motion.div
        className="rounded-lg border border-border/50 bg-card/90 backdrop-blur-sm p-4 shadow-sm"
        style={{
          borderColor: useTransform(borderOpacity, (v) => `hsl(var(--primary) / ${v * 0.5})`)
        }}
      >
        <h4 className="font-semibold text-sm text-foreground mb-1">{title}</h4>
        <p className="text-xs text-muted-foreground leading-relaxed">{description}</p>
      </motion.div>
    </motion.div>
  )
}
