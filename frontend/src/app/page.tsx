
"use client"

import { BuiltForScale } from "@/components/landing/scale"
import { BuiltForDevelopers } from "@/components/landing/sections/built-for-developers"
import { CTA } from "@/components/landing/sections/cta"
import { DevExperience } from "@/components/landing/sections/dev-experience"
import { Features } from "@/components/landing/sections/features"
import { Footer } from "@/components/landing/sections/footer"
import { Hero } from "@/components/landing/sections/hero"
import { Stats } from "@/components/landing/sections/stats"
import { Testimonials } from "@/components/landing/sections/testimonials"
import { AnimatedBackground } from "@/components/landing/ui/animated-background"
import { MagneticButton } from "@/components/landing/ui/magnetic-button"
import { SectionDivider } from "@/components/landing/ui/section-divider"
import { Button } from "@/components/ui/button"
import { motion } from "framer-motion"
import Link from "next/link"

export default function Home() {
  return (
    <div className="flex min-h-screen flex-col antialiased selection:bg-primary/20 selection:text-primary">
      <AnimatedBackground />

      {/* Fixed Header */}
      <motion.header
        initial={{ y: -100, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        transition={{ duration: 0.6, ease: [0.16, 1, 0.3, 1] }}
        className="fixed top-0 z-50 w-full border-b border-border/40 bg-background/80 backdrop-blur-xl supports-[backdrop-filter]:bg-background/60"
      >
        <div className="container mx-auto px-4 max-w-screen-xl flex h-16 items-center justify-between">
          <Link href="/" className="flex items-center gap-2 font-bold text-xl group">
            <motion.div
              whileHover={{ scale: 1.05 }}
              transition={{ duration: 0.3 }}
              className="bg-primary text-primary-foreground w-9 h-9 rounded-lg flex items-center justify-center shadow-lg shadow-primary/20"
            >
              V
            </motion.div>
            <span className="group-hover:text-primary transition-colors duration-300">Vertex</span>
          </Link>

          <nav className="hidden md:flex items-center gap-8 text-sm text-muted-foreground">
            <a href="#features" className="hover:text-foreground transition-colors duration-300 relative group">
              Features
              <span className="absolute -bottom-1 left-0 w-0 h-0.5 bg-primary transition-all duration-300 group-hover:w-full" />
            </a>
            <a href="#scale" className="hover:text-foreground transition-colors duration-300 relative group">
              Architecture
              <span className="absolute -bottom-1 left-0 w-0 h-0.5 bg-primary transition-all duration-300 group-hover:w-full" />
            </a>
            <a href="#developers" className="hover:text-foreground transition-colors duration-300 relative group">
              Developers
              <span className="absolute -bottom-1 left-0 w-0 h-0.5 bg-primary transition-all duration-300 group-hover:w-full" />
            </a>
          </nav>

          <div className="flex items-center gap-4">
            <Link href="/login">
              <Button variant="ghost" className="text-muted-foreground hover:text-foreground transition-colors duration-300">
                Log in
              </Button>
            </Link>
            <MagneticButton strength={0.15}>
              <Link href="/register">
                <Button className="rounded-full shadow-md shadow-primary/10 hover:shadow-primary/20 transition-all duration-300">
                  Get Started
                </Button>
              </Link>
            </MagneticButton>
          </div>
        </div>
      </motion.header>

      <main className="flex-1 pt-16">
        {/* Hero - The Hook */}
        <Hero />

        {/* Stats - Proof Points */}
        <Stats />

        <SectionDivider />

        {/* Built for Scale - Scroll-Driven Architecture Visualization */}
        <div id="scale">
          <BuiltForScale />
        </div>

        <SectionDivider />

        {/* Built for Developers - Premium Scroll Section */}
        <div id="developers">
          <BuiltForDevelopers />
        </div>

        <SectionDivider />

        {/* Features - Capabilities */}
        <div id="features">
          <Features />
        </div>

        {/* Code Experience */}
        <DevExperience />

        <SectionDivider />

        {/* Testimonials - Social Proof */}
        <Testimonials />

        {/* CTA - The Close */}
        <CTA />
      </main>

      <Footer />
    </div>
  )
}
