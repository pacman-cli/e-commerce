
"use client"

import { motion, useScroll, useTransform } from "framer-motion"
import { Check, Code2, Cpu, GitBranch, Layers, Terminal, Zap } from "lucide-react"
import { useRef } from "react"

// Premium easing
const easeOutSoft: [number, number, number, number] = [0.16, 1, 0.3, 1]

const features = [
  {
    icon: Terminal,
    title: "One Command Setup",
    description: "Clone, run docker-compose up, and you're live in under 2 minutes.",
  },
  {
    icon: Code2,
    title: "TypeScript First",
    description: "Full type safety from frontend to API. Catch errors before runtime.",
  },
  {
    icon: GitBranch,
    title: "Git-Based Workflow",
    description: "Branch, PR, merge. Infrastructure follows your code.",
  },
  {
    icon: Layers,
    title: "Modular Architecture",
    description: "Add or remove services without touching the rest of the system.",
  },
  {
    icon: Zap,
    title: "Hot Reload Everything",
    description: "Frontend, backend, even Kafka consumers. No restart needed.",
  },
  {
    icon: Cpu,
    title: "Production Ready",
    description: "Health checks, graceful shutdown, structured logging. Built in.",
  },
]

interface FeatureRowProps {
  icon: typeof Terminal
  title: string
  description: string
  index: number
  scrollYProgress: any
}

function FeatureRow({ icon: Icon, title, description, index, scrollYProgress }: FeatureRowProps) {
  const start = index * 0.1
  const end = start + 0.15

  const opacity = useTransform(scrollYProgress, [start, start + 0.05, end, end + 0.1], [0.2, 1, 1, 0.4])
  const x = useTransform(scrollYProgress, [start, start + 0.08], [-30, 0])
  const scale = useTransform(scrollYProgress, [start, start + 0.05], [0.95, 1])

  return (
    <motion.div
      style={{ opacity, x, scale }}
      className="flex items-start gap-5 py-6 border-b border-border/30 last:border-0"
    >
      <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-primary">
        <Icon className="h-5 w-5" />
      </div>
      <div>
        <h4 className="font-semibold text-foreground mb-1 flex items-center gap-2">
          {title}
          <Check className="h-4 w-4 text-green-500" />
        </h4>
        <p className="text-sm text-muted-foreground leading-relaxed">{description}</p>
      </div>
    </motion.div>
  )
}

export function BuiltForDevelopers() {
  const containerRef = useRef<HTMLDivElement>(null)

  const { scrollYProgress } = useScroll({
    target: containerRef,
    offset: ["start end", "end start"]
  })

  // Header animations
  const headerOpacity = useTransform(scrollYProgress, [0, 0.15], [0, 1])
  const headerY = useTransform(scrollYProgress, [0, 0.15], [40, 0])

  // Badge animations
  const badgeScale = useTransform(scrollYProgress, [0.05, 0.12], [0.8, 1])
  const badgeOpacity = useTransform(scrollYProgress, [0.05, 0.12], [0, 1])

  // Progress bar that fills as you scroll
  const progressWidth = useTransform(scrollYProgress, [0.1, 0.8], ["0%", "100%"])

  return (
    <section
      ref={containerRef}
      className="relative py-32 md:py-48 overflow-hidden"
    >
      {/* Background */}
      <div className="absolute inset-0 bg-gradient-to-b from-muted/30 via-background to-background pointer-events-none" />

      <div className="container mx-auto px-4 max-w-4xl relative">
        {/* Header */}
        <motion.div
          style={{ opacity: headerOpacity, y: headerY }}
          className="text-center mb-16"
        >
          {/* Badge */}
          <motion.div
            style={{ scale: badgeScale, opacity: badgeOpacity }}
            className="inline-flex items-center gap-2 rounded-full border border-primary/20 bg-primary/5 px-4 py-1.5 text-sm mb-6"
          >
            <Terminal className="h-3.5 w-3.5 text-primary" />
            <span className="text-primary font-medium">Developer Experience</span>
          </motion.div>

          <h2 className="text-4xl font-bold tracking-tight md:text-5xl mb-6">
            Built for Developers
          </h2>
          <p className="text-lg text-muted-foreground max-w-xl mx-auto leading-relaxed">
            Everything you need to go from zero to production. No boilerplate, no configuration hell.
          </p>

          {/* Progress indicator */}
          <div className="mt-10 mx-auto max-w-sm">
            <div className="h-1 w-full bg-border/30 rounded-full overflow-hidden">
              <motion.div
                style={{ width: progressWidth }}
                className="h-full bg-gradient-to-r from-primary to-primary/60 rounded-full"
              />
            </div>
            <p className="text-xs text-muted-foreground mt-2">Scroll to explore</p>
          </div>
        </motion.div>

        {/* Feature List */}
        <div className="relative">
          {/* Vertical line on left */}
          <div className="absolute left-6 top-0 bottom-0 w-px bg-border/20" />

          {/* Features */}
          <div className="space-y-2">
            {features.map((feature, index) => (
              <FeatureRow
                key={feature.title}
                icon={feature.icon}
                title={feature.title}
                description={feature.description}
                index={index}
                scrollYProgress={scrollYProgress}
              />
            ))}
          </div>
        </div>

        {/* Bottom CTA */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6, ease: easeOutSoft }}
          className="text-center mt-16"
        >
          <p className="text-muted-foreground text-sm">
            Ready to start? Run{" "}
            <code className="px-2 py-1 rounded bg-muted font-mono text-xs text-primary">
              docker-compose up
            </code>{" "}
            and you're live.
          </p>
        </motion.div>
      </div>
    </section>
  )
}
