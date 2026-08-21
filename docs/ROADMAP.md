# 🗺️ AzərKalori Microservices — Roadmap (0 → Final)

Goal: transform AzərKalori into a **Eureka-based microservices system** with role-based access
(USER / DOCTOR / ADMIN), calorie tracking + diet plan control, deployed on a DigitalOcean Droplet.

**Total duration: ~5 weeks (part-time).** Each phase ends with a "Definition of Done" (DoD) —
do not move on until it passes.

---

## 📊 Architecture Target

```
                        ┌──────────────────────┐
   Client ──────────────►   API GATEWAY :8080   │  (Spring Cloud Gateway + JWT filter)
                        └───────┬──────────────┘
                                │ (lb:// via Eureka)
        ┌───────────┬───────────┼────────────┬─────────────┐
        ▼           ▼           ▼            ▼             ▼
  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌───────────┐ ┌──────────────┐
  │  AUTH    │ │ CATALOG  │ │ TRACKING │ │ NUTRITION │ │  DISCOVERY   │
  │  :8081   │ │  :8082   │ │  :8083   │ │:8084/:9090│ │ EUREKA :8761 │
  │ JWT,users│ │ products │ │ logs, WS │ │ gRPC calc │ └──────────────┘
  │ roles    │ │ Redis,CB │ │ Kafka    │ │ diet plans│
  └────┬─────┘ └────┬─────┘ └───┬──┬───┘ └─────┬─────┘
       │            │           │  │           │
       ▼            ▼           ▼  └──WebSocket▼push to client
  ┌─────────────────────────┐ ┌───────┐ ┌────────────┐
  │       PostgreSQL        │ │ Kafka │ │   Redis    │
  │ (schema per service)    │ │(KRaft)│ │cache+dedup │
  └─────────────────────────┘ └───────┘ └────────────┘
```

### ❓ Is Kafka needed? → **YES, keep it**
Verdict: in a microservices setup Kafka is *more* justified than in the monolith:
1. `tracking-service` must update summaries + push WebSocket + notify doctors **without blocking** the `POST /api/logs` request.
2. Services are decoupled: tomorrow a `report-service` can consume the same `food-logs` topic with zero changes.
3. It's a required bootcamp Month-6 topic — removing it weakens the CV story.

What we do NOT need: Kafka between *every* pair of services. Synchronous needs (goal calculation, plan lookup) use gRPC/REST via Eureka. Kafka is used **only** for the food-log event stream.

---

## Phase 0 — Foundations (2–3 days)

**Learn / verify before writing code:**
- [ ] Spring Cloud basics: what Eureka solves (service registry + client-side load balancing)
- [ ] Spring Cloud Gateway: routes, predicates, filters, `lb://SERVICE-NAME` URIs
- [ ] Docker multi-stage builds; `docker compose` networking (service names = hostnames)
- [ ] Decide database strategy: **1 PostgreSQL container, separate schema per service**
      (`auth`, `catalog`, `tracking`, `nutrition`) — cheap, still enforces service boundaries
- [ ] Versions: Spring Boot 3.3.x + Spring Cloud 2023.0.x + Java 17

**DoD:** you can explain why the gateway calls `lb://AUTH-SERVICE` and not `http://localhost:8081`.

---

## Phase 1 — Discovery + Gateway skeleton (3–4 days)

1. Create `discovery-server` with `@EnableEurekaServer` (port 8761).
2. Create `api-gateway` with routes for the 4 future services (they can 404 for now).
3. Create empty `auth-service` registering into Eureka — see it appear in the Eureka dashboard.
4. Add Docker Compose with only: discovery + gateway + auth + postgres.

**DoD:** `curl http://localhost:8080/api/auth/ping` returns from auth-service *through the gateway*, and http://localhost:8761 shows both services UP.

---

## Phase 2 — Auth service: users, roles, JWT (4–5 days)

Roles model:

| Role | Can do |
|---|---|
| **USER** (patient) | register, log food, view own logs/summary, view own diet plan, get calorie goal |
| **DOCTOR** | view assigned patients, create/update diet plans for them, view patient summaries, receive limit-breach alerts |
| **ADMIN** | manage product catalog, create doctor accounts, assign doctor ↔ patient, manage users |

Tasks:
1. `User` entity: email, password (BCrypt), fullName, role, `doctorId` (nullable — assigned doctor for patients).
2. `POST /api/auth/register` (always role USER), `POST /api/auth/login` → JWT with `role` + `userId` claims.
3. Admin endpoints: `POST /api/auth/admin/doctors` (create doctor), `PUT /api/auth/admin/patients/{id}/doctor/{docId}` (assign).
4. **Gateway JWT filter**: validates token once at the edge, forwards `X-User-Id` and `X-User-Role` headers downstream. Downstream services trust these headers (they are unreachable from outside).
5. Unit tests: token generation/validation, role checks.

**DoD:** login as ADMIN → create a DOCTOR → register a USER → assign doctor to user. Requests without a token are rejected at the gateway.

---

## Phase 3 — Catalog service: products, Redis, Circuit Breaker (4–5 days)

