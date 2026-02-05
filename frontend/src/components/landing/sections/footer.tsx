
export function Footer() {
  return (
    <footer className="border-t py-12 bg-muted/20">
      <div className="container mx-auto px-4 max-w-screen-xl flex flex-col md:flex-row justify-between items-center gap-6">
        <div className="flex items-center gap-2 font-bold text-xl">
          <div className="bg-primary text-primary-foreground w-8 h-8 rounded-full flex items-center justify-center">V</div>
          <span>Vertex</span>
        </div>

        <div className="flex gap-8 text-sm text-muted-foreground">
          <a href="#" className="hover:text-foreground transition-colors">Documentation</a>
          <a href="#" className="hover:text-foreground transition-colors">Architecture</a>
          <a href="#" className="hover:text-foreground transition-colors">GitHub</a>
        </div>

        <p className="text-sm text-muted-foreground">
          &copy; 2026 Vertex. Open Source.
        </p>
      </div>
    </footer>
  )
}
