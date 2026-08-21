# 🥗 AzərKalori — Final Project Guide / Yekun Layihə Bələdçisi

Bilingual documentation (English + Azərbaycan dili) for the **final, clean version**:
no Kafka, all issues fixed, full frontend, one-command deploy.

İkidilli sənəd (İngilis + Azərbaycan) — **yekun, təmiz versiya** üçün:
Kafka yoxdur, bütün problemlər həll olunub, tam frontend, bir əmrlə deploy.

---

# PART 1 — WHAT IT IS & WHY / NƏDİR VƏ NİYƏ

## 🇬🇧 English
**AzərKalori** is an Azerbaijani food & calorie platform built as a **Spring Cloud microservices system**. Unlike a plain calorie counter, it adds **doctor-controlled diet plans** and three roles:

- **USER (patient)** — registers, calculates their calorie goal, logs food, watches a **live calorie counter**, sees their diet plan.
- **DOCTOR** — sees only their assigned patients, creates diet plans, and gets **real-time alerts** when a patient exceeds their limit.
- **ADMIN** — manages the product catalog, creates doctors, assigns a doctor to each patient.

**Why microservices?** Each concern (identity, catalog, nutrition, tracking) is a separate service that scales, deploys, and fails independently, discovered through **Eureka** and reached through one **API Gateway**.

**Why no Kafka?** The earlier design used Kafka to process food logs asynchronously. For a low-cost DigitalOcean droplet, Kafka is ~300 MB of RAM and a whole extra moving part that this app does not need — a food log is processed **synchronously** in one quick request (save → update summary → push over WebSocket). Removing it makes the app **cheaper, simpler, and easier to deploy**, while keeping every headline feature (real-time counter, doctor alerts, gRPC, GraphQL, Redis cache, circuit breakers).

## 🇦🇿 Azərbaycan dili
**AzərKalori** — **Spring Cloud mikroservis sistemi** kimi qurulmuş Azərbaycan qida və kalori platformasıdır. Adi kalori sayğacından fərqli olaraq, **həkim tərəfindən idarə olunan pəhriz planları** və üç rol əlavə edir:

- **USER (pasiyent)** — qeydiyyatdan keçir, kalori məqsədini hesablayır, qida qeyd edir, **canlı kalori sayğacına** baxır, pəhriz planını görür.
- **DOCTOR (həkim)** — yalnız təyin olunmuş pasiyentlərini görür, pəhriz planları yaradır və pasiyent limiti keçəndə **real-time xəbərdarlıq** alır.
- **ADMIN** — məhsul kataloqunu idarə edir, həkim yaradır, hər pasiyentə həkim təyin edir.

**Niyə mikroservis?** Hər sahə (kimlik, kataloq, qidalanma, izləmə) ayrıca servisdir; **Eureka** ilə tapılır və bir **API Gateway** vasitəsilə əlçatandır.

**Niyə Kafka yoxdur?** Əvvəlki dizayn qida qeydlərini asinxron emal etmək üçün Kafka istifadə edirdi. Ucuz DigitalOcean droplet üçün Kafka ~300 MB RAM və bu tətbiqə lazım olmayan əlavə komponentdir — qida qeydi bir sürətli sorğuda **sinxron** emal olunur (saxla → xülasəni yenilə → WebSocket ilə göndər). Onu silmək tətbiqi **ucuz, sadə və deploy üçün asan** edir, bütün əsas funksiyaları saxlayaraq.

---

# PART 2 — HOW IT WORKS, STEP BY STEP / NECƏ İŞLƏYİR, ADDIM-ADDIM

## 🇬🇧 The three journeys

**A. Public visitor (no login) — the calorie calculator**
1. Opens the site → the landing page shows a calculator (age, sex, height, weight, activity, goal).
2. Clicks **Hesabla** → the browser POSTs to `/api/goals/calculate`.
3. The gateway sees this path is **public**, forwards it to nutrition-service.
4. nutrition-service’s REST controller calls its **own embedded gRPC engine** → returns BMR, TDEE, daily calories, and macros. The page draws the result with macro bars.

**B. Patient (USER)**
1. **Register/Login** (`/api/auth/register` or `/login`) → receives a **JWT**.
2. Opens **Panelim (Dashboard)**. The browser opens a **WebSocket** to `/ws`, sending the JWT; tracking-service validates it and remembers "this socket = user 3".
3. Patient picks a product + grams → **Jurnala əlavə et** → POST `/api/logs`.
4. tracking-service: fetches the product from catalog-service (Feign), computes macros for that gram amount, saves the `FoodLog`, updates today’s `DailySummary`, then **pushes the new totals over the WebSocket**. The ring counter updates live (green → amber at 80 % → red at 100 %).
5. Patient sees today’s logs and their doctor’s **diet plan** (`/api/plans/my`).

