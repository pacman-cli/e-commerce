
"use client"

import { MagneticButton } from "@/components/landing/ui/magnetic-button"
import { Section } from "@/components/landing/ui/section"
import { Button } from "@/components/ui/button"
import { motion } from "framer-motion"
import { ArrowRight, Sparkles } from "lucide-react"
import Link from "next/link"

// Premium easing
const easeOutSoft: [number, number, number, number] = [0.16, 1, 0.3, 1]

export function CTA() {
  return (
    <Section className="py-36 md:py-52">
      <div className="container mx-auto px-4 max-w-4xl text-center">
        <motion.div
          initial={{ opacity: 0, scale: 0.98 }}
          whileInView={{ opacity: 1, scale: 1 }}
          viewport={{ once: true }}
          transition={{ duration: 0.8, ease: easeOutSoft }}
          className="relative"
        >
          {/* Background Glow - Subtle, no pulse */}
          <div className="absolute inset-0 -z-10">
            <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 h-[350px] w-[350px] rounded-full bg-primary/15 blur-[120px]" />
          </div>

          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ delay: 0.1, duration: 0.7, ease: easeOutSoft }}
            className="inline-flex items-center gap-2 rounded-full border border-primary/15 bg-primary/5 px-4 py-1.5 text-sm mb-10"
          >
            <Sparkles className="h-3.5 w-3.5 text-primary" />
            <span className="text-primary font-medium">Open Source Forever</span>
          </motion.div>

          <motion.h2
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ delay: 0.2, duration: 0.8, ease: easeOutSoft }}
            className="text-4xl font-bold tracking-tight sm:text-5xl md:text-6xl mb-8"
          >
            Ready to build
            <br />
            <span className="text-primary">
              something amazing?
            </span>
          </motion.h2>

          <motion.p
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ delay: 0.3, duration: 0.7, ease: easeOutSoft }}
            className="text-lg text-muted-foreground max-w-xl mx-auto mb-12 leading-relaxed"
          >
            Join thousands of developers building the next generation of scalable platforms.
          </motion.p>

          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ delay: 0.4, duration: 0.7, ease: easeOutSoft }}
            className="flex flex-col sm:flex-row gap-4 justify-center"
          >
            {/* Clean Button - No aggressive gradient border */}
            <MagneticButton strength={0.2}>
              <Link href="/register">
                <Button size="lg" className="h-14 px-10 text-base rounded-full shadow-lg shadow-primary/20 hover:shadow-primary/30 transition-all duration-400">
                  Get Started Free
                  <ArrowRight className="ml-2 h-4 w-4 transition-transform duration-300 group-hover:translate-x-0.5" />
                </Button>
              </Link>
            </MagneticButton>

            <MagneticButton strength={0.15}>
              <Link href="#architecture">
                <Button variant="ghost" size="lg" className="h-14 px-10 text-base rounded-full transition-all duration-300">
                  Explore Architecture
                </Button>
              </Link>
            </MagneticButton>
          </motion.div>
        </motion.div>
      </div>
    </Section>
  )
}
