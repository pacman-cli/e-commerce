
"use client"

import { cn } from "@/lib/utils"
import { motion, MotionValue, useTransform } from "framer-motion"
import { LucideIcon } from "lucide-react"

interface ScaleNodeProps {
  icon: LucideIcon
  label: string
  scrollYProgress: MotionValue<number>
  activateAt: number
  position: "left" | "right"
  yPosition: number // Pixel position from top
}

export function ScaleNode({
  icon: Icon,
  label,
  scrollYProgress,
  activateAt,
  position,
  yPosition
}: ScaleNodeProps) {
  const opacity = useTransform(
    scrollYProgress,
    [activateAt - 0.05, activateAt, activateAt + 0.05],
    [0, 1, 1]
  )

  const scale = useTransform(
    scrollYProgress,
    [activateAt - 0.05, activateAt, activateAt + 0.02],
    [0.8, 1, 1]
  )

  const glowOpacity = useTransform(
    scrollYProgress,
    [activateAt - 0.02, activateAt, activateAt + 0.15, activateAt + 0.2],
    [0, 0.4, 0.15, 0.05]
  )

  return (
    <motion.div
      style={{
        opacity,
        scale,
        top: yPosition,
        // Position node relative to center
        left: position === "right" ? "calc(50% + 24px)" : undefined,
        right: position === "left" ? "calc(50% + 24px)" : undefined,
      }}
      className={cn(
        "absolute flex items-center gap-3",
        position === "left" ? "flex-row-reverse" : "flex-row"
      )}
    >
      {/* Node Circle */}
      <div className="relative">
        {/* Glow */}
        <motion.div
          style={{ opacity: glowOpacity }}
          className="absolute inset-0 -m-3 rounded-full bg-primary blur-lg"
        />

        {/* Node */}
        <div className="relative flex h-11 w-11 items-center justify-center rounded-full border border-primary/20 bg-card shadow-md">
          <Icon className="h-5 w-5 text-primary" />
        </div>
      </div>

      {/* Label */}
      <span className={cn(
        "text-sm font-medium text-foreground whitespace-nowrap",
        position === "left" ? "text-right" : "text-left"
      )}>
        {label}
      </span>
    </motion.div>
  )
}
