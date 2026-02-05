
"use client"

import { motion, useScroll, useTransform } from "framer-motion"
import { Database, Globe, Layers, LucideIcon, Server, ShoppingBag, Users } from "lucide-react"
import { useRef } from "react"

// Premium easing
const easeOutSoft = [0.16, 1, 0.3, 1]

interface ScaleStepProps {
  icon: LucideIcon
  title: string
  label: string
  description: string
  index: number
  total: number
  scrollYProgress: any
}

function ScaleStep({ icon: Icon, title, label, description, index, total, scrollYProgress }: ScaleStepProps) {
  const isLeft = index % 2 === 0
  const activateAt = (index + 1) / (total + 1)

  const opacity = useTransform(
    scrollYProgress,
    [activateAt - 0.08, activateAt, activateAt + 0.05],
    [0.2, 1, 1]
  )

  const x = useTransform(
    scrollYProgress,
    [activateAt - 0.08, activateAt],
    [isLeft ? -20 : 20, 0]
  )

  const nodeScale = useTransform(
    scrollYProgress,
    [activateAt - 0.05, activateAt],
    [0.8, 1]
  )

  const glowOpacity = useTransform(
    scrollYProgress,
    [activateAt - 0.02, activateAt, activateAt + 0.1],
    [0, 0.4, 0.1]
  )

  return (
    <div className="relative grid grid-cols-[1fr_auto_1fr] gap-8 items-center min-h-[140px]">
      {/* Left Content */}
      <motion.div
        style={{ opacity, x: isLeft ? x : 0 }}
        className={`${isLeft ? '' : 'invisible'}`}
      >
        {isLeft && (
          <div className="text-right pr-4">
            <h4 className="font-semibold text-foreground mb-1">{title}</h4>
            <p className="text-sm text-muted-foreground leading-relaxed">{description}</p>
          </div>
        )}
      </motion.div>

      {/* Center Node */}
      <div className="relative flex flex-col items-center z-10">
        {/* Glow */}
        <motion.div
          style={{ opacity: glowOpacity }}
          className="absolute inset-0 -m-4 rounded-full bg-primary blur-xl"
        />

        {/* Node Circle */}
        <motion.div
          style={{ scale: nodeScale }}
          className="relative flex h-14 w-14 items-center justify-center rounded-full border-2 border-primary/30 bg-card shadow-lg"
        >
          <Icon className="h-6 w-6 text-primary" />
        </motion.div>

        {/* Label below node */}
        <motion.span
          style={{ opacity }}
          className="mt-2 text-xs font-medium text-muted-foreground whitespace-nowrap"
        >
          {label}
        </motion.span>
      </div>

      {/* Right Content */}
      <motion.div
        style={{ opacity, x: !isLeft ? x : 0 }}
        className={`${!isLeft ? '' : 'invisible'}`}
      >
        {!isLeft && (
          <div className="text-left pl-4">
            <h4 className="font-semibold text-foreground mb-1">{title}</h4>
            <p className="text-sm text-muted-foreground leading-relaxed">{description}</p>
          </div>
        )}
      </motion.div>
    </div>
  )
}

const steps = [
  {
    icon: Globe,
    label: "Client",
    title: "Global Traffic",
    description: "Users connect from anywhere in the world with edge-optimized delivery."
  },
  {
    icon: Server,
    label: "API Gateway",
    title: "Intelligent Routing",
    description: "Load balancing, rate limiting, and authentication at the edge."
  },
  {
    icon: Users,
    label: "User Service",
    title: "Independent Scaling",
    description: "Each service scales based on its own demand patterns."
  },
  {
    icon: ShoppingBag,
    label: "Order Service",
    title: "Resilient Processing",
    description: "Graceful degradation ensures orders are never lost."
  },
  {
    icon: Layers,
    label: "Event Bus",
    title: "Async Communication",
    description: "Kafka ensures eventual consistency across all services."
  },
  {
    icon: Database,
    label: "Data Layer",
    title: "Distributed Storage",
    description: "Each service owns its data. No single point of failure."
  },
]

export function BuiltForScale() {
  const containerRef = useRef<HTMLDivElement>(null)

  const { scrollYProgress } = useScroll({
    target: containerRef,
    offset: ["start end", "end start"]
  })

  // Animate the vertical line
  const lineHeight = useTransform(scrollYProgress, [0.1, 0.9], ["0%", "100%"])

  return (
    <section
      ref={containerRef}
      className="relative py-32 md:py-40 overflow-hidden"
    >
      {/* Background */}
      <div className="absolute inset-0 bg-gradient-to-b from-background via-muted/10 to-background pointer-events-none" />

      <div className="container mx-auto px-4 max-w-4xl">
        {/* Header */}
        <motion.div
          initial={{ opacity: 0, y: 24 }}
          whileInView={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8, ease: easeOutSoft }}
          viewport={{ once: true }}
          className="text-center mb-20"
        >
          <h2 className="text-4xl font-bold tracking-tight md:text-5xl mb-6">
            Built for Scale
          </h2>
          <p className="text-lg text-muted-foreground max-w-2xl mx-auto leading-relaxed">
            Watch how traffic flows through our distributed system. Each component scales independently.
          </p>
        </motion.div>

        {/* Steps Container */}
        <div className="relative">
          {/* Vertical Line Background */}
          <div className="absolute left-1/2 top-0 bottom-0 w-0.5 -translate-x-1/2 bg-border/30" />

          {/* Animated Line Progress */}
          <motion.div
            style={{ height: lineHeight }}
            className="absolute left-1/2 top-0 w-0.5 -translate-x-1/2 bg-gradient-to-b from-primary via-primary to-primary/50 origin-top"
          />

          {/* Steps */}
          <div className="relative space-y-8">
            {steps.map((step, index) => (
              <ScaleStep
                key={step.label}
                icon={step.icon}
                title={step.title}
                label={step.label}
                description={step.description}
                index={index}
                total={steps.length}
                scrollYProgress={scrollYProgress}
              />
            ))}
          </div>
        </div>
      </div>
    </section>
  )
}
