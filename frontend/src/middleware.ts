
import type { NextRequest } from "next/server"
import { NextResponse } from "next/server"

export function middleware(request: NextRequest) {
    // Simple check for token existence on protected routes
    // Note: Since we store token in localStorage (client-side),
    // middleware (server-side) can't see it unless it's in a cookie.
    // FOR THIS DEMO: We will rely on Client-side protection in Layout/Hook mostly,
    // OR we assume the user might set a cookie.

    // However, strict middleware protection requires cookies.
    // If we stick to the user's "localStorage" request implied by the earlier code,
    // Middleware can't do much.

    // BUT: The prompt asked for "Middleware-based route protection".
    // This implies we SHOULD use cookies.
    // I'll adjust the Axios interceptor logic slightly to also set a cookie if possible,
    // or just implement the middleware logic assuming a cookie named 'token' exists.

    const token = request.cookies.get("token")?.value
    const isAuthPage = request.nextUrl.pathname.startsWith("/login") ||
        request.nextUrl.pathname.startsWith("/register")

    if (request.nextUrl.pathname.startsWith("/dashboard")) {
        if (!token) {
            return NextResponse.redirect(new URL("/login", request.url))
        }
    }

    if (isAuthPage) {
        if (token) {
            return NextResponse.redirect(new URL("/dashboard", request.url))
        }
    }

    return NextResponse.next()
}

export const config = {
    matcher: ["/dashboard/:path*", "/login", "/register"],
}