1. `Product` entity + CRUD; write endpoints guarded by `X-User-Role == ADMIN`.
2. Redis cache for hot product lookups (24h TTL).
3. `POST /api/products/enrich` — barcode → OpenFoodFacts via Feign, wrapped in **Resilience4j CircuitBreaker**; fallback = manually entered data.
4. GraphQL endpoint: product search by name/category/kcal-range/macros (DataLoader against N+1).
5. Seed script: 150+ real Azerbaijani products.
6. Tests: WireMock for OpenFoodFacts (200 / timeout / 503), CB open-state test.

**DoD:** kill WireMock mid-test → circuit opens → fallback data returned; GraphQL filter query works through the gateway.

---

## Phase 4 — Nutrition service: gRPC engine + diet plans (5–6 days)

1. `nutrition.proto`: `CalculateGoalRequest{age, weightKg, heightCm, sex, activityLevel, goal}` → `CalculateGoalResponse{bmr, tdee, dailyCalories, proteinG, fatG, carbsG}`.
2. Embedded gRPC server on **:9090** — Mifflin-St Jeor BMR, activity multiplier TDEE, ±15% deficit/surplus.
3. REST `POST /api/goals/calculate` → internally calls the gRPC service (shows client+server usage).
4. **DietPlan** entity: `patientId, doctorId, dailyCalorieTarget, proteinG, fatG, carbsG, notes, startDate, endDate, active`.
   - `POST /api/plans` — DOCTOR only, and only for *their own* patients (verify via auth-service Feign call).
   - `GET /api/plans/my` — USER sees their active plan.
   - `GET /api/plans/patient/{id}` — DOCTOR (own patient) or ADMIN.
5. Unit tests: 8 BMR/TDEE body-type combinations; plan authorization rules.

**DoD:** a doctor creates a plan for their patient; another doctor gets 403; gRPC call visible in logs when calculating goals.

---

## Phase 5 — Tracking service: Kafka pipeline + WebSocket (5–6 days)

1. `POST /api/logs` → save `FoodLog` to PostgreSQL → publish `FoodLoggedEvent` to `food-logs` topic (**partition key = userId** → ordered per-user processing, no race conditions).
2. Consumer: updates `DailyNutritionSummary`, checks against the user's **active diet plan target** (cached from nutrition-service, Feign + CB fallback to TDEE goal).
3. Redis-based idempotency: processed event IDs, 24h TTL. Bad events → `food-logs-dlq`.
4. **WebSocket/STOMP** `/ws`: after each consumed event push `{calories, protein, fat, carbs, percentOfTarget}` to `/user/queue/calories`; 80% → WARN payload, 100% → LIMIT payload.
5. Doctor alert: on 100% breach, publish `limit-breached` event → push to doctor's channel `/user/queue/patients`.
6. Tests: Mockito for summary math; **TestContainers** (real Kafka + PostgreSQL) full pipeline test.

**DoD:** two rapid parallel `POST /api/logs` for the same user → summary is exact (no lost update); WebSocket client sees live counter; DLQ receives a poisoned message.

---

## Phase 6 — Hardening + integration tests (3–4 days)

- [ ] TestContainers end-to-end: register → login → log food → summary via GraphQL.
- [ ] Resilience4j on *inter-service* Feign calls (tracking→nutrition, nutrition→auth) with sane fallbacks.
- [ ] Actuator + `/actuator/health` for every service; gateway route to expose them (ADMIN only).
- [ ] Global exception handling, consistent error JSON.
- [ ] Postman/Bruno collection committed to the repo.

**DoD:** `./gradlew test` green on all services; stopping nutrition-service does NOT break food logging (fallback works).

---

## Phase 7 — Deployment to DigitalOcean Droplet (3–4 days)

See **docs/DEPLOYMENT-DROPLET.md** step by step. Summary:
- ⚠️ 6 JVMs + Kafka do **not** fit in 2 GB. Use the **$24/mo · 4 GB Droplet** (or trim: see doc).
- Multi-stage Dockerfiles, `-Xmx` capped per service, one `docker compose up -d`.
- UFW firewall (only 22/80/443 open), Nginx reverse proxy → gateway, optional Certbot TLS.
- Swap file (2 GB) as a safety net.
- Optional: GitHub Actions → build → `docker compose pull && up -d` over SSH.

**DoD:** `https://yourdomain/api/auth/login` works from your phone; Eureka dashboard reachable only via SSH tunnel.

---

## Phase 8 — Final polish / CV (2 days)

- README with architecture diagram, run instructions, screenshots (Eureka dashboard, WebSocket demo).
- Updated CV blurb:

> *"Built AzərKalori, a microservices nutrition platform (Spring Cloud, Eureka, API Gateway): role-based access for patients, doctors and admins; doctor-managed diet plans; event-driven meal logging via Kafka with per-user partition ordering; real-time WebSocket calorie counter; gRPC nutrition engine; GraphQL catalog; Resilience4j fault tolerance across services. Deployed on DigitalOcean with Docker Compose."*

---

## 📅 Timeline overview

| Week | Phases |
|---|---|
| 1 | Phase 0 + 1 (Eureka, Gateway) |
| 2 | Phase 2 (Auth/roles) + start Phase 3 |
| 3 | Phase 3 (Catalog) + Phase 4 (Nutrition/plans) |
| 4 | Phase 5 (Kafka/WebSocket) |
| 5 | Phase 6 + 7 + 8 (tests, deploy, polish) |