**C. Doctor**
1. Logs in as DOCTOR → **Həkim** panel lists their patients (`/api/auth/doctor/patients`).
2. Fills the plan form → POST `/api/plans`. nutrition-service verifies (via auth-service) that this patient really belongs to this doctor, deactivates any old plan, saves the new one.
3. The doctor’s panel keeps a WebSocket open to `/user/queue/alerts`. When any of their patients crosses 100 %, tracking-service pushes a **breach alert** and it pops up in real time.

**D. Admin**
1. Logs in as ADMIN → **Admin** panel.
2. Adds products to the catalog (`POST /api/products`), creates doctors (`/api/auth/admin/doctors`), and assigns a doctor to a patient (`PUT /api/auth/admin/patients/{id}/doctor/{docId}`).

**The security model:** the **gateway validates the JWT once**, then injects trusted `X-User-Id` and `X-User-Role` headers. Internal services are never exposed to the internet, so they trust those headers. The WebSocket is authenticated separately at the STOMP CONNECT frame using the same JWT.

## 🇦🇿 Üç ssenari

**A. Qonaq (girişsiz) — kalori kalkulyatoru**
1. Sayta girir → açılış səhifəsində kalkulyator var (yaş, cins, boy, çəki, aktivlik, məqsəd).
2. **Hesabla** düyməsi → brauzer `/api/goals/calculate`-ə POST göndərir.
3. Gateway bu yolun **açıq** olduğunu görür, nutrition-service-ə ötürür.
4. nutrition-service **öz daxili gRPC mühərrikini** çağırır → BMR, TDEE, gündəlik kalori və makroları qaytarır. Səhifə nəticəni makro zolaqları ilə çəkir.

**B. Pasiyent (USER)**
1. **Qeydiyyat/Giriş** → **JWT** alır.
2. **Panelim**-i açır. Brauzer `/ws`-ə **WebSocket** açır və JWT göndərir; tracking-service onu yoxlayır və "bu soket = user 3" yadda saxlayır.
3. Məhsul + qram seçir → **Jurnala əlavə et** → POST `/api/logs`.
4. tracking-service: məhsulu catalog-service-dən alır (Feign), makroları hesablayır, `FoodLog`-u saxlayır, bugünkü `DailySummary`-ni yeniləyir və yeni cəmləri **WebSocket ilə göndərir**. Halqa sayğacı canlı yenilənir (yaşıl → 80%-də narıncı → 100%-də qırmızı).
5. Pasiyent bugünkü qeydlərini və həkiminin **pəhriz planını** görür.

**C. Həkim**
1. DOCTOR kimi girir → **Həkim** paneli pasiyentlərini sayır.
2. Plan formasını doldurur → POST `/api/plans`. nutrition-service (auth-service vasitəsilə) pasiyentin həqiqətən bu həkimə aid olduğunu yoxlayır, köhnə planı deaktiv edir, yenisini saxlayır.
3. Həkim paneli `/user/queue/alerts`-ə WebSocket saxlayır. Pasiyent 100%-i keçəndə tracking-service **xəbərdarlıq** göndərir və real-time görünür.

**D. Admin**
1. ADMIN kimi girir → **Admin** paneli.
2. Kataloqa məhsul əlavə edir, həkim yaradır, pasiyentə həkim təyin edir.

**Təhlükəsizlik modeli:** **gateway JWT-ni bir dəfə yoxlayır**, sonra etibarlı `X-User-Id` və `X-User-Role` başlıqlarını əlavə edir. Daxili servislər internetə açıq deyil, ona görə həmin başlıqlara etibar edir. WebSocket ayrıca STOMP CONNECT mərhələsində eyni JWT ilə autentifikasiya olunur.

---

# PART 3 — ARCHITECTURE / ARXİTEKTURA

