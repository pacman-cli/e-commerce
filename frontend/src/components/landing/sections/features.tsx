
"use client"

import { GlowCard } from "@/components/landing/ui/glow-card"
import { HoverTilt } from "@/components/landing/ui/hover-tilt"
import { Section } from "@/components/landing/ui/section"
import { motion } from "framer-motion"
import { Activity, Cpu, Globe, Layers, LucideIcon, ShieldCheck, Zap } from "lucide-react"

// Premium easing
const easeOutSoft = [0.16, 1, 0.3, 1]

interface FeatureItemProps {
  title: string
  description: string
  icon: LucideIcon
  index: number
}

function FeatureItem({ title, description, icon: Icon, index }: FeatureItemProps) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 24 }}
      whileInView={{ opacity: 1, y: 0 }}
      transition={{ delay: index * 0.08, duration: 0.7, ease: easeOutSoft }}
      viewport={{ once: true }}
    >
      <HoverTilt maxRotation={4}>
        <GlowCard className="h-full">
          <div className="flex flex-col gap-4">
            <motion.div
              whileHover={{ scale: 1.05 }}
              transition={{ duration: 0.3, ease: easeOutSoft }}
              className="inline-flex h-12 w-12 items-center justify-center rounded-lg bg-primary/8 text-primary"
            >
              <Icon className="h-5 w-5" />
            </motion.div>
            <h3 className="text-lg font-semibold">{title}</h3>
            <p className="text-muted-foreground text-sm leading-relaxed">{description}</p>
          </div>
        </GlowCard>
      </HoverTilt>
    </motion.div>
  )
}

export function Features() {
  const features = [
    {
      title: "Microservices Architecture",
      description: "Decoupled services for maximum scalability and independent deployment cycles.",
      icon: Layers,
    },
    {
      title: "Event-Driven",
      description: "Asynchronous communication via Apache Kafka ensures eventual consistency.",
      icon: Activity,
    },
    {
      title: "Enterprise Security",
      description: "JWT-based stateless authentication with role-based access control.",
      icon: ShieldCheck,
    },
    {
      title: "High Performance",
      description: "Optimized Spring Boot 3 applications running on Java 21.",
      icon: Zap,
    },
    {
      title: "Edge-Ready Frontend",
      description: "Next.js 15 App Router with server components for optimal performance.",
      icon: Globe,
    },
    {
      title: "Modern Stack",
      description: "TypeScript, Tailwind CSS, and fully containerized with Docker.",
      icon: Cpu,
    },
  ]

  return (
    <Section className="py-28 md:py-44">
      <div className="container mx-auto px-4 max-w-screen-xl">
        <motion.div
          initial={{ opacity: 0, y: 24 }}
          whileInView={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8, ease: easeOutSoft }}
          viewport={{ once: true }}
          className="text-center mb-20 max-w-3xl mx-auto"
        >
          <h2 className="text-3xl font-bold tracking-tight md:text-5xl mb-6">
            Everything you need.
            <br />
            <span className="text-muted-foreground">Nothing you don't.</span>
          </h2>
          <p className="text-lg text-muted-foreground max-w-xl mx-auto leading-relaxed">
            A carefully curated stack designed to solve real-world challenges.
          </p>
        </motion.div>

        <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {features.map((feature, index) => (
            <FeatureItem
              key={feature.title}
              title={feature.title}
              description={feature.description}
              icon={feature.icon}
              index={index}
            />
          ))}
        </div>
      </div>
    </Section>
  )
}
