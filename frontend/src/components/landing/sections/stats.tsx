
"use client"

import { Section } from "@/components/landing/ui/section"
import { motion, useInView } from "framer-motion"
import { useEffect, useRef, useState } from "react"

// Premium easing
const easeOutSoft: [number, number, number, number] = [0.16, 1, 0.3, 1]

interface StatItemProps {
  value: number
  suffix: string
  label: string
  index: number
}

function AnimatedCounter({ value, suffix }: { value: number; suffix: string }) {
  const ref = useRef<HTMLSpanElement>(null)
  const isInView = useInView(ref, { once: true, margin: "-100px" })
  const [displayValue, setDisplayValue] = useState(0)

  useEffect(() => {
    if (isInView) {
      let start = 0
      const end = value
      const duration = 2500 // Slower for calmer feel
      const startTime = performance.now()

      const animate = (currentTime: number) => {
        const elapsed = currentTime - startTime
        const progress = Math.min(elapsed / duration, 1)
        // Very gentle easing
        const eased = 1 - Math.pow(1 - progress, 4)
        setDisplayValue(Math.floor(eased * end))
        if (progress < 1) {
          requestAnimationFrame(animate)
        }
      }

      requestAnimationFrame(animate)
    }
  }, [isInView, value])

  return (
    <span ref={ref} className="tabular-nums">
      {displayValue.toLocaleString()}{suffix}
    </span>
  )
}

function StatItem({ value, suffix, label, index }: StatItemProps) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      whileInView={{ opacity: 1, y: 0 }}
      transition={{ delay: index * 0.1, duration: 0.7, ease: easeOutSoft }}
      viewport={{ once: true }}
      className="flex flex-col items-center text-center p-8"
    >
      <span className="text-5xl md:text-6xl font-bold bg-gradient-to-br from-foreground to-muted-foreground/80 bg-clip-text text-transparent">
        <AnimatedCounter value={value} suffix={suffix} />
      </span>
      <span className="mt-3 text-muted-foreground text-sm">{label}</span>
    </motion.div>
  )
}

export function Stats() {
  const stats = [
    { value: 99, suffix: ".9%", label: "Uptime Guarantee" },
    { value: 50, suffix: "ms", label: "Average Latency" },
    { value: 10, suffix: "k+", label: "Concurrent Users" },
    { value: 5, suffix: "", label: "Microservices" },
  ]

  return (
    <Section className="border-y border-border/40 py-16 md:py-20">
      <div className="container mx-auto px-4 max-w-screen-xl">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {stats.map((stat, i) => (
            <StatItem key={stat.label} value={stat.value} suffix={stat.suffix} label={stat.label} index={i} />
          ))}
        </div>
      </div>
    </Section>
  )
}