```
                 Browser (frontend SPA, served by Nginx :80)
                        │  /api/*  /ws  /graphql   (Nginx reverse proxy)
                        ▼
                 API GATEWAY :8080   ── validates JWT once, adds X-User-Id / X-User-Role
                        │  lb:// via Eureka
   ┌──────────┬─────────┼───────────┬─────────────┐
   ▼          ▼         ▼           ▼             ▼
 AUTH       CATALOG   TRACKING    NUTRITION    DISCOVERY (Eureka :8761)
 :8081      :8082     :8083       :8084/:9090
 users,     products, food logs,  gRPC calc,
 roles,     Redis,    summary,    diet plans
 JWT        GraphQL   WebSocket
   │          │         │            │
   └──────────┴────┬────┴────────────┘
                   ▼
             PostgreSQL (schema per service)   +   Redis (catalog cache)
```

| Service | Port | Responsibility |
|---|---|---|
| discovery-server | 8761 | Eureka registry |
| api-gateway | 8080 | Single entry, JWT validation, routing |
| auth-service | 8081 | Users, roles, JWT, doctor↔patient |
| catalog-service | 8082 | Products, Redis cache, GraphQL, OpenFoodFacts + circuit breaker |
| nutrition-service | 8084 / gRPC 9090 | BMR/TDEE gRPC engine, diet plans |
| tracking-service | 8083 | Food logs → summary → WebSocket live counter + doctor alerts |
| frontend | 80 | Nginx serving the SPA + reverse-proxy to the gateway |
| postgres / redis | — | One DB (schema per service) · Redis cache |

---

# PART 4 — BACKEND FROM ZERO TO END / BACKEND SIFIRDAN SONA

Read this top-to-bottom to understand every class. / Hər sinifi anlamaq üçün baştan-sona oxu.

## 4.0 Shared infra files / Ümumi infrastruktur

- **`docker-compose.yml`** — 🇬🇧 Orchestrates postgres, redis, the 6 services (capped `-Xmx`), and the Nginx `frontend`. Only ports 80 (frontend) is public; 8080/8761 bound to localhost. 🇦🇿 postgres, redis, 6 servis və Nginx frontend-i idarə edir; yalnız 80 portu açıqdır.
- **`init-schemas.sql`** — 🇬🇧 Creates the `auth`, `catalog`, `nutrition`, `tracking` schemas on first DB boot. 🇦🇿 İlk açılışda 4 schema yaradır.
- **`.env` / `.env.example`** — 🇬🇧 `POSTGRES_*` and a strong `JWT_SECRET` (shared by gateway, auth, tracking). 🇦🇿 DB açarları və güclü `JWT_SECRET`.
- Each service has: **`build.gradle`** (dependencies), **`settings.gradle`** (project name), a committed **Gradle wrapper** (`gradlew`, pinned to 8.10.2), a **`Dockerfile`** (Gradle build → Corretto 17 run), and **`application.yml`**.

## 4.1 discovery-server (Eureka, :8761)
- **`DiscoveryServerApplication`** — 🇬🇧 `@EnableEurekaServer` turns this app into the service registry every other service registers into. 🇦🇿 `@EnableEurekaServer` bu tətbiqi servis registrinə çevirir.
- **`application.yml`** — 🇬🇧 Port 8761; it does not register with itself. 🇦🇿 Port 8761; özü-özünə qeydiyyatdan keçmir.

## 4.2 api-gateway (:8080)
- **`GatewayApplication`** — 🇬🇧 Boot entry; routes are in YAML. 🇦🇿 Giriş; marşrutlar YAML-dadır.
- **`filter/JwtAuthFilter`** — 🇬🇧 A `GlobalFilter` (order −1). Public paths — `/api/auth/login`, `/register`, `/ping`, **`/api/goals/calculate`** (public calculator), **`/ws`** (WebSocket handshake) — pass through. Everything else must carry `Authorization: Bearer <jwt>`; it verifies the HMAC signature, extracts `subject` (user id) + `role`, and injects `X-User-Id` / `X-User-Role`. Bad/missing token → 401. 🇦🇿 `GlobalFilter` (order −1). Açıq yollar keçir; qalanları `Bearer` token tələb edir, imzanı yoxlayır, `X-User-Id`/`X-User-Role` əlavə edir. Yanlış token → 401.
- **`application.yml`** — 🇬🇧 4 routes: auth, catalog (`/api/products/**`, `/graphql/**`), tracking (`/api/logs/**`, `/api/summary/**`, `/ws/**`), nutrition (`/api/goals/**`, `/api/plans/**`), all `lb://SERVICE-NAME`. 🇦🇿 4 marşrut, hamısı `lb://` ilə.

