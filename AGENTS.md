# Repository Instructions

## Structure

- `frontend/` is the runnable Next.js 16 App Router application (React 19, TypeScript strict mode, Tailwind CSS 4, shadcn configuration).
- The backend is only partially materialized: `user-service/`, `product-service/`, and `inventory-service/` have Maven descriptors; `api-gateway/`, `order-service/`, `payment-service/`, `notification-service/`, and `shared-lib/` currently contain source files but no `pom.xml`.
- There is no root `pom.xml`, Maven parent POM, `docker-compose.yml`, or `.env.example` in the tracked repository. Treat the root README and shell scripts as aspirational where they reference those missing files; verify files exist before using their commands.

## Frontend commands

Run these from `frontend/` and use npm because `package-lock.json` is committed:

```bash
npm ci
npm run dev
npm run lint
npm run build
npm start
```

- There is no frontend test script in `package.json`; do not assume `npm test` exists.
- `@/*` resolves to `frontend/src/*`. Shared UI components follow the shadcn aliases in `frontend/components.json`.
- `NEXT_PUBLIC_API_URL` controls the API base URL and defaults to `http://localhost:8080` in `frontend/src/lib/axios.ts`.

## Frontend auth gotcha

- Axios reads the JWT from `localStorage.token` and sends it as a Bearer token.
- Next middleware protects `/dashboard/*` by checking a `token` cookie, not local storage. Changing auth flows must keep this mismatch in mind; localStorage-only login will not satisfy middleware protection on a fresh request.

## Backend and local-run gotchas

- `start.sh` assumes `.env.example` and `docker-compose.yml`, and its cleanup step runs `docker-compose down -v --remove-orphans`; do not run it blindly because `-v` removes Docker volumes and database data.
- Root `.env` values include credentials and integration secrets. The repository has no root `.gitignore`; keep local secrets untracked and never add them to commits.
- A direct check such as `mvn -f inventory-service/pom.xml validate` currently fails because the referenced `com.ecommerce:e-commerce-microservices:1.0.0` parent POM is absent. Do not report the backend as buildable until the parent/build descriptors are restored.
- Configured service ports include user `8081`, product `8085`, and inventory `8086`; the frontend gateway default is `8080`.

## Architecture notes

- External traffic is intended to enter through the API gateway; the gateway code applies rate limiting, request-size/content-type validation, security headers, and correlation IDs.
- The order saga implementation in `order-service/src/main/java/com/ecommerce/order/saga/SagaOrchestrator.java` stores active saga state in memory, so it is not restart-safe despite the repository README describing a production-oriented architecture.
