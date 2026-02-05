
"use client"

import { Section } from "@/components/landing/ui/section"
import { motion } from "framer-motion"
import { Quote } from "lucide-react"

const testimonials = [
  {
    quote: "This platform transformed how we think about e-commerce infrastructure. The event-driven architecture handles our peak loads effortlessly.",
    author: "Sarah Chen",
    role: "CTO, ScaleUp Commerce",
    avatar: "SC",
  },
  {
    quote: "We went from prototype to production in weeks, not months. The documentation and developer experience is world-class.",
    author: "Marcus Williams",
    role: "Lead Engineer, TechFlow",
    avatar: "MW",
  },
  {
    quote: "Finally, a microservices template that doesn't require weeks of configuration. It just works.",
    author: "Elena Rodriguez",
    role: "Founder, DevStartup",
    avatar: "ER",
  },
]

export function Testimonials() {
  return (
    <Section className="py-24 md:py-40">
      <div className="container mx-auto px-4 max-w-screen-xl">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="text-center mb-16"
        >
          <h2 className="text-4xl font-bold tracking-tight md:text-5xl mb-4">
            Loved by Developers
          </h2>
          <p className="text-lg text-muted-foreground max-w-xl mx-auto">
            See what engineering teams are saying about the platform.
          </p>
        </motion.div>

        <div className="grid gap-8 md:grid-cols-3">
          {testimonials.map((testimonial, i) => (
            <motion.div
              key={testimonial.author}
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              transition={{ delay: i * 0.1, duration: 0.5 }}
              viewport={{ once: true }}
              className="relative"
            >
              <div className="relative rounded-2xl border border-border bg-card p-6 shadow-sm hover:shadow-md transition-shadow">
                <Quote className="absolute top-6 right-6 h-8 w-8 text-primary/10" />
                <p className="text-muted-foreground leading-relaxed mb-6 relative z-10">
                  "{testimonial.quote}"
                </p>
                <div className="flex items-center gap-3">
                  <div className="flex h-10 w-10 items-center justify-center rounded-full bg-primary/10 text-primary font-semibold text-sm">
                    {testimonial.avatar}
                  </div>
                  <div>
                    <p className="font-semibold text-sm">{testimonial.author}</p>
                    <p className="text-xs text-muted-foreground">{testimonial.role}</p>
                  </div>
                </div>
              </div>
            </motion.div>
          ))}
        </div>
      </div>
    </Section>
  )
}