## 4.3 auth-service (:8081)
- **`AuthServiceApplication`** — boot entry.
- **`entity/Role`** — 🇬🇧 enum `USER, DOCTOR, ADMIN`. 🇦🇿 enum.
- **`entity/User`** — 🇬🇧 JPA `users`: email (unique), password (BCrypt, **`@JsonProperty(WRITE_ONLY)`** so it never leaks in JSON — *fixed issue*), fullName, role, `doctorId`, and profile (age/weight/height/sex/activity). 🇦🇿 `users` entity; parol **WRITE_ONLY** (JSON-da sızmır — *düzəldilmiş problem*).
- **`repo/UserRepository`** — 🇬🇧 `findByEmail`, `findByDoctorId`, `findByRole`. 🇦🇿 email/doctorId/rol üzrə axtarış.
- **`security/JwtService`** — 🇬🇧 Issues a signed JWT: subject = user id, claims `role` + `email`, 12 h expiry. 🇦🇿 İmzalı JWT verir (id, role, email; 12 saat).
- **`security/SecurityConfig`** — 🇬🇧 BCrypt bean + stateless permit-all chain (the gateway already authenticated). 🇦🇿 BCrypt + stateless zəncir.
- **`web/AuthController`** — 🇬🇧 `ping`; `register` (always USER); `login`; `me` (own profile from `X-User-Id`); admin: `admin/doctors`, `admin/users`, `admin/doctors` (list), `admin/patients/{id}/doctor/{docId}`; doctor: `doctor/patients`; internal: `internal/users/{id}` (used by nutrition). `requireRole` guards each. 🇦🇿 qeydiyyat/giriş/profil + admin və həkim əməliyyatları; `requireRole` qoruyur.
- **`config/DataSeeder`** — 🇬🇧 On an empty DB seeds an **admin**, a **doctor**, and a **demo patient** (assigned to that doctor), so the app is usable immediately. 🇦🇿 Boş DB-də admin, həkim və demo pasiyent yaradır.

## 4.4 catalog-service (:8082)
- **`CatalogServiceApplication`** — `@EnableCaching`, `@EnableFeignClients`.
- **`entity/Product`** — 🇬🇧 `products`: name, brand, category, barcode, per-100 g macros, `enriched`; `Serializable` for Redis caching. 🇦🇿 `products`; Redis üçün `Serializable`.
- **`repo/ProductRepository`** — 🇬🇧 JPA + `JpaSpecificationExecutor` (for GraphQL) + `findByBarcode`. 🇦🇿 dinamik sorğular + barkod.
- **`client/OpenFoodFactsClient`** — 🇬🇧 Feign to OpenFoodFacts. 🇦🇿 Feign müştəri.
- **`client/EnrichmentService`** — 🇬🇧 `@CircuitBreaker` on the OpenFoodFacts call; if it fails, `fallback()` keeps manual values. Correctly placed in its own bean so the proxy works. 🇦🇿 `@CircuitBreaker`; uğursuzluqda əl ilə daxil edilmiş dəyərlər.
- **`web/ProductController`** — 🇬🇧 `GET /` list, `GET /{id}` (`@Cacheable` in Redis), `POST /` (ADMIN), `POST /{id}/enrich` (ADMIN). 🇦🇿 siyahı, keşli oxu, yaratma, zənginləşdirmə.
- **`web/ProductGraphQL`** — 🇬🇧 `searchProducts(name, category, min/maxCalories, minProtein)` builds a JPA `Specification` dynamically. 🇦🇿 dinamik GraphQL axtarışı.
- **`config/DataSeeder`** — 🇬🇧 Seeds ~25 real Azerbaijani foods (Plov, Dolma, Paxlava…) on an empty catalog. 🇦🇿 ~25 Azərbaycan qidası əlavə edir.
- **`resources/graphql/schema.graphqls`** — GraphQL schema.

