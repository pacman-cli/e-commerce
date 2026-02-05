
import { OrderList } from "@/features/orders/order-list"

export default function OrdersPage() {
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-3xl font-bold tracking-tight">Orders</h2>
      </div>
      <OrderList />
    </div>
  )
}
