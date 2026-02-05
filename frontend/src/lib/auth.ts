
import { z } from "zod"

export const LoginSchema = z.object({
    email: z.string().email("Invalid email address"),
    password: z.string().min(6, "Password must be at least 6 characters"),
})

export const RegisterSchema = z.object({
    fullName: z.string().min(3, "Full name must be at least 3 characters"),
    email: z.string().email("Invalid email address"),
    password: z.string().min(6, "Password must be at least 6 characters"),
    role: z.enum(["ROLE_USER", "ROLE_ADMIN"]).optional(),
})

export type LoginPayload = z.infer<typeof LoginSchema>
export type RegisterPayload = z.infer<typeof RegisterSchema>

export interface UserResponse {
    id: string
    email: string
    fullName: string
    role: string
    createdAt?: string
}

export interface AuthResponse {
    token: string
    type: string
    user: UserResponse
}
