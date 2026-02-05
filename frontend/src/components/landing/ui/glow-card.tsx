
"use client"

import { cn } from "@/lib/utils"
import { motion, useMotionValue, useTransform } from "framer-motion"
import { MouseEvent, ReactNode, useRef } from "react"

interface GlowCardProps {
  children: ReactNode
  className?: string
  containerClassName?: string
}

export function GlowCard({ children, className, containerClassName }: GlowCardProps) {
  const ref = useRef<HTMLDivElement>(null)
  const mouseX = useMotionValue(0)
  const mouseY = useMotionValue(0)

  function handleMouseMove(e: MouseEvent<HTMLDivElement>) {
    if (!ref.current) return
    const rect = ref.current.getBoundingClientRect()
    mouseX.set(e.clientX - rect.left)
    mouseY.set(e.clientY - rect.top)
  }

  return (
    <div
      ref={ref}
      onMouseMove={handleMouseMove}
      className={cn("group relative", containerClassName)}
    >
      <motion.div
        className="pointer-events-none absolute -inset-px rounded-xl opacity-0 transition-opacity duration-500 group-hover:opacity-100"
        style={{
          background: useTransform(
            [mouseX, mouseY],
            ([x, y]) =>
              `radial-gradient(500px circle at ${x}px ${y}px, rgba(var(--primary-rgb), 0.08), transparent 40%)`
          ),
        }}
      />
      <div
        className={cn(
          "relative rounded-xl border border-border/80 bg-card p-6 transition-all duration-500 group-hover:border-primary/20 group-hover:shadow-sm",
          className
        )}
      >
        {children}
      </div>
    </div>
  )
}
