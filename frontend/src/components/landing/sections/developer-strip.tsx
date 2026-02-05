
"use client"

import { motion } from "framer-motion"
import { Code2, Coffee, Terminal, Zap } from "lucide-react"

export function DeveloperStrip() {
  const badges = [
    { icon: Terminal, text: "One-Command Setup" },
    { icon: Code2, text: "TypeScript First" },
    { icon: Zap, text: "Hot Reload" },
    { icon: Coffee, text: "Developer Friendly" },
  ]

  return (
    <div className="relative overflow-hidden border-y border-border/50 bg-muted/30 py-6">
      {/* Subtle gradient overlay */}
      <div className="absolute inset-0 bg-gradient-to-r from-background via-transparent to-background pointer-events-none z-10" />

      <div className="container mx-auto px-4 max-w-screen-xl">
        <motion.div
          initial={{ opacity: 0 }}
          whileInView={{ opacity: 1 }}
          viewport={{ once: true }}
          className="flex flex-wrap items-center justify-center gap-8 md:gap-16"
        >
          <span className="text-sm font-semibold text-muted-foreground uppercase tracking-wider">
            Built for Developers
          </span>

          {badges.map((badge, i) => (
            <motion.div
              key={badge.text}
              initial={{ opacity: 0, y: 10 }}
              whileInView={{ opacity: 1, y: 0 }}
              transition={{ delay: i * 0.1 }}
              viewport={{ once: true }}
              className="flex items-center gap-2 text-muted-foreground hover:text-foreground transition-colors"
            >
              <badge.icon className="h-4 w-4 text-primary" />
              <span className="text-sm font-medium">{badge.text}</span>
            </motion.div>
          ))}
        </motion.div>
      </div>
    </div>
  )
}
