
"use client"

import { Button } from "@/components/ui/button"
import { useAuth } from "@/hooks/use-auth"
import { cn } from "@/lib/utils"
import {
  Bell,
  CreditCard,
  LayoutDashboard,
  LogOut,
  Package,
  ShoppingCart,
  Users
} from "lucide-react"
import Link from "next/link"
import { usePathname } from "next/navigation"

const routes = [
  {
    label: "Dashboard",
    icon: LayoutDashboard,
    href: "/dashboard",
    color: "text-sky-500",
  },
  {
    label: "Orders",
    icon: ShoppingCart,
    href: "/dashboard/orders",
    color: "text-violet-500",
  },
  {
    label: "Products", // Assuming Products/Inventory logic exists or part of Orders
    icon: Package,
    href: "/dashboard/products",
    color: "text-pink-700",
  },
  {
    label: "Payments",
    icon: CreditCard,
    href: "/dashboard/payments",
    color: "text-emerald-500",
  },
  {
    label: "Notifications",
    icon: Bell,
    href: "/dashboard/notifications",
    color: "text-orange-500",
  },
  {
    label: "Users", // Admin only usually
    icon: Users,
    href: "/dashboard/users",
    color: "text-gray-500",
  }
]

export function Sidebar() {
  const pathname = usePathname()
  const { logout } = useAuth()

  return (
    <div className="space-y-4 py-4 flex flex-col h-full bg-slate-900 text-white">
      <div className="px-3 py-2 flex-1">
        <Link href="/dashboard" className="flex items-center pl-3 mb-14">
          <div className="relative w-8 h-8 mr-4">
            {/* Logo Placeholder */}
            <div className="bg-white w-full h-full rounded-full flex items-center justify-center text-slate-900 font-bold">V</div>
          </div>
          <h1 className="text-2xl font-bold">Vertex</h1>
        </Link>
        <div className="space-y-1">
          {routes.map((route) => (
            <Link
              key={route.href}
              href={route.href}
              className={cn(
                "text-sm group flex p-3 w-full justify-start font-medium cursor-pointer hover:text-white hover:bg-white/10 rounded-lg transition",
                pathname === route.href ? "text-white bg-white/10" : "text-zinc-400"
              )}
            >
              <div className="flex items-center flex-1">
                <route.icon className={cn("h-5 w-5 mr-3", route.color)} />
                {route.label}
              </div>
            </Link>
          ))}
        </div>
      </div>
      <div className="px-3 py-2">
        <Button
          onClick={() => logout()}
          variant="ghost"
          className="w-full justify-start text-zinc-400 hover:text-white hover:bg-white/10"
        >
          <LogOut className="h-5 w-5 mr-3" />
          Logout
        </Button>
      </div>
    </div>
  )
}
