
import { QueryClient } from "@tanstack/react-query"

export const queryClient = new QueryClient({
    defaultOptions: {
        queries: {
            retry: 1, // Retry failed queries once
            refetchOnWindowFocus: false, // Disable aggressive refetching
            staleTime: 1000 * 60 * 5, // Data stale after 5 mins
        },
    },
})
