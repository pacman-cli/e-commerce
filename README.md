# E-Commerce Microservices

An in-progress e-commerce platform composed of a Next.js frontend and several Spring-based backend services. The repository demonstrates service boundaries, JWT authentication, PostgreSQL persistence, Kafka events, transactional outbox code, Redis locking/caching, and resilience patterns.

> **Repository status — verified October 26, 2026:** this checkout is a partially materialized architecture, not a runnable production deployment. The frontend builds, but its lint gate fails. The backend modules cannot be validated because the root Maven parent POM and several module POMs are absent. Docker Compose and `.env.example` are also absent, although the README and shell scripts refer to them.

## Contents

- [What exists today](#what-exists-today)
- [Architecture](#architecture)
- [Runtime components](#runtime-components)
- [Request and event flows](#request-and-event-flows)
- [Repository layout](#repository-layout)
- [API inventory](#api-inventory)
- [Data and persistence](#data-and-persistence)
- [Security model](#security-model)
- [Local development](#local-development)
- [Verification](#verification)
- [Bug and gap audit](#bug-and-gap-audit)
- [Remediation roadmap](#remediation-roadmap)
- [Contribution guidance](#contribution-guidance)

## What exists today

### Implemented or partially implemented

- **Frontend:** Next.js 16 App Router application with React 19, TypeScript, Tailwind CSS 4, React Query, forms, login/register pages, protected dashboard routes, and an order list.
- **User service:** registration, login, profile lookup, BCrypt password hashing, JWT issuance, PostgreSQL repository, and outbox publisher/poller code.
- **Product service:** category and product CRUD/read/search operations, JPA persistence, and a direct stock-reduction endpoint.
- **Inventory service:** stock initialization, status, reservation, release, deduction, PostgreSQL persistence, and Redisson-based per-product locks.
- **Order service:** order persistence, pagination, cached reads, asynchronous method wrappers, outbox publisher/poller code, API versioning classes, and a separate in-memory saga helper.
- **Payment service:** payment persistence, Kafka listener for order events, payment event publication, and an idempotency helper that is not wired into the payment flow.
- **Notification service:** Kafka listeners for user, order, and payment topics plus an email adapter whose current implementation only logs messages.
- **Shared library:** JWT utility, Kafka topic declarations, shared event DTOs, and a shared error DTO.
- **API gateway source:** JWT filter and security/validation filters are present, but the gateway build descriptor and route configuration are not present in this checkout.

### Not currently materialized

The following files/directories are referenced by existing documentation or scripts but are not tracked at the repository root:

- `pom.xml`
- `shared-lib/pom.xml`
- `api-gateway/pom.xml`
- `order-service/pom.xml`
- `payment-service/pom.xml`
- `notification-service/pom.xml`
- `docker-compose.yml`
- `.env.example`
- root-level `observability/`
- runtime configuration files for order, payment, notification, and gateway services

These omissions are the main reason the backend cannot be built or launched from a clean clone.

## Architecture

The repository's intended design is a database-per-service microservices system:

```text
Browser / Next.js frontend
          |
          | REST + Bearer JWT (frontend default: http://localhost:8080)
          v
     API Gateway source
       /   |   |   \
      v    v   v    v
  User  Product Order Inventory
    |      |      |       |
  User DB Product DB Order DB Inventory DB
                   |       |
                   +-------+---- Redis locks/cache

 User/Order outbox publishers ---> Kafka topics ---> Payment / Order / Notification listeners
                                                    |
                                             Payment DB / email adapter
```

The current code does **not** fully realize this diagram:

1. The gateway has no tracked `application.yml`, `application.properties`, Maven descriptor, or route definitions.
2. The order checkout path never calls the inventory service and never invokes `SagaOrchestrator`.
3. Kafka payload and consumer contracts are inconsistent.
4. Payment is simulated rather than integrated with Stripe.
5. Notification delivery is logging only.

### Interactive architecture artifact

The repository-backed Archify diagram is available at:

`.archify/architecture-e-commerce-20261026-0436/e-commerce-architecture.html`

Its candidate specification and automated receipts are alongside the HTML file. The final Archify run passed validation, delivery, strict checks, and browser checks. Automated review still recommends a manual route-readability inspection because the event feedback paths share several corridors.

### Architectural boundaries

| Boundary | Responsibility | Evidence |
| --- | --- | --- |
| Frontend | User-facing landing page, auth forms, dashboard, API client | `frontend/src/app/page.tsx`, `frontend/src/lib/axios.ts` |
| Gateway | Intended external edge; JWT validation, request limits, headers, correlation IDs | `api-gateway/src/main/java/com/ecommerce/gateway/filter/JwtAuthenticationFilter.java`, `api-gateway/src/main/java/com/ecommerce/gateway/config/SecurityConfig.java` |
| User | Identity lifecycle and JWT issuance | `user-service/src/main/java/com/ecommerce/user/controller/UserController.java` |
| Product | Categories, product catalog, search, product stock field | `product-service/src/main/java/com/ecommerce/product/controller/ProductController.java` |
| Inventory | Reservation state and Redis locking | `inventory-service/src/main/java/com/ecommerce/inventory/service/impl/InventoryServiceImpl.java` |
| Order | Orders, order events, outbox, caches, API versioning | `order-service/src/main/java/com/ecommerce/order/service/impl/OrderServiceImpl.java` |
| Payment | Payment records and payment event publication | `payment-service/src/main/java/com/ecommerce/payment/service/impl/PaymentServiceImpl.java` |
| Notification | Kafka consumers and email adapter boundary | `notification-service/src/main/java/com/ecommerce/notification/service/NotificationEventListener.java` |
| Shared library | JWT, topic names, event DTOs, shared errors | `shared-lib/src/main/java/com/ecommerce/shared/` |

## Runtime components

### Frontend

The frontend is a Next.js App Router application under `frontend/`.

- `/` renders a marketing-style landing page.
- `/login` and `/register` render client-side forms.
- `/dashboard` and child routes are protected by `frontend/src/middleware.ts` using a `token` cookie.
- Axios reads the JWT from `localStorage.token` and sends it as `Authorization: Bearer ...`.
- Login writes both local storage and a JavaScript-managed cookie; logout removes both.
- React Query provides the order list query and shared query client.

The frontend currently uses the following backend paths:

- `POST /api/users/login`
- `POST /api/users/register`
- `GET /api/orders`
- `GET /api/orders/{id}`
- `POST /api/orders`

The dashboard statistics, chart, recent sales panel, and order row `View` action are placeholders.

### API gateway

The gateway source contains:

- `JwtAuthenticationFilter`: validates a Bearer token with the shared `JwtUtil` and injects `X-User-Id`, `X-User-Role`, and `X-User-Email` headers.
- `RateLimitingFilter`: an in-memory IP counter with stricter limits for login/register.
- `RequestValidationFilter`: checks `Content-Length` and requires JSON for POST/PUT/PATCH.
- `SecurityHeadersFilter`: adds response security headers.
- `CorrelationIdFilter`: generates or forwards `X-Correlation-Id`.

There is no route configuration in the tracked source. Treat the gateway as an incomplete edge component until its build descriptor, configuration, downstream URI map, and deployment definition are restored.

### User service — port 8081 by default

Endpoints:

- `POST /api/users/register`
- `POST /api/users/login`
- `GET /api/users/profile` — expects `X-User-Id`.

Registration stores a BCrypt hash, creates a `UserCreated` outbox record, and returns a JWT. Login verifies the password and returns a JWT. `SecurityConfig` currently permits every request because authentication is expected to happen at the gateway.

### Product service — port 8085 by default

Endpoints:

- `POST /api/categories`
- `GET /api/categories/{id}`
- `GET /api/categories`
- `POST /api/products`
- `GET /api/products/{id}`
- `GET /api/products?page=0&size=20`
- `GET /api/products/category/{categoryId}`
- `GET /api/products/search?name=...`
- `POST /api/products/{id}/reduce-stock?quantity=...`

Product creation uses the client-provided `categoryId`, price, and stock quantity after validation. Product stock is separate from the inventory service's stock model, which creates two competing sources of truth.

### Inventory service — port 8086 by default

Endpoints:

- `POST /api/inventory/initialize/{productId}?quantity=...`
- `GET /api/inventory/{productId}`
- `POST /api/inventory/reserve`
- `POST /api/inventory/release`
- `POST /api/inventory/deduct`

Reservation, release, and deduction acquire a Redisson lock using `inventory:lock:{productId}`. The service stores available and reserved quantities in PostgreSQL. No order-service caller or Kafka consumer currently invokes these endpoints.

### Order service

The order service contains three overlapping API surfaces:

1. `OrderController` at `/api/orders`.
2. `OrderControllerV1` at `/api/v1/orders`.
3. `OrderControllerV2` at `/api/v2/orders`.

The default controller creates an order in `CREATED` state, calculates the total from client-supplied item prices, saves the order, and writes an `OrderCreated` outbox event. The service then exposes asynchronous `CompletableFuture` methods and cached reads.

The V2 controller is explicitly incomplete: it returns a generated tracking ID without storing the future, always reports `PROCESSING`, ignores the `status` and `sort` query parameters, and returns a simulated cancellation response without changing the order or issuing a refund.

### Payment service

The payment listener consumes `order-events`. `PaymentServiceImpl` writes a pending payment, then randomly returns `SUCCESS` 90% of the time and `FAILED` 10% of the time. It publishes a `PaymentProcessedEvent` directly to Kafka.

There is no Stripe client call in the Java source. The idempotency helper exists as a standalone service, but `processPayment` does not use it and the payment controller exposes only a read endpoint.

### Notification service

The service listens to:

- `user-events`
- `order-events`
- `payment-events`

`EmailServiceImpl` logs welcome, order confirmation, receipt, and failure messages. It does not inject `JavaMailSender`, use SMTP, or send an email.

## Request and event flows

### Actual user registration flow

1. The frontend submits `/api/users/register` through Axios.
2. The user service validates the DTO and checks the email.
3. The password is BCrypt-hashed and the user is saved.
4. A `UserCreated` outbox row is saved in the same user transaction.
5. The user service returns a JWT.
6. The frontend stores the response in local storage and writes a non-HttpOnly `token` cookie.
7. The outbox poller later publishes `user-events` asynchronously.

### Actual order flow

1. The frontend submits an order with product IDs, quantities, and prices.
2. The order service creates a local `Order` and `OrderItem` graph.
3. The total is calculated from the request prices.
4. The order and an outbox row are stored.
5. The outbox poller publishes an order event.
6. Payment and notification listeners attempt to consume it.
7. Payment publishes a payment event.
8. The order listener attempts to update the order status.

Inventory reservation is not part of this flow. The current implementation therefore cannot provide the documented reserve → pay → confirm/release saga.

### Declared Kafka topics

| Topic | Publisher | Consumers | Current contract risk |
| --- | --- | --- | --- |
| `user-events` | User outbox | Notification | Outbox publishes serialized strings; consumer expects a typed object with no tracked consumer configuration. |
| `order-events` | Order outbox | Payment, Notification | Outbox payload is a string; consumers expect different shared/local event classes. |
| `payment-events` | Payment service | Order, Notification | Payment emits `status=SUCCESS/FAILED`; order expects `success=true/false`; notification checks for `COMPLETED`. |
| `*.dlq` | Outbox pollers | None | DLQ send is asynchronous but events are marked processed immediately. |

## Repository layout

```text
.
├── frontend/                 # Next.js 16 application
├── api-gateway/              # Gateway source; build/config incomplete
├── user-service/             # Authentication and users
├── product-service/          # Product and category catalog
├── inventory-service/        # Stock reservations and Redis locks
├── order-service/             # Orders, outbox, caches, versioned APIs
├── payment-service/          # Payment persistence and event publisher
├── notification-service/     # Kafka listeners and email adapter boundary
├── shared-lib/               # Shared Java types and JWT utility
├── start.sh                  # Aspirational Docker startup script
├── verify.sh                 # Live smoke-test script
├── AGENTS.md                 # Local coding-assistant instructions
└── README.md
```

## API inventory

| Method | Path | Service | Auth expectation | Notes |
| --- | --- | --- | --- | --- |
| POST | `/api/users/register` | User | Public | Returns auth response immediately. |
| POST | `/api/users/login` | User | Public | Returns JWT. |
| GET | `/api/users/profile` | User | Gateway-injected `X-User-Id` | Direct service requests can forge this header unless network isolation exists. |
| GET/POST | `/api/categories` | Product | Intended bearer auth | No service-level security config is tracked. |
| GET/POST | `/api/products` | Product | Intended bearer auth | Product writes are not role-protected in source. |
| POST | `/api/products/{id}/reduce-stock` | Product | Intended internal/admin use | Quantity is not validated as positive. |
| GET | `/api/inventory/{productId}` | Inventory | Intended bearer auth | Direct controller has no security annotation. |
| POST | `/api/inventory/reserve` | Inventory | Intended internal use | Not called by order flow. |
| POST | `/api/inventory/release` | Inventory | Intended internal use | Can inflate stock when release exceeds reservation. |
| POST | `/api/inventory/deduct` | Inventory | Intended internal use | No reservation identity is checked. |
| POST | `/api/orders` | Order | Gateway-injected user headers | Trusts client-provided item prices. |
| GET | `/api/orders/{id}` | Order | Intended owner access | Current service lookup ignores the authenticated user. |
| GET | `/api/orders` | Order | Gateway-injected `X-User-Id` | Uses the user ID for list filtering. |
| GET | `/api/payments/order/{id}` | Payment | Intended bearer auth | No ownership check is visible. |
| POST | `/api/notifications/test` | Notification | Test endpoint | Sends/logs a test notification; not production delivery. |

## Data and persistence

### Service-owned data

- **User DB:** `User` and user outbox records.
- **Product DB:** `Category` and `Product` records.
- **Inventory DB:** `Inventory` with available and reserved quantities.
- **Order DB:** `Order`, `OrderItem`, outbox records, and event-sourcing helper entities.
- **Payment DB:** `Payment` and idempotency records.
- **Redis:** Redisson inventory locks and order-service cache configuration.

The repository intends to use database-per-service boundaries. However, only user, product, and inventory datasource configuration is tracked. Order and payment entities/repositories exist without corresponding service runtime configuration in this checkout.

### Outbox behavior

User and order services persist outbox rows and poll them every 100 ms. The poller:

1. Reads unprocessed rows.
2. Derives the topic from `aggregateType`.
3. Sends the stored payload to Kafka.
4. Marks the row processed in the Kafka callback.
5. Retries by incrementing `retryCount`.
6. Sends exhausted events to a `.dlq` topic.

This is a useful skeleton, but it is not safe for multiple poller instances because rows are not claimed/locked before publication, and asynchronous callback persistence is not part of the polling transaction.

## Security model

### Intended model

- User service authenticates credentials and signs JWTs.
- Gateway validates JWTs.
- Gateway forwards identity in `X-User-*` headers.
- Downstream services trust the gateway and are expected to be private.

### Current risks

- The gateway does not have tracked route/build configuration.
- User service permits all requests and accepts all origins with credentials.
- Downstream controllers rely on gateway headers rather than validating a token or enforcing network-level isolation.
- Frontend tokens are stored in both local storage and a JavaScript-readable cookie.
- The gateway accepts the first `X-Forwarded-For` value for rate limiting without a trusted proxy boundary.
- Product, inventory, and payment ownership/role checks are not visible in the service code.
- The shared JWT utility has an insecure development default if `jwt.secret` is absent in a service that loads it.

## Local development

### Frontend only

Prerequisites: Node.js and npm.

```bash
cd frontend
npm ci
npm run dev
```

Open `http://localhost:3000`. The API base URL is controlled by `NEXT_PUBLIC_API_URL` and defaults to `http://localhost:8080`.

Available frontend checks:

```bash
npm run lint
npm run build
```

### Backend reality

The documented Docker command cannot currently work from this checkout:

```bash
docker-compose up -d --build
```

There is no `docker-compose.yml`, and the Maven module POMs reference a missing parent:

```xml
<parent>
  <groupId>com.ecommerce</groupId>
  <artifactId>e-commerce-microservices</artifactId>
  <version>1.0.0</version>
</parent>
```

Before backend startup is possible, restore a root parent POM, add every module to its reactor, add `shared-lib/pom.xml`, restore service POMs/configuration, and add the runtime infrastructure definition.

Do not run `start.sh` blindly. It expects `.env.example` and `docker-compose.yml`, and it executes `docker-compose down -v --remove-orphans`, which deletes Docker volumes and database data.

## Verification

The following checks were run on commit `459977c9dc9d10a02742a2ef0e9ad181ef6c1981` on October 26, 2026:

| Check | Result | Evidence |
| --- | --- | --- |
| `cd frontend && npm run build` | **Pass** | Next.js production build completed; routes were generated. |
| `cd frontend && npm run lint` | **Fail** | 18 errors and 3 warnings. Main categories: explicit `any`, unescaped JSX punctuation, unused variable, and state update inside effect. |
| `mvn -f user-service/pom.xml validate` | **Fail** | Missing `com.ecommerce:e-commerce-microservices:1.0.0` parent POM. |
| `mvn -f product-service/pom.xml validate` | **Fail** | Same missing parent POM. |
| `mvn -f inventory-service/pom.xml validate` | **Fail** | Same missing parent POM. |
| `bash -n start.sh` | **Pass** | Shell syntax is valid. Runtime prerequisites/files are missing. |
| `bash -n verify.sh` | **Pass** | Shell syntax is valid. It requires a live gateway and has weak response assertions. |
| Archify `finalize architecture` | **Pass** | Validation, delivery, strict check, and browser check passed. |

No live end-to-end backend test was possible because the backend build and infrastructure are incomplete.

## Bug and gap audit

The findings below are ordered by impact. “Open” means the issue is present in the current checkout; no production claim is made because the backend could not be launched here.

### Blocking build and deployment findings

| ID | Severity | Finding | Evidence / consequence | Smallest useful correction |
| --- | --- | --- | --- | --- |
| B-01 | Blocker | Missing Maven reactor parent | `user-service/pom.xml:8-12`, `product-service/pom.xml:7-11`, and `inventory-service/pom.xml:7-11` reference a root parent that is absent. | Restore root `pom.xml` and reactor modules, then validate every service. |
| B-02 | Blocker | Missing module build descriptors | `api-gateway`, `order-service`, `payment-service`, `notification-service`, and `shared-lib` have source but no tracked `pom.xml`. | Add module POMs and dependency versions to the reactor. |
| B-03 | Blocker | Dockerfiles reference absent build inputs | `api-gateway/Dockerfile` copies root and service POMs that do not exist; `inventory-service/Dockerfile` expects a prebuilt `target` JAR. | Make every Dockerfile build from a clean checkout or document a verified artifact pipeline. |
| B-04 | Blocker | Missing runtime infrastructure | `start.sh` and README expect `docker-compose.yml` and `.env.example`, neither of which is tracked. | Add a compose file and a redacted env template, or remove the scripts and document another launch path. |
| B-05 | Blocker | Gateway is not launchable as documented | Gateway source has filters but no tracked route configuration or build descriptor. | Restore gateway POM, application config, downstream URIs, health checks, and route tests. |

### Critical correctness and security findings

| ID | Severity | Finding | Evidence / consequence | Smallest useful correction |
| --- | --- | --- | --- | --- |
| C-01 | Critical | Payment event contract disagrees with order consumer | Payment emits `PaymentProcessedEvent` with `status=SUCCESS/FAILED` (`payment-service/.../PaymentProcessedEvent.java:15-22`), while order consumes `PaymentCompletedEvent` with `success` (`order-service/.../PaymentCompletedEvent.java:14-20`). A successful event can deserialize with `success=false`, causing cancellation. | Define one shared versioned event DTO and use it in both producer and consumer; add contract tests. |
| C-02 | Critical | Payment success is treated as notification failure | `NotificationEventListener.java:40` checks for `COMPLETED`, but `PaymentStatus` only contains `PENDING`, `SUCCESS`, and `FAILED`. Successful payments therefore take the failure-email branch. | Compare against the shared enum/string contract, preferably with a typed status. |
| C-03 | Critical | Kafka outbox payload and consumer configuration are incompatible | User/order outbox pollers use `KafkaTemplate<String,String>` and send stored JSON strings; listeners accept typed Java objects. No tracked consumer factory/deserializer configuration exists. | Standardize JSON serialization, type headers, trusted packages, and consumer error handling; test each topic end to end. |
| C-04 | Critical | Order checkout never reserves inventory | `OrderServiceImpl.createOrder` only saves an order and publishes `OrderCreated`; no inventory client/listener/call site exists. | Make inventory reservation a real saga step before payment and persist reservation IDs. |
| C-05 | Critical | Order saga helper is not connected to checkout | `SagaOrchestrator` is only referenced by its own class; `createOrderProcessingSaga` is never called. Its state is in-memory and lost on restart. | Wire one orchestrator into the order flow and persist saga state, steps, retries, and compensations. |
| C-06 | Critical | Any authenticated user can request another user's order | `OrderController.getOrderById` receives `X-User-Id` but `OrderServiceImpl.getOrderById` queries only by order ID. V1/V2 detail endpoints are also not owner-scoped. | Query by `(orderId, userId)` or enforce an explicit admin permission. Add authorization tests. |
| C-07 | Critical | Downstream identity headers are forgeable if services are reachable directly | User service permits all requests (`SecurityConfig.java:21-28`); downstream controllers trust `X-User-Id`/`X-User-Email`. | Enforce private networking and/or validate JWTs in every service; strip external `X-User-*` headers at the edge. |
| C-08 | Critical | Client controls order prices | `CreateOrderRequest.OrderItemRequest` accepts `price`, and `OrderServiceImpl` calculates totals from that value without loading product prices. A client can underpay or create inconsistent payment records. | Accept only product ID and quantity; load authoritative prices from a catalog/pricing service. |
| C-09 | Critical | Inventory release can create stock from nothing | `InventoryServiceImpl.releaseStock` adds the requested quantity to available stock even when reserved quantity is smaller, then clamps reserved to zero. | Track reservation IDs and reject/reconcile releases greater than the reservation. |

### High-priority reliability findings

| ID | Severity | Finding | Evidence / consequence | Smallest useful correction |
| --- | --- | --- | --- | --- |
| H-01 | High | Outbox rows are not claimed before publishing | Pollers select all unprocessed rows and publish asynchronously. Multiple instances can publish the same row before the callback marks it processed. | Use `SELECT ... FOR UPDATE SKIP LOCKED`, a claim/lease column, or a dedicated outbox relay with idempotent consumers. |
| H-02 | High | DLQ rows are marked processed before Kafka confirms delivery | `handleFailedEvent` calls `kafkaTemplate.send(...)` and immediately calls `event.markProcessed()`. A later async send failure loses the retryable outbox row. | Wait for the send result and mark processed only after acknowledgement. |
| H-03 | High | Payment has a database/Kafka dual-write gap | `PaymentServiceImpl` saves the payment and sends directly to Kafka without an outbox. A DB commit followed by Kafka failure leaves the order stuck. | Add a payment outbox or transactional Kafka publishing strategy. |
| H-04 | High | Payment idempotency helper is unused | `IdempotencyService` is not injected into `PaymentServiceImpl`; every order event can create a new payment record. | Require and persist an idempotency key/event ID and return the original result for duplicates. |
| H-05 | High | Saga compensation status can report success after failure | `SagaInstance.startCompensation` sets `COMPENSATING`, logs failed compensations, but leaves the status unchanged; line 130 then converts `COMPENSATING` to `COMPENSATED`. | Track a failed-compensation flag and transition to `FAILED` with manual intervention metadata. |
| H-06 | High | Transaction annotation does not cover async order work | `OrderServiceImpl.createOrder` starts `CompletableFuture.supplyAsync` inside a method annotated `@Transactional`; the database work runs on another thread after the caller transaction boundary. | Move the transactional unit into a separately proxied async service or use synchronous transactional work followed by an explicit async handoff. |
| H-07 | High | V2 status endpoint is fake | `OrderControllerV2.checkOrderStatus` always returns `PROCESSING`; the tracking future is never registered. | Store tracking state in durable storage and return actual terminal status. |
| H-08 | High | V2 cancellation does not cancel anything | `cancelOrder` returns `CANCELLED` and `refundStatus=PENDING` without changing the order or initiating a refund. | Implement state validation, persistence, inventory compensation, and refund workflow before exposing the endpoint. |
| H-09 | High | Product stock and inventory stock are competing stores | Product has `stockQuantity`, while inventory maintains available/reserved quantities. The two are not synchronized. | Choose inventory as the sole stock authority and remove or derive the product stock field. |
| H-10 | High | Product stock reduction accepts zero/negative quantities | `ProductController.reduceStock` accepts an unvalidated `int`; `reduceStock` subtracts it, so a negative quantity increases stock. | Add `@Positive` request validation and reject invalid values in the service too. |
| H-11 | High | Inventory initialization accepts invalid quantities | `initializeStock` accepts an unvalidated `Integer`; negative initial stock can be persisted. | Add `@Min(0)` and service-level validation. |
| H-12 | High | Notifications are not delivered | `EmailServiceImpl` only logs messages and has no SMTP client. The README's Gmail/SMTP claim is not true for current code. | Inject `JavaMailSender`, configure secrets, add retry/DLQ behavior, and test delivery safely. |
| H-13 | High | Payment is random simulation, not a payment provider | `simulatePaymentProcessing()` returns success based on `SecureRandom`; no Stripe code exists. | Isolate a provider adapter and integrate a test-mode provider with webhook/idempotency handling. |

### Medium and frontend findings

| ID | Severity | Finding | Evidence / consequence | Smallest useful correction |
| --- | --- | --- | --- | --- |
| M-01 | Medium | Frontend auth has two sources of truth | Axios reads `localStorage.token`; middleware reads a cookie. A token can exist in one and not the other, producing redirect/API mismatches. | Use a secure HttpOnly session cookie or make the client-only approach explicit and remove middleware dependence. |
| M-02 | Medium | Frontend auth cookie is JavaScript-readable | `Cookies.set("token", data.token, { expires: 7 })` omits `Secure`, `SameSite`, and HttpOnly is impossible from JS. | Set the session server-side in an HttpOnly cookie and keep the JWT out of local storage. |
| M-03 | Medium | `getCurrentUser` violates its return contract | `AuthService.getCurrentUser` is typed as `Promise<AuthResponse>` but directly returns parsed local storage or `null`, without token validation. | Return `Promise<AuthResponse | null>` and validate with a backend `/me` endpoint. |
| M-04 | Medium | Frontend lint gate is red | `npm run lint` reports 18 errors and 3 warnings, including explicit `any`, unescaped JSX punctuation, unused variables, and `setState` in an effect. | Fix lint issues instead of disabling rules; then rerun lint and build. |
| M-05 | Medium | Dashboard displays fabricated metrics | `frontend/src/app/dashboard/page.tsx:8-33` hard-codes revenue, order, sales, and user values. | Load metrics from a real endpoint and label empty states honestly. |
| M-06 | Medium | Order “View” action has no behavior | `frontend/src/features/orders/order-list.tsx:83-85` renders a button without a click handler or link. | Link to a detail route and load the selected order. |
| M-07 | Medium | V2 query parameters are ignored | `status` and `sort` are accepted by `OrderControllerV2` but never applied to the repository query. | Implement validated filters/sorting or remove the parameters. |
| M-08 | Medium | API versioning documentation is stale | V1 says it will sunset on June 1, 2025; the current date is October 26, 2026, and both V1 and V2 remain in source. | Decide a real deprecation policy and update code/docs together. |
| M-09 | Medium | Gateway rate limiter is process-local and spoofable | `SecurityConfig.RateLimitingFilter` stores counters in memory and trusts `X-Forwarded-For`. Scaling out bypasses limits; clients can spoof identity if no trusted proxy is enforced. | Use a distributed limiter and trusted proxy configuration. |
| M-10 | Medium | Gateway always adds HSTS | `SecurityHeadersFilter` emits HSTS even when local traffic is HTTP. Browsers can cache an inappropriate policy for local/test hosts. | Emit HSTS only behind HTTPS in a production profile. |
| M-11 | Medium | Outbox cleanup may delete evidence too early | Processed events are deleted after seven days by default, while failure investigation and replay requirements are not documented. | Define retention and archive/replay policy before cleanup. |
| M-12 | Medium | Shared and service-local event DTOs drift | `OrderCreatedEvent`, `PaymentProcessedEvent`, and `PaymentCompletedEvent` are duplicated in different packages with different fields. | Keep versioned event contracts in one shared module and forbid local duplicates. |

## Remediation roadmap

### Phase 1 — make the repository buildable

1. Restore the root Maven parent and module reactor.
2. Add POMs for every source module and pin compatible Spring Boot/Cloud versions.
3. Add service runtime configuration and a redacted `.env.example`.
4. Restore `docker-compose.yml` for PostgreSQL, Redis, Kafka, gateway, services, and frontend.
5. Make every Dockerfile build from a clean checkout.
6. Add CI jobs for Maven validation, frontend lint/build, and container builds.

### Phase 2 — repair contracts and authorization

1. Define one versioned event contract per topic.
2. Configure JSON serializers/deserializers and contract tests.
3. Implement owner-scoped order/payment reads.
4. Protect internal endpoints and strip/validate identity headers.
5. Replace client prices with server-side catalog pricing.
6. Make inventory the only stock authority.

### Phase 3 — implement the business workflow

1. Wire order creation to inventory reservation.
2. Persist saga state and reservation/payment IDs.
3. Add compensation for payment failure, timeout, cancellation, and notification failure policy.
4. Add payment outbox/idempotency and provider adapter.
5. Add real notification delivery with retries and a DLQ.
6. Make V2 tracking and cancellation real before documenting them as supported.

### Phase 4 — harden and observe

1. Replace process-local rate limiting with Redis or a managed gateway limiter.
2. Add outbox row claiming and replay tooling.
3. Add metrics for lag, retries, DLQ depth, payment outcomes, reservation failures, and saga state.
4. Add integration tests with PostgreSQL, Kafka, and Redis containers.
5. Add browser tests for login, middleware redirects, order list, and logout.
6. Run a security review before exposing any service outside the private network.

## Contribution guidance

- Read `AGENTS.md` before changing files.
- Keep secrets in local untracked configuration; never commit `.env` values.
- Run the narrowest relevant check first, then the full frontend/build checks when available.
- Do not claim backend build success until the Maven reactor and infrastructure are restored.
- Update this README when architecture, ports, event contracts, or run commands change.
- Add tests for every fixed bug, especially event compatibility, authorization, inventory accounting, idempotency, and async status behavior.

## License

No license file is currently tracked. Add an explicit license before distributing the project.
