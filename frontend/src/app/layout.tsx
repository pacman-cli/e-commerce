import "./globals.css"
import { Providers } from "./providers"

export const metadata = {
  title: "SaaS Dashboard",
  description: "Microservices Frontend",
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body className="antialiased font-sans">
        <Providers>{children}</Providers>
      </body>
    </html>
  )
}
