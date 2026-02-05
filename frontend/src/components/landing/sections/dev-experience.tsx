
"use client"

import { Section } from "@/components/landing/ui/section"
import { motion } from "framer-motion"
import { Check } from "lucide-react"

const CodeLine = ({ children, delay = 0, color = "text-foreground" }: { children: React.ReactNode, delay?: number, color?: string }) => (
  <motion.div
    initial={{ opacity: 0, x: -10 }}
    whileInView={{ opacity: 1, x: 0 }}
    transition={{ delay, duration: 0.3 }}
    viewport={{ once: true }}
    className={`font-mono text-sm md:text-base ${color}`}
  >
    {children}
  </motion.div>
)

export function DevExperience() {
  return (
    <Section className="bg-black text-white py-24 md:py-32">
      <div className="container mx-auto px-4 grid md:grid-cols-2 gap-16 items-center">
        {/* Left: Copy */}
        <div className="space-y-8">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
          >
            <h2 className="text-3xl font-bold tracking-tight md:text-5xl bg-gradient-to-r from-white to-neutral-400 bg-clip-text text-transparent">
              Developer First. <br />
              Production Ready.
            </h2>
            <p className="mt-4 text-neutral-400 text-lg">
              Forget complex setups. We've containerized everything.
              Spin up the entire stack with a single command and get meaningful observability out of the box.
            </p>
          </motion.div>

          <ul className="space-y-4">
            {[
              "Dockerized Microservices Environment",
              "Hot-reload enabled for Next.js Frontend",
              "Pre-configured Kafka Topics",
              "PostgreSQL containers with persistence"
            ].map((item, i) => (
              <motion.li
                key={i}
                initial={{ opacity: 0, x: -20 }}
                whileInView={{ opacity: 1, x: 0 }}
                transition={{ delay: 0.2 + (i * 0.1) }}
                viewport={{ once: true }}
                className="flex items-center gap-3 text-neutral-300"
              >
                <div className="h-6 w-6 rounded-full bg-green-500/10 flex items-center justify-center">
                  <Check className="h-3.5 w-3.5 text-green-500" />
                </div>
                {item}
              </motion.li>
            ))}
          </ul>
        </div>

        {/* Right: Code Block */}
        <motion.div
          initial={{ opacity: 0, scale: 0.95 }}
          whileInView={{ opacity: 1, scale: 1 }}
          viewport={{ once: true }}
          className="relative rounded-xl border border-white/10 bg-neutral-900 shadow-2xl overflow-hidden"
        >
          <div className="flex items-center gap-2 border-b border-white/5 bg-white/5 px-4 py-3">
            <div className="h-3 w-3 rounded-full bg-red-500/20" />
            <div className="h-3 w-3 rounded-full bg-yellow-500/20" />
            <div className="h-3 w-3 rounded-full bg-green-500/20" />
            <div className="ml-2 text-xs text-neutral-500 font-mono">zsh — 80x24</div>
          </div>

          <div className="p-6 space-y-2">
            <CodeLine delay={0.2} color="text-green-400">➜  ~ git clone https://github.com/vertex/platform.git</CodeLine>
            <CodeLine delay={0.8}>Cloning into 'microservices'...</CodeLine>
            <CodeLine delay={1.2}>remote: Enumerating objects: 1842, done.</CodeLine>
            <CodeLine delay={1.6}>Receiving objects: 100% (1842/1842), 4.20 MiB | 2.14 MiB/s, done.</CodeLine>
            <CodeLine delay={2.0} color="text-green-400">➜  ~ cd microservices</CodeLine>
            <CodeLine delay={2.5} color="text-green-400">➜  microservices git:(main) docker-compose up -d</CodeLine>
            <CodeLine delay={3.5} color="text-blue-400">[+] Running 8/8</CodeLine>
            <CodeLine delay={3.6} color="text-neutral-300"> ⠿ Container zookeeper      Started</CodeLine>
            <CodeLine delay={3.7} color="text-neutral-300"> ⠿ Container kafka          Started</CodeLine>
            <CodeLine delay={3.8} color="text-neutral-300"> ⠿ Container user-db        Started</CodeLine>
            <CodeLine delay={3.9} color="text-neutral-300"> ⠿ Container user-service   Started</CodeLine>
            <CodeLine delay={4.0} color="text-green-400">➜  microservices git:(main) <span className="animate-pulse">_</span></CodeLine>
          </div>
        </motion.div>
      </div>
    </Section>
  )
}
