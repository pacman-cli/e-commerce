
"use client"

import { motion, useScroll, useTransform } from "framer-motion"

export function AnimatedBackground() {
  const { scrollYProgress } = useScroll()
  const y = useTransform(scrollYProgress, [0, 1], [0, -80])
  const opacity = useTransform(scrollYProgress, [0, 0.5], [0.4, 0.15])

  return (
    <div className="fixed inset-0 -z-10 h-full w-full overflow-hidden bg-background">
      {/* Animated Gradient Mesh - Very Slow, Subtle */}
      <motion.div
        style={{ opacity }}
        className="absolute inset-0"
      >
        {/* Primary Blob - 40s loop for calm feel */}
        <motion.div
          animate={{
            x: [0, 60, 30, 0],
            y: [0, 30, 60, 0],
            scale: [1, 1.05, 0.98, 1],
          }}
          transition={{
            duration: 40,
            repeat: Infinity,
            ease: "linear",
          }}
          className="absolute top-0 left-1/4 h-[700px] w-[700px] -translate-x-1/2 rounded-full bg-gradient-to-br from-primary/15 to-purple-500/8 blur-[150px]"
        />

        {/* Secondary Blob - Offset timing */}
        <motion.div
          animate={{
            x: [0, -50, -25, 0],
            y: [0, 50, 25, 0],
            scale: [1, 0.97, 1.03, 1],
          }}
          transition={{
            duration: 45,
            repeat: Infinity,
            ease: "linear",
            delay: 10,
          }}
          className="absolute top-1/2 right-1/4 h-[600px] w-[600px] rounded-full bg-gradient-to-br from-blue-500/8 to-cyan-500/5 blur-[130px]"
        />
      </motion.div>

      {/* Grid Pattern - Subtle Parallax */}
      <motion.div
        style={{ y }}
        className="absolute inset-0 bg-[linear-gradient(to_right,#80808006_1px,transparent_1px),linear-gradient(to_bottom,#80808006_1px,transparent_1px)] bg-[size:56px_56px] [mask-image:radial-gradient(ellipse_80%_50%_at_50%_0%,#000_70%,transparent_100%)]"
      />

      {/* Noise Overlay - Very Subtle */}
      <div
        className="pointer-events-none absolute inset-0 opacity-[0.012]"
        style={{
          backgroundImage: `url("data:image/svg+xml,%3Csvg viewBox='0 0 256 256' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noiseFilter'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noiseFilter)'/%3E%3C/svg%3E")`,
        }}
      />
    </div>
  )
}