## 4.5 nutrition-service (:8084 / gRPC :9090)
- **`NutritionServiceApplication`** — `@EnableFeignClients`.
- **`proto/nutrition.proto`** — 🇬🇧 gRPC contract; the Gradle `com.google.protobuf` plugin generates Java + gRPC stubs at build time. 🇦🇿 gRPC müqaviləsi; Gradle plugin stub-lar yaradır.
- **`grpc/NutritionGrpcService`** — 🇬🇧 The embedded gRPC **server** (`@GrpcService`, :9090). BMR (Mifflin-St Jeor), TDEE (activity ×), goal ±15 %, macros. 🇦🇿 Daxili gRPC serveri; BMR/TDEE/makro.
- **`web/GoalController`** — 🇬🇧 REST facade that is also a gRPC **client** (`@GrpcClient`); `POST /api/goals/calculate`. 🇦🇿 gRPC müştərisi olan REST fasadı.
- **`entity/DietPlan`** — 🇬🇧 `diet_plans`: patientId, doctorId, dailyCalorieTarget, macros, notes, dates, `active`. 🇦🇿 pəhriz planı entity-si.
- **`repo/DietPlanRepository`** — `findByPatientIdAndActiveTrue`, `findByDoctorId`.
- **`web/AuthClient`** — 🇬🇧 Feign to auth-service to verify doctor↔patient. 🇦🇿 həkim↔pasiyent yoxlaması.
- **`web/DietPlanController`** — 🇬🇧 `POST /` (DOCTOR, own patient only; deactivates old plan); `GET /my` (USER); `GET /patient/{id}` (DOCTOR/ADMIN, and used internally by tracking to read target + doctorId); `GET /mine-as-doctor`. 🇦🇿 plan yaratma və oxuma.
- **`config/DataSeeder`** — 🇬🇧 Seeds one active plan for the demo patient so the live counter has a target on first login. 🇦🇿 demo pasiyent üçün bir aktiv plan.

## 4.6 tracking-service (:8083) — Kafka removed, synchronous
- **`TrackingServiceApplication`** — `@EnableFeignClients`.
- **`entity/FoodLog`** — 🇬🇧 `food_logs`: userId, productId, product-name snapshot, grams, macros, dates. 🇦🇿 qida qeydi entity-si.
- **`entity/DailySummary`** — 🇬🇧 `daily_summaries` (unique userId+day): running totals + `targetCalories` + **`doctorId`** (so a breach can alert the doctor without another lookup). 🇦🇿 gündəlik xülasə + hədəf + `doctorId`.
- **`repo/FoodLogRepository`, `repo/DailySummaryRepository`** — 🇬🇧 lookups by user/date. 🇦🇿 istifadəçi/tarix axtarışı.
- **`web/CatalogClient`, `web/NutritionClient`** — 🇬🇧 Feign clients (product lookup; active plan). 🇦🇿 Feign müştərilər.
- **`service/NutritionPlanClient`** — 🇬🇧 Wraps the nutrition Feign call with `@CircuitBreaker` **in its own bean** so the proxy actually applies (*fixed issue*). If nutrition is down, `fallback()` returns an empty map → food logging still works. 🇦🇿 `@CircuitBreaker` ayrı bean-də (*düzəldilmiş problem*); nutrition sönsə belə qeyd işləyir.
- **`service/SummaryService`** — 🇬🇧 The heart: `apply()` loads/creates today’s summary (fetching target + doctorId on first log), adds the macros, saves, then `push()` sends the live totals to `/user/{id}/queue/calories` and — on ≥100 % — a breach to the doctor’s `/user/{doctorId}/queue/alerts`. 🇦🇿 Ürək: xülasəni yenilə, WebSocket ilə göndər, limit keçiləndə həkimə xəbərdarlıq.
- **`web/FoodLogController`** — 🇬🇧 `POST /api/logs` (fetch product → build FoodLog → save → `summaryService.apply`); `GET /today`; `GET /patient/{id}/today` (DOCTOR/ADMIN). 🇦🇿 qida qeydi + siyahı.
- **`web/SummaryController`** — 🇬🇧 `GET /api/summary/today` (*fixed issue — endpoint now exists*) and `/patient/{id}/today`. 🇦🇿 xülasə REST son-nöqtəsi (*düzəldilmiş problem*).
- **`ws/WebSocketConfig`** — 🇬🇧 STOMP `/ws` (SockJS), broker `/queue`+`/topic`, user prefix `/user`, and registers the JWT interceptor. 🇦🇿 STOMP konfiqurasiyası + JWT interceptor.
- **`ws/JwtChannelInterceptor`** — 🇬🇧 On STOMP CONNECT it reads the `Authorization` header, verifies the JWT, and **sets the session Principal to the user id** — this is what makes `/user/...` messages actually reach the right browser (*fixed issue: the live counter now works*). 🇦🇿 CONNECT-də JWT-ni yoxlayır və **Principal-ı user id-yə təyin edir** — canlı sayğacın işləməsinin səbəbi (*düzəldilmiş problem*).

---

# PART 5 — WHAT WAS FIXED / NƏ DÜZƏLDİLDİ

