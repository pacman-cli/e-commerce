
import axios, { AxiosError } from "axios";

// Default to localhost:8080 if not specified
const BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

type APIError = {
  timestamp: string;
  status: number;
  error: string;
  path: string;
};

export const api = axios.create({
  baseURL: BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
  withCredentials: true, // If we used cookies, but we might use localStorage for JWT based on request
});

// Request Interceptor: Add JWT & Correlation ID
api.interceptors.request.use(
  (config) => {
    // Inject Correlation ID
    const correlationId = crypto.randomUUID();
    config.headers["X-Correlation-Id"] = correlationId;

    // Inject Auth Token
    // Ideally use HttpOnly cookies, but if simple JWT in localStorage:
    if (typeof window !== "undefined") {
      const token = localStorage.getItem("token");
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response Interceptor: Handle Errors globally
api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<APIError>) => {
    const originalRequest = error.config;

    // Handle 401: Unauthorized (Logout)
    if (error.response?.status === 401) {
      if (typeof window !== "undefined") {
        localStorage.removeItem("token");
        localStorage.removeItem("user");
        // Optional: Redirect to login or use a store to trigger logout state
        // window.location.href = "/login";
      }
    }

    return Promise.reject(error);
  }
);
