
import { LoginPayload, RegisterPayload } from "@/lib/auth"
import { AuthService } from "@/services/auth.service"
import { useMutation, useQueryClient } from "@tanstack/react-query"
import Cookies from "js-cookie"
import { useRouter } from "next/navigation"
import { toast } from "sonner"

export function useAuth() {
    const queryClient = useQueryClient()
    const router = useRouter()

    const loginMutation = useMutation({
        mutationFn: (credentials: LoginPayload) => AuthService.login(credentials),
        onSuccess: (data) => {
            // Save token & user
            localStorage.setItem("token", data.token)
            localStorage.setItem("user", JSON.stringify(data))
            Cookies.set("token", data.token, { expires: 7 }) // Set cookie for middleware

            toast.success("Login successful")
            router.push("/dashboard")
        },
        onError: (error: any) => {
            toast.error(error.response?.data?.message || error.message || "Login failed")
        },
    })

    const registerMutation = useMutation({
        mutationFn: (payload: RegisterPayload) => AuthService.register(payload),
        onSuccess: () => {
            toast.success("Registration successful! Please login.")
            router.push("/login") // Redirect to login after register
        },
        onError: (error: any) => {
            toast.error(error.response?.data?.message || "Registration failed")
        },
    })

    const logout = () => {
        AuthService.logout()
        Cookies.remove("token")
        queryClient.clear()
        router.push("/login")
        toast.info("Logged out")
    }

    return {
        login: loginMutation,
        register: registerMutation,
        logout,
    }
}