| # | Issue | Fix |
|---|---|---|
| 1 | Local build failed on JDK 25 | You now use **Corretto 17**; wrapper pinned to Gradle 8.10.2; Docker uses JDK 17. |
| 2 | WebSocket live counter never delivered | New **`JwtChannelInterceptor`** sets the STOMP Principal → `/user/queue/*` now routes correctly. |
| 3 | Food logging broke when nutrition was down | Circuit breaker moved into its own bean **`NutritionPlanClient`**; fallback works, logging never fails. |
| 4 | `/api/summary` had no handler | New **`SummaryController`** (`/today`, `/patient/{id}/today`). |
| 5 | Password hash leaked in JSON | `password` marked **`@JsonProperty(WRITE_ONLY)`**. |
| 6 | Doctor breach alerts never delivered | Now pushed **synchronously** to `/user/{doctorId}/queue/alerts` in `SummaryService`. |
| + | Kafka cost/complexity | **Removed** — synchronous processing; ~300 MB RAM saved, cheaper droplet. |

---

# PART 6 — FRONTEND / FRONTEND

🇬🇧 A single polished SPA (vanilla HTML/CSS/JS, fresh green health theme inspired by diyetkolik) served by **Nginx**, which also reverse-proxies `/api`, `/ws`, `/graphql` to the gateway. Views: public **calorie calculator**, **auth** (login/register), **USER dashboard** (live ring counter, food logging, today’s logs, diet plan), **DOCTOR** (patients, create plan, live alerts), **ADMIN** (add product, create doctor, assign doctor). WebSocket uses vendored SockJS + STOMP (`frontend/js/vendor/`), so no CDN is needed at runtime.

🇦🇿 Nginx ilə verilən tək, səliqəli SPA (sadə HTML/CSS/JS, diyetkolik ruhunda yaşıl tema); `/api`, `/ws`, `/graphql`-i gateway-ə ötürür. Görünüşlər: açıq **kalkulyator**, **giriş/qeydiyyat**, **USER paneli** (canlı halqa sayğac, qida qeydi, plan), **DOCTOR** (pasiyentlər, plan, canlı xəbərdarlıq), **ADMIN**. WebSocket üçün SockJS + STOMP lokal saxlanılıb — CDN lazım deyil.

---

# PART 7 — RUN & DEPLOY / İŞƏ SAL VƏ DEPLOY

## Local (one command) / Lokal (bir əmr)
```bash
cp .env.example .env         # set a strong JWT_SECRET (openssl rand -base64 48)
docker compose up -d --build
```
Open **http://localhost** → the whole app. Eureka (admin) via SSH-tunnel/localhost:8761.

**Demo logins:** `admin@azerkalori.az / admin123` · `doctor@azerkalori.az / doctor123` · `user@azerkalori.az / user123`

## Build one service locally with Gradle (Corretto 17) / Bir servisi lokal qur
```bash
export JAVA_HOME=/Users/vusalbilalov/Library/Java/JavaVirtualMachines/corretto-17.0.19/Contents/Home
cd tracking-service && ./gradlew bootJar
```

## DigitalOcean Droplet / DigitalOcean-da deploy
🇬🇧 A **2 GB droplet ($12/mo)** is now enough (Kafka gone). Steps:
1. Create an Ubuntu 24.04 droplet, add your SSH key.
2. `ufw allow OpenSSH && ufw allow 80 && ufw allow 443 && ufw enable`.
3. Add a 2 GB swap file (JVM safety net).
4. Install Docker (`curl -fsSL https://get.docker.com | sudo sh`).
5. `git clone` the repo, `cp .env.example .env`, set `JWT_SECRET` + DB password.
6. `docker compose up -d --build` → the app is live on port **80**.
7. (Optional) point a domain at the droplet and run `certbot` for HTTPS.

🇦🇿 İndi **2 GB droplet ($12/ay)** kifayətdir (Kafka getdi). Addımlar: Ubuntu 24.04 droplet yarat, SSH açar əlavə et; `ufw` ilə yalnız 22/80/443 aç; 2 GB swap; Docker qur; repo-nu klonla, `.env`-i doldur; `docker compose up -d --build` → tətbiq **80** portunda canlıdır; istəyə görə domen + `certbot` ilə HTTPS.

*All 6 services compile on Corretto 17, `docker compose build` succeeds, and the stack runs end-to-end.*
*Bütün 6 servis Corretto 17-də kompilyasiya olunur, `docker compose build` uğurludur və sistem baştan-sona işləyir.*
