
import { AuthResponse, LoginPayload, RegisterPayload } from "@/lib/auth"
import { api } from "@/lib/axios"

export const AuthService = {
    login: async (credentials: LoginPayload): Promise<AuthResponse> => {
        const { data } = await api.post<AuthResponse>("/api/users/login", credentials)
        return data
    },

    register: async (payload: RegisterPayload): Promise<any> => {
        const { data } = await api.post("/api/users/register", payload)
        return data
    },

    getCurrentUser: async (): Promise<AuthResponse> => {
        // Modify URL if backend has a dedicated /me endpoint.
        // Assuming we reconstruct state from token or stored data for now,
        // but if we need to validate token:
        // return api.get("/api/auth/me");
        return JSON.parse(localStorage.getItem("user") || "null")
    },

    logout: () => {
        localStorage.removeItem("token")
        localStorage.removeItem("user")
    }
}
