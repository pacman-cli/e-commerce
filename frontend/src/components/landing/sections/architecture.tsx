
"use client"

import { GlowCard } from "@/components/landing/ui/glow-card"
import { Section } from "@/components/landing/ui/section"
import { cn } from "@/lib/utils"
import { AnimatePresence, motion } from "framer-motion"
import { Database, Globe, Layers, LucideIcon, Mail, Server, ShoppingBag, Users } from "lucide-react"
import { useState } from "react"

interface ServiceNodeProps {
  icon: LucideIcon
  title: string
  description: string
  delay: number
  isActive: boolean
  onHover: () => void
  onLeave: () => void
}

function ServiceNode({ icon: Icon, title, description, delay, isActive, onHover, onLeave }: ServiceNodeProps) {
  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.8 }}
      whileInView={{ opacity: 1, scale: 1 }}
      transition={{ delay, duration: 0.5 }}
      viewport={{ once: true }}
      onMouseEnter={onHover}
      onMouseLeave={onLeave}
      className={cn(
        "relative flex flex-col items-center gap-2 transition-opacity duration-300",
        isActive ? "opacity-100 z-10" : "opacity-40"
      )}
    >
      <GlowCard containerClassName="w-full" className="flex flex-col items-center p-4 text-center">
        <motion.div
          whileHover={{ scale: 1.1 }}
          className="flex h-14 w-14 items-center justify-center rounded-xl bg-primary/10 text-primary mb-2"
        >
          <Icon className="h-7 w-7" />
        </motion.div>
        <span className="text-sm font-semibold">{title}</span>
        <AnimatePresence>
          {isActive && (
            <motion.span
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: "auto" }}
              exit={{ opacity: 0, height: 0 }}
              className="text-xs text-muted-foreground mt-1"
            >
              {description}
            </motion.span>
          )}
        </AnimatePresence>
      </GlowCard>
    </motion.div>
  )
}

export function Architecture() {
  const [activeService, setActiveService] = useState<string | null>(null)

  const services = [
    { id: "gateway", icon: Server, title: "API Gateway", description: "Routes all incoming traffic" },
    { id: "users", icon: Users, title: "User Service", description: "Handles auth & profiles" },
    { id: "orders", icon: ShoppingBag, title: "Order Service", description: "Manages order lifecycle" },
    { id: "payments", icon: Database, title: "Payment Service", description: "Processes transactions" },
    { id: "notifications", icon: Mail, title: "Notifications", description: "Sends email & SMS" },
  ]

  const isActive = (id: string) => activeService === null || activeService === id

  return (
    <Section className="bg-muted/20 py-24 md:py-40">
      <div className="container mx-auto px-4 max-w-screen-xl">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="text-center mb-16"
        >
          <h2 className="text-4xl font-bold tracking-tight md:text-5xl mb-4">Built for Scale</h2>
          <p className="text-lg text-muted-foreground max-w-xl mx-auto">
            A visual overview of our event-driven microservices architecture. Hover to explore.
          </p>
        </motion.div>

        <div className="relative mx-auto max-w-4xl">
          {/* Connection Lines (Animated) */}
          <motion.div
            initial={{ scaleX: 0 }}
            whileInView={{ scaleX: 1 }}
            transition={{ duration: 0.8, delay: 0.5 }}
            viewport={{ once: true }}
            className="absolute top-[calc(50%-1px)] left-0 right-0 h-0.5 bg-gradient-to-r from-transparent via-primary/30 to-transparent origin-center"
          />

          {/* Client -> Gateway Flow */}
          <div className="grid gap-8">
            {/* Level 1: Client */}
            <div className="flex justify-center">
              <motion.div
                initial={{ opacity: 0, scale: 0.8 }}
                whileInView={{ opacity: 1, scale: 1 }}
                viewport={{ once: true }}
                className={cn(
                  "flex flex-col items-center transition-opacity duration-300",
                  activeService === null ? "opacity-100" : "opacity-40"
                )}
              >
                <div className="flex h-16 w-16 items-center justify-center rounded-2xl border border-border bg-card shadow-sm">
                  <Globe className="h-8 w-8 text-muted-foreground" />
                </div>
                <span className="mt-2 text-sm font-medium text-muted-foreground">Web Client</span>
              </motion.div>
            </div>

            {/* Animated Vertical Line */}
            <motion.div
              initial={{ scaleY: 0 }}
              whileInView={{ scaleY: 1 }}
              transition={{ duration: 0.5, delay: 0.2 }}
              viewport={{ once: true }}
              className="mx-auto h-12 w-0.5 bg-gradient-to-b from-primary/50 to-border origin-top"
            />

            {/* Level 2: Gateway */}
            <div className="flex justify-center">
              <ServiceNode
                icon={Server}
                title="API Gateway"
                description="Routes, authenticates, and rate-limits."
                delay={0.3}
                isActive={isActive("gateway")}
                onHover={() => setActiveService("gateway")}
                onLeave={() => setActiveService(null)}
              />
            </div>

            {/* Animated Fan-Out Lines */}
            <motion.div
              initial={{ opacity: 0 }}
              whileInView={{ opacity: 1 }}
              transition={{ duration: 0.5, delay: 0.5 }}
              viewport={{ once: true }}
              className="relative mx-auto h-12 w-3/4"
            >
              <div className="absolute top-0 left-1/2 h-6 w-0.5 bg-border" />
              <div className="absolute top-6 left-0 right-0 h-0.5 bg-border" />
              <div className="absolute top-6 left-0 h-6 w-0.5 bg-border" />
              <div className="absolute top-6 left-1/4 h-6 w-0.5 bg-border" />
              <div className="absolute top-6 left-1/2 h-6 w-0.5 bg-border" />
              <div className="absolute top-6 left-3/4 h-6 w-0.5 bg-border" />
              <div className="absolute top-6 right-0 h-6 w-0.5 bg-border" />
            </motion.div>

            {/* Level 3: Services */}
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-5 gap-4 justify-items-center">
              {services.slice(1).map((service, i) => (
                <ServiceNode
                  key={service.id}
                  icon={service.icon}
                  title={service.title}
                  description={service.description}
                  delay={0.5 + i * 0.1}
                  isActive={isActive(service.id)}
                  onHover={() => setActiveService(service.id)}
                  onLeave={() => setActiveService(null)}
                />
              ))}
            </div>

            {/* Level 4: Kafka & DBs */}
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.8 }}
              viewport={{ once: true }}
              className="mt-8 grid grid-cols-1 md:grid-cols-2 gap-6 items-center justify-items-center"
            >
              <div className="flex items-center gap-3 rounded-xl border border-dashed border-border bg-card/50 p-4 backdrop-blur-sm">
                <Layers className="h-6 w-6 text-muted-foreground" />
                <div>
                  <span className="font-semibold">Apache Kafka</span>
                  <p className="text-xs text-muted-foreground">Event Bus & Stream Processing</p>
                </div>
              </div>
              <div className="flex items-center gap-3 rounded-xl border border-dashed border-border bg-card/50 p-4 backdrop-blur-sm">
                <Database className="h-6 w-6 text-muted-foreground" />
                <div>
                  <span className="font-semibold">PostgreSQL</span>
                  <p className="text-xs text-muted-foreground">One DB per Microservice</p>
                </div>
              </div>
            </motion.div>
          </div>
        </div>
      </div>
    </Section>
  )
}
