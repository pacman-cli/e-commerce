
"use client"

import { Section } from "@/components/landing/ui/section"
import { motion } from "framer-motion"
import { Package, Rocket, Settings, Shield } from "lucide-react"

const timelineSteps = [
  {
    icon: Package,
    title: "Clone the Repository",
    description: "Get the complete source code from GitHub.",
  },
  {
    icon: Settings,
    title: "Configure Environment",
    description: "Set up your .env file with database credentials.",
  },
  {
    icon: Rocket,
    title: "Launch with Docker",
    description: "Run docker-compose up and watch the magic happen.",
  },
  {
    icon: Shield,
    title: "Secure & Scale",
    description: "Deploy to production with confidence using Kubernetes.",
  },
]

export function Timeline() {
  return (
    <Section className="bg-muted/20 py-24 md:py-40">
      <div className="container mx-auto px-4 max-w-screen-xl">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="text-center mb-20 max-w-3xl mx-auto"
        >
          <h2 className="text-4xl font-bold tracking-tight md:text-5xl mb-6">
            Get Started in Minutes
          </h2>
          <p className="text-lg text-muted-foreground">
            From clone to production, we've streamlined the entire process.
          </p>
        </motion.div>

        <div className="relative max-w-2xl mx-auto">
          {/* Vertical Line */}
          <motion.div
            initial={{ scaleY: 0 }}
            whileInView={{ scaleY: 1 }}
            transition={{ duration: 1, ease: "easeOut" }}
            viewport={{ once: true }}
            className="absolute left-6 md:left-1/2 top-0 bottom-0 w-0.5 bg-gradient-to-b from-primary via-border to-transparent origin-top"
          />

          <div className="space-y-12">
            {timelineSteps.map((step, i) => (
              <motion.div
                key={step.title}
                initial={{ opacity: 0, x: i % 2 === 0 ? -20 : 20 }}
                whileInView={{ opacity: 1, x: 0 }}
                transition={{ delay: i * 0.2, duration: 0.5 }}
                viewport={{ once: true }}
                className={`relative flex items-start gap-6 ${i % 2 === 0 ? "md:flex-row-reverse md:text-right" : ""}`}
              >
                {/* Icon Marker */}
                <motion.div
                  initial={{ scale: 0 }}
                  whileInView={{ scale: 1 }}
                  transition={{ delay: i * 0.2 + 0.2, type: "spring" }}
                  viewport={{ once: true }}
                  className="relative z-10 flex h-12 w-12 flex-shrink-0 items-center justify-center rounded-full border border-primary/30 bg-background shadow-lg shadow-primary/10"
                >
                  <step.icon className="h-5 w-5 text-primary" />
                </motion.div>

                {/* Content */}
                <div className={`flex-1 pt-1 ${i % 2 === 0 ? "md:pr-8" : "md:pl-8"}`}>
                  <div className="rounded-lg border border-border bg-card p-4 shadow-sm">
                    <h3 className="font-semibold text-lg">{step.title}</h3>
                    <p className="text-muted-foreground text-sm mt-1">{step.description}</p>
                  </div>
                </div>
              </motion.div>
            ))}
          </div>
        </div>
      </div>
    </Section>
  )
}
