# 🥗 AzərKalori — Microservices Edition (Eureka)

Azerbaijani food & calorie tracker: calorie measurement + **doctor-controlled diet plans**.

| Service | Port | Responsibility |
|---|---|---|
| discovery-server | 8761 | Eureka service registry |
| api-gateway | 8080 | Single entry point, JWT validation, routing (`lb://`) |
| auth-service | 8081 | Users, roles (USER/DOCTOR/ADMIN), JWT issuing, doctor↔patient assignment |
| catalog-service | 8082 | Product catalog, Redis cache, OpenFoodFacts + Resilience4j CB, GraphQL search |
| nutrition-service | 8084 / gRPC 9090 | BMR/TDEE gRPC engine, **diet plans (doctor-managed)** |
| tracking-service | 8083 | Food logs → daily summary → **WebSocket** live counter + doctor alerts |
| frontend | 80 | Nginx-served SPA + reverse proxy to the gateway |

Infra: PostgreSQL (schema-per-service) · Redis (catalog cache). Built with **Gradle** (JDK 17).
No Kafka — food logs are processed synchronously, so the whole stack fits a small 2 GB droplet.

## Roles

| Action | USER | DOCTOR | ADMIN |
|---|---|---|---|
| Register / login | ✅ | ✅ | ✅ |
| Log food, live WS counter | ✅ | — | — |
| View own summary & plan | ✅ | — | — |
| Create/edit diet plan for **own patients** | — | ✅ | — |
| View patient summaries, breach alerts | — | ✅ | — |
| Manage product catalog | — | — | ✅ |
| Create doctors, assign doctor↔patient | — | — | ✅ |

## Quick start (local)

```bash
cp .env.example .env         # set a strong JWT_SECRET
docker compose up -d --build # builds all 6 services + frontend
```

App (whole thing): http://localhost · Eureka: http://localhost:8761

Demo logins (seeded automatically):
`admin@azerkalori.az / admin123` · `doctor@azerkalori.az / doctor123` · `user@azerkalori.az / user123`

Build one service locally with Gradle (JDK 17): `cd auth-service && ./gradlew bootJar`

## Docs
- [docs/ROADMAP.md](docs/ROADMAP.md) — full 0→final plan (phases, DoD, timeline)
- [docs/DEPLOYMENT-DROPLET.md](docs/DEPLOYMENT-DROPLET.md) — DigitalOcean deployment
