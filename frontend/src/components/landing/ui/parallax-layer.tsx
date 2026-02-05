
"use client"

import { cn } from "@/lib/utils"
import { motion, useScroll, useTransform } from "framer-motion"
import { ReactNode, useRef } from "react"

interface ParallaxLayerProps {
  children: ReactNode
  className?: string
  speed?: number // 0 = no parallax, 1 = full parallax (moves with scroll), -1 = inverse
  offset?: number // Starting offset in pixels
}

export function ParallaxLayer({ children, className, speed = 0.5, offset = 0 }: ParallaxLayerProps) {
  const ref = useRef<HTMLDivElement>(null)
  const { scrollYProgress } = useScroll({
    target: ref,
    offset: ["start end", "end start"]
  })

  const y = useTransform(scrollYProgress, [0, 1], [offset, offset + speed * -200])

  return (
    <motion.div
      ref={ref}
      style={{ y }}
      className={cn("will-change-transform", className)}
    >
      {children}
    </motion.div>
  )
}
