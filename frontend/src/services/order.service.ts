
import { api } from "@/lib/axios"

export interface Order {
    id: string
    userId: string
    userEmail: string
    totalAmount: number
    status: "CREATED" | "PAID" | "SHIPPED" | "CANCELLED"
    createdAt: string
    items: any[]
}

export interface PageOrderResponse {
    content: Order[]
    totalElements: number
    totalPages: number
}

export const OrderService = {
    getOrders: async (page = 0, size = 10): Promise<PageOrderResponse> => {
        const { data } = await api.get<PageOrderResponse>(`/api/orders?page=${page}&size=${size}`)
        return data
    },

    getOrder: async (id: string): Promise<Order> => {
        const { data } = await api.get<Order>(`/api/orders/${id}`)
        return data
    },

    createOrder: async (payload: any): Promise<any> => {
        const { data } = await api.post("/api/orders", payload)
        return data
    }
}
