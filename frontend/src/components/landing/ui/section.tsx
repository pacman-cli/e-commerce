
"use client"

import { cn } from "@/lib/utils"
import { HTMLMotionProps, motion } from "framer-motion"
import { ReactNode } from "react"

interface SectionProps extends HTMLMotionProps<"section"> {
  children: ReactNode
  className?: string
  delay?: number
}

// Premium easing - gentle, confident
const easeOut: [number, number, number, number] = [0.16, 1, 0.3, 1]

export function Section({ children, className, delay = 0, ...props }: SectionProps) {
  return (
    <motion.section
      initial={{ opacity: 0, y: 30 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true, margin: "-50px" }}
      transition={{
        duration: 0.8,
        delay,
        ease: easeOut
      }}
      className={cn("relative py-24 md:py-40 overflow-hidden", className)}
      {...props}
    >
      {children}
    </motion.section>
  )
}
