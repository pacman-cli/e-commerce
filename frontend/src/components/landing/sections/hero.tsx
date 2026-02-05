
"use client"

import { MagneticButton } from "@/components/landing/ui/magnetic-button"
import { Section } from "@/components/landing/ui/section"
import { Button } from "@/components/ui/button"
import { motion } from "framer-motion"
import { ArrowRight, ChevronRight, Github, Sparkles } from "lucide-react"
import Link from "next/link"

// Premium easing - confident and calm
const easeOutSoft: [number, number, number, number] = [0.16, 1, 0.3, 1]

// Slower, calmer animation variants
const containerVariants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: {
      staggerChildren: 0.2,
      delayChildren: 0.1,
    },
  },
}

const itemVariants = {
  hidden: { opacity: 0, y: 24, filter: "blur(8px)" },
  visible: {
    opacity: 1,
    y: 0,
    filter: "blur(0px)",
    transition: {
      duration: 1,
      ease: easeOutSoft,
    },
  },
}

const badgeVariants = {
  hidden: { opacity: 0, scale: 0.9 },
  visible: {
    opacity: 1,
    scale: 1,
    transition: { duration: 0.6, ease: easeOutSoft }
  },
}

// Company logos for the marquee
const companies = [
  "Acme Corp",
  "TechGiant",
  "StartupX",
  "FinanceApp",
  "DataFlow",
  "CloudNine",
  "DevStack",
  "ScaleUp",
]

// Infinite scrolling marquee component
function LogoMarquee() {
  return (
    <div className="relative w-full overflow-hidden">
      {/* Gradient masks on sides */}
      <div className="absolute left-0 top-0 bottom-0 w-20 bg-gradient-to-r from-background to-transparent z-10 pointer-events-none" />
      <div className="absolute right-0 top-0 bottom-0 w-20 bg-gradient-to-l from-background to-transparent z-10 pointer-events-none" />

      {/* Scrolling container */}
      <motion.div
        animate={{ x: [0, -1920] }}
        transition={{
          x: {
            repeat: Infinity,
            repeatType: "loop",
            duration: 30,
            ease: "linear",
          },
        }}
        className="flex gap-16 whitespace-nowrap"
      >
        {/* First set of logos */}
        {companies.map((company, i) => (
          <div
            key={`a-${i}`}
            className="flex items-center gap-3 text-muted-foreground/50 hover:text-muted-foreground transition-colors duration-300"
          >
            <div className="w-8 h-8 rounded-lg bg-muted/50 flex items-center justify-center text-xs font-bold">
              {company.charAt(0)}
            </div>
            <span className="font-medium text-sm">{company}</span>
          </div>
        ))}

        {/* Second set (for seamless loop) */}
        {companies.map((company, i) => (
          <div
            key={`b-${i}`}
            className="flex items-center gap-3 text-muted-foreground/50 hover:text-muted-foreground transition-colors duration-300"
          >
            <div className="w-8 h-8 rounded-lg bg-muted/50 flex items-center justify-center text-xs font-bold">
              {company.charAt(0)}
            </div>
            <span className="font-medium text-sm">{company}</span>
          </div>
        ))}

        {/* Third set (extra buffer for wide screens) */}
        {companies.map((company, i) => (
          <div
            key={`c-${i}`}
            className="flex items-center gap-3 text-muted-foreground/50 hover:text-muted-foreground transition-colors duration-300"
          >
            <div className="w-8 h-8 rounded-lg bg-muted/50 flex items-center justify-center text-xs font-bold">
              {company.charAt(0)}
            </div>
            <span className="font-medium text-sm">{company}</span>
          </div>
        ))}
      </motion.div>
    </div>
  )
}

export function Hero() {
  return (
    <Section className="relative min-h-screen pt-12 pb-24 md:pt-16 md:pb-36 flex flex-col items-center text-center overflow-hidden">
      <motion.div
        variants={containerVariants}
        initial="hidden"
        animate="visible"
        className="relative z-10 mx-auto flex max-w-[980px] flex-col items-center gap-8 px-4"
      >
        {/* Badge / Pill - Subtle, no ping animation */}
        <motion.div
          variants={badgeVariants}
          className="inline-flex items-center gap-2 rounded-full border border-primary/15 bg-primary/5 px-4 py-1.5 text-sm backdrop-blur-sm"
        >
          <Sparkles className="h-3.5 w-3.5 text-primary" />
          <span className="text-primary font-medium">v2.0 — Production Ready</span>
          <div className="mx-2 h-4 w-[1px] bg-primary/15" />
          <span className="flex items-center text-muted-foreground hover:text-primary transition-colors duration-300 cursor-pointer group">
            Read the docs
            <ChevronRight className="ml-1 h-3 w-3 transition-transform duration-300 group-hover:translate-x-0.5" />
          </span>
        </motion.div>

        {/* Main Headline */}
        <motion.h1
          variants={itemVariants}
          className="text-5xl font-bold tracking-tight sm:text-7xl md:text-8xl max-w-4xl leading-[1.05]"
        >
          <span className="bg-gradient-to-br from-foreground via-foreground to-muted-foreground/70 bg-clip-text text-transparent">
            Build for Scale.
          </span>
          <br />
          <span className="text-primary">
            Ship with Confidence.
          </span>
        </motion.h1>

        {/* Subtext */}
        <motion.p
          variants={itemVariants}
          className="max-w-2xl text-lg text-muted-foreground sm:text-xl leading-relaxed"
        >
          The open-source microservices platform engineered for reliability.
          Event-driven, secure by default, and ready to deploy.
        </motion.p>

        {/* Action Buttons */}
        <motion.div
          variants={itemVariants}
          className="mt-6 flex flex-wrap justify-center gap-4"
        >
          <MagneticButton strength={0.2}>
            <Link href="/register">
              <Button size="lg" className="h-13 px-8 text-base rounded-full shadow-lg shadow-primary/15 hover:shadow-primary/25 transition-all duration-300">
                Start Building
                <ArrowRight className="ml-2 h-4 w-4 transition-transform duration-300 group-hover:translate-x-0.5" />
              </Button>
            </Link>
          </MagneticButton>

          <MagneticButton strength={0.15}>
            <Link href="https://github.com/your-repo" target="_blank">
              <Button variant="outline" size="lg" className="h-13 px-8 text-base rounded-full border-muted-foreground/15 hover:bg-muted/30 transition-all duration-300">
                <Github className="mr-2 h-4 w-4" />
                View on GitHub
              </Button>
            </Link>
          </MagneticButton>
        </motion.div>
      </motion.div>

      {/* Trusted By - Horizontal Scrolling Marquee */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 1.5, duration: 1 }}
        className="absolute bottom-24 left-0 right-0 w-full"
      >
        <p className="text-xs text-muted-foreground/50 tracking-widest uppercase text-center mb-6">
          Trusted by engineering teams at
        </p>
        <LogoMarquee />
      </motion.div>
    </Section>
  )
}
