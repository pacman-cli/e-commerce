
"use client"

import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { OrderService } from "@/services/order.service"
import { useQuery } from "@tanstack/react-query"
import { Loader2 } from "lucide-react"

// Basic formatter if date-fns not present
const formatDate = (dateString: string) => {
  try {
    return new Date(dateString).toLocaleDateString()
  } catch {
    return dateString
  }
}

export function OrderList() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ["orders"],
    queryFn: () => OrderService.getOrders(0, 20),
  })

  if (isLoading) {
    return (
      <div className="flex justify-center items-center h-64">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    )
  }

  if (isError) {
    return (
      <div className="text-center text-red-500 py-10">
        Failed to load orders.
      </div>
    )
  }

  return (
    <div className="rounded-md border">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Order ID</TableHead>
            <TableHead>Customer</TableHead>
            <TableHead>Status</TableHead>
            <TableHead className="text-right">Amount</TableHead>
            <TableHead className="text-right">Date</TableHead>
            <TableHead className="text-right">Actions</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {data?.content?.length === 0 && (
            <TableRow>
              <TableCell colSpan={6} className="text-center h-24">No orders found.</TableCell>
            </TableRow>
          )}
          {data?.content?.map((order) => (
            <TableRow key={order.id}>
              <TableCell className="font-medium">{order.id.substring(0, 8)}...</TableCell>
              <TableCell>{order.userEmail}</TableCell>
              <TableCell>
                <Badge variant={
                  order.status === "PAID" ? "default" :
                    order.status === "CANCELLED" ? "destructive" :
                      order.status === "SHIPPED" ? "secondary" : "outline"
                }>
                  {order.status}
                </Badge>
              </TableCell>
              <TableCell className="text-right">${order.totalAmount.toFixed(2)}</TableCell>
              <TableCell className="text-right">{formatDate(order.createdAt)}</TableCell>
              <TableCell className="text-right">
                <Button variant="ghost" size="sm">View</Button>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  )
}
