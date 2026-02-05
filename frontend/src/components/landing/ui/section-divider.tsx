
"use client"

import { motion } from "framer-motion"

interface SectionDividerProps {
  className?: string
}

export function SectionDivider({ className }: SectionDividerProps) {
  return (
    <motion.div
      initial={{ scaleX: 0, opacity: 0 }}
      whileInView={{ scaleX: 1, opacity: 1 }}
      transition={{ duration: 0.8, ease: "easeOut" }}
      viewport={{ once: true }}
      className={`mx-auto h-px w-full max-w-screen-lg bg-gradient-to-r from-transparent via-border to-transparent ${className}`}
    />
  )
}
