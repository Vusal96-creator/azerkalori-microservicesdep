# AzərKalori (Kalori Kolik) - Müdafiə Dərsliyi

> Bu sənəd layihənin müdafiəsinə hazırlıq üçündür. Hər bölmə həm **sadə dildə** (Feynman qaydası — 10 yaşındakı uşağa izah edir kimi), həm də **texniki dildə** (backend developer üçün) yazılıb.

---

## 📋 MÜNDƏRİCAT

1. [Layihə haqqında ümumi məlumat](#1-layihə-haqqında)
2. [Arxitektura (Microservices)](#2-arxitektura)
3. [Texnologiyalar](#3-texnologiyalar)
4. [Verilənlər bazası (PostgreSQL)](#4-verilənlər-bazası)
5. [Discovery Server (Eureka)](#5-discovery-server)
6. [API Gateway](#6-api-gateway)
7. [Auth Service](#7-auth-service)
8. [Catalog Service](#8-catalog-service)
9. [Nutrition Service](#9-nutrition-service)
10. [Tracking Service](#10-tracking-service)
11. [Frontend](#11-frontend)
12. [Docker və Docker Compose](#12-docker)
13. [n8n + AI Chatbot (RAG)](#13-n8n)
14. [DNS və Domain](#14-dns)
15. [Deployment (DigitalOcean)](#15-deployment)
16. [CI/CD (GitHub Actions)](#16-cicd)
17. [Test yazma](#17-testlər)
18. [Tez-tez verilən suallar](#18-suallar)

---

## 1. LAYİHƏ HAQQINDA

### Sadə dildə
AzərKalori — insanların gündəlik yediklərini qeyd edib, aldıqları kaloriləri hesablayan və sağlam həyat tərzi qurmasına kömək edən veb tətbiqidir. Sanki elektron bir gündəlik: nə yedin? nə qədər kalori aldın? hədəfə çatdın mı? Həmçinin süni intellekt köməkçisi var — qida haqqında sual verirsən, cavab alırsan.

### Texniki dildə
Spring Boot microservice arxitekturasında qurulmuş full-stack tətbiq. 6 mikroservis (auth, catalog, nutrition, tracking, api-gateway, discovery-server), PostgreSQL (schema-per-service), Redis cache, WebSocket (STOMP) real-time bildirişlər, JWT auth, Stripe billing, gRPC inter-service communication, n8n + Qdrant + Groq üzərində RAG chatbot.

### İstifadəçi rolları
- **USER**: adi istifadəçi — qida qeyd edir, hesabatlarını görür
- **DOCTOR**: həkim — xəstələrin hesabatlarını görür, alertlər alır
- **ADMIN**: administrator — həkimləri təsdiqləyir, istifadəçiləri idarə edir

---

## 2. ARXITEKTURA (MICROSERVICES)

### Sadə dildə
Böyük bir binanı təsəvvür et: bir mərtəbədə mətbəx, bir mərtəbədə yataq otağı, birində vanna. Hər biri öz işini görür, amma bir bina təşkil edirlər. Microservices də belədir — bir böyük tətbiqi kiçik parçalara bölürsən, hər biri öz vəzifəsini icra edir. Biri sırf istifadəçi girişini idarə edir, digəri qidaların siyahısını saxlayır, üçüncüsü isə gündəlik hesabatı yığır.

### Texniki dildə
Layihə **microservices arxitekturası** üzərində qurulub. Hər servis:
- Öz DB schema-sına malikdir (schema-per-service pattern)
- Müstəqil deploy oluna bilir
- Eureka Service Discovery ilə bir-birini tapır
- Fault isolation: bir servis çökəndə digərləri işləməyə davam edir

### Servislər arası əlaqə diaqramı
```
                    ┌──────────────┐
                    │   FRONTEND   │  (Nginx :80)
                    └──────┬───────┘
                           │ HTTP + WebSocket
                           ▼
                    ┌──────────────┐
                    │ API Gateway  │  (:8080) - JWT yoxlaması
                    └──────┬───────┘
                           │
        ┌──────────────────┼──────────────────────┐
        ▼                  ▼                      ▼
   ┌─────────┐        ┌─────────┐            ┌─────────┐
   │  Auth   │        │ Catalog │            │Tracking │
   │  :8081  │        │  :8082  │            │  :8083  │
   └────┬────┘        └────┬────┘            └────┬────┘
        │                  │  ┌─────────┐         │
        │                  │  │Nutrition│◄────────┤
        │                  │  │  :8084  │(gRPC)   │
        │                  │  └────┬────┘         │
        │                  │       │              │
        └──────────────────┴───────┴──────────────┘
                           ▼
                    ┌──────────────┐
                    │ PostgreSQL   │  (4 schema: auth/catalog/nutrition/tracking)
                    │   :5440      │
                    └──────────────┘

    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
    │Discovery-8761│    │  Redis-6379  │    │  n8n+Qdrant  │
    │  (Eureka)    │    │  (Cache)     │    │(AI Chatbot)  │
    └──────────────┘    └──────────────┘    └──────────────┘
```

### Niyə microservices?
- **Ayrı komandalar** ayrı servislərdə paralel işləyə bilər
- Bir servisdə problem varsa **digərləri işləyir**
- Hər servisin **öz texnologiyası** ola bilər (biri Java, digəri Node.js və s.)
- **Scale**: yalnız yüklü servisi çoxaltmaq olar (məs. 5 tracking-service instance)

---

## 3. TEXNOLOGIYALAR

| Layer | Texnologiya | Nə üçün? |
|-------|-------------|----------|
| Backend | **Java 17 + Spring Boot 3** | Enterprise-grade, geniş community |
| Build | **Gradle (multi-module)** | Bütün servisləri bir yerdən build |
| DB | **PostgreSQL 16** | ACID, güclü SQL, geniş dəstək |
| Cache | **Redis 7** | Sürətli in-memory cache |
| Discovery | **Netflix Eureka** | Servislər bir-birini tapır |
| Gateway | **Spring Cloud Gateway** | Reactive, yüngül |
| Auth | **JWT (JSON Web Token)** | Stateless, scale-friendly |
| Real-time | **WebSocket (STOMP + SockJS)** | Canlı bildirişlər |
| RPC | **gRPC + Protocol Buffers** | Sürətli inter-service |
| Payment | **Stripe** | Test rejimində |
| Email | **Spring Mail (Gmail SMTP)** | Xoş gəldin məktubu |
| AI | **n8n + Qdrant + Groq (Llama 3.3)** | Chatbot RAG |
| Frontend | **Vanilla HTML/CSS/JS** | Sadə, çevik |
| Web server | **Nginx** | Frontend serve edir |
| HTTPS | **Caddy** | Avtomatik Let's Encrypt |
| Containerization | **Docker + Docker Compose** | Deployment |
| Cloud | **DigitalOcean Droplet** | Ubuntu 24.04, 4GB RAM |
| CI/CD | **GitHub Actions** | Auto build+test+deploy |

---

## 4. VERİLƏNLƏR BAZASI

### Sadə dildə
Bir kitabxananı təsəvvür et — kitabxanada dörd otaq var: birində istifadəçi kartları, birində qida siyahısı, birində qidalanma planları, birində gündəlik qeydlər. Hər otaq öz məsuliyyət sahəsindəki məlumatı saxlayır. PostgreSQL bizim üçün bu kitabxanadır, "otaqlar" isə **schema** adlanır.

### Texniki dildə
Tək PostgreSQL instance, **4 schema** (schema-per-service pattern):

**`init-schemas.sql`**:
```sql
CREATE SCHEMA IF NOT EXISTS auth;
CREATE SCHEMA IF NOT EXISTS catalog;
CREATE SCHEMA IF NOT EXISTS nutrition;
CREATE SCHEMA IF NOT EXISTS tracking;
```

### Cədvəllər
- **`auth.users`** — istifadəçilər (email, password, role, doctorId, pro)
- **`catalog.products`** — qidalar (name, calories, protein, fat, carbs)
- **`nutrition.diet_plans`** — qidalanma planları
- **`tracking.food_logs`** — gündəlik qida qeydləri
- **`tracking.daily_summaries`** — gündəlik ümumi hesabat
- **`tracking.alerts`** — həkimə göndərilən xəbərdarlıqlar
- **`tracking.chat_messages`** — həkim-xəstə söhbətləri

### Konfiqurasiya (`docker-compose.yml`)
```yaml
postgres:
  image: postgres:16-alpine
  ports: ["127.0.0.1:5440:5432"]  # kənardan qorunur
  volumes:
    - pgdata:/var/lib/postgresql/data
    - ./init-schemas.sql:/docker-entrypoint-initdb.d/init.sql
```

`127.0.0.1:5440` deməkdir ki, DB **yalnız local network-dən** əlçatandır — İnternet-dən birbaşa bağlanmaq mümkün deyil (təhlükəsizlik).

### Sual: Niyə hər servis üçün ayrı DB deyil?
**Cavab**: Ayrı schema saxlayaraq microservices izolyasiyasını qoruyuruq, amma resource sərfini (RAM/CPU) azaldırıq. Prod-da real yükdə ayırmaq olar.

---

## 5. DISCOVERY SERVER (EUREKA)

### Sadə dildə
Böyük ofis binasında yeni işçi gəlir və deyir "Mühasibatı harada tapım?". Reception ona deyir "3-cü mərtəbədə, 305-ci otaqda". Eureka bizim tətbiqin **reception**-udur. Auth-service "Nutrition-service haradadır?" deyəndə Eureka ona ünvan verir.

### Texniki dildə
Netflix Eureka Service Registry. Hər servis start olanda özünü Eureka-ya register edir:
```
POST /eureka/apps/AUTH-SERVICE
{
  "instanceId": "auth-service-1",
  "ipAddr": "172.18.0.5",
  "port": 8081,
  "status": "UP"
}
```

Digər servislər `NutritionClient` kimi `@FeignClient(name="NUTRITION-SERVICE")` yazır — Feign Eureka-dan ünvan alıb HTTP çağırışını yönləndirir.

### Fayllar
- `discovery-server/src/main/java/az/azerkalori/discovery/DiscoveryServerApplication.java`
- Application-də `@EnableEurekaServer` annotasiyası
- `application.yml`: port 8761

---

## 6. API GATEWAY

### Sadə dildə
Bir şirkətin ön qapısı təsəvvür et — bütün gələnlər əvvəlcə qapıçıdan (security) keçirlər. Qapıçı deyir: "Sənin şəxsiyyət vəsiqən var? Kimsən?". Vəsiqə düzdürsə buraxır, düz deyilsə qaytarır. Gateway də bunu edir — bütün istəklər əvvəlcə ora gəlir, JWT tokeni yoxlanır, sonra düzgün servisə göndərilir.

### Texniki dildə
Spring Cloud Gateway (reactive, Netty üzərində). Bütün trafik `:8080` port-una gəlir. **`JwtAuthFilter`** hər istəyi yoxlayır:

**`api-gateway/src/main/java/az/azerkalori/gateway/filter/JwtAuthFilter.java`**:
- Public path-lər siyahısı: `/api/auth/login`, `/api/auth/register`, `/ws` və s. — bunlara token lazım deyil
- Digər istəklərdə `Authorization: Bearer <token>` header-i tələb olunur
- Token doğru olsa, `X-User-Id` və `X-User-Role` header-ləri əlavə edilib backend servisə göndərilir

```java
Claims claims = Jwts.parser().verifyWith(key).build()
        .parseSignedClaims(header.substring(7)).getPayload();

ServerHttpRequest mutated = exchange.getRequest().mutate()
        .header("X-User-Id", claims.getSubject())
        .header("X-User-Role", claims.get("role", String.class))
        .build();
```

### Routing (`api-gateway/src/main/resources/application.yml`)
- `/api/auth/**` → auth-service
- `/api/products/**` → catalog-service
- `/api/goals/**` → nutrition-service
- `/api/logs/**`, `/api/summary/**`, `/api/chat/**` → tracking-service

---

## 7. AUTH SERVICE

### Sadə dildə
Bir klub təsəvvür et — girişdə üzvlük kartın yoxlanılır. Kartın varsa girirsən, yoxdursa qeydiyyat gişəsində alırsan. Auth-service bizim klubun **giriş qapıçısı və qeydiyyat gişəsidir**. Hər kimin adı, şifrəsi və hansı üzvlük səviyyəsinə sahib olduğu burada saxlanılır.

### Texniki dildə
Port `:8081`. İstifadəçi qeydiyyatı, giriş, JWT token verilməsi, Stripe billing, email göndərmə.

### Fayllar

**`AuthServiceApplication.java`** — Spring Boot main class, `@SpringBootApplication`.

**`entity/User.java`** — istifadəçi entity
```java
@Entity @Table(name = "users")
public class User {
    Long id;
    String email;      // unikal
    String password;   // BCrypt hash
    String fullName;
    Role role;         // USER, DOCTOR, ADMIN
    Long doctorId;     // xəstə üçün təyin olunmuş həkim
    boolean pro;       // Stripe abunə
    Instant proUntil;
    boolean approved;  // həkim üçün admin təsdiqi
    Integer age;
    Double weightKg;
    Double heightCm;
    String sex;        // MALE/FEMALE
    String activityLevel;
}
```

**`entity/Role.java`** — enum: `USER`, `DOCTOR`, `ADMIN`.

**`security/JwtService.java`** — JWT token yaradır və imzalayır
```java
public String issue(User user) {
    return Jwts.builder()
        .subject(String.valueOf(user.getId()))  // sub = userId
        .claim("role", user.getRole().name())
        .claim("email", user.getEmail())
        .expiration(Date.from(Instant.now().plus(12, ChronoUnit.HOURS)))
        .signWith(key)   // HMAC-SHA imzalama
        .compact();
}
```
**JWT nədir?** — 3 hissədən ibarət token: `header.payload.signature`. Server signature-ı yoxlayır → token həqiqidirsə userId çıxarır. Serverdə session saxlanılmır (stateless).

**`security/SecurityConfig.java`** — Spring Security konfiqurasiyası:
- `BCryptPasswordEncoder` — parolları hash edir
- CSRF disabled (JWT ilə lazım deyil)
- Bütün endpoint-lər açıq (auth Gateway-də olur)

**`web/AuthController.java`** — REST endpoint-lər:
- `POST /api/auth/register` — qeydiyyat (parol hash edilir, xoş gəldin məktubu göndərilir)
- `POST /api/auth/login` — giriş (parol yoxlanılır, JWT qaytarılır)
- `GET /api/auth/me` — cari istifadəçi məlumatı
- `GET /api/auth/admin/pending` — admin: təsdiq gözləyən həkimlər
- `PUT /api/auth/admin/approve/{id}` — admin: həkim təsdiqi
- `POST /api/auth/admin/doctors` — admin: həkim yaratma
- `PUT /api/auth/admin/patients/{p}/doctor/{d}` — admin: xəstəyə həkim təyin
- `GET /api/auth/doctor/patients` — həkim: öz xəstələri
- `GET /api/auth/internal/users/{id}` — servislərarası çağırışlar üçün

**`web/BillingController.java`** — Stripe ödəniş:
- `POST /api/billing/checkout` — Stripe Checkout Session yaradır
- `POST /api/billing/webhook` — Stripe uğurlu ödənişdən sonra bura çağırır → user.pro=true

**`mail/MailService.java`** — Gmail SMTP ilə xoş gəldin məktubu.

**`config/DataSeeder.java`** — start-da default admin yaradır (əgər DB boşdursa).

**`repo/UserRepository.java`** — JPA Repository:
```java
Optional<User> findByEmail(String email);
List<User> findByApprovedFalse();
List<User> findByRole(Role role);
List<User> findByDoctorId(Long doctorId);
```

---

## 8. CATALOG SERVICE

### Sadə dildə
Bu böyük bir yemək menyusudur. Marketdən aldığın hər məhsulun kalori miqdarı, zülalı, yağı burada yazılıb. Sən "toyuq döşü" axtardığında sənə deyir: "100 qramında 165 kalori var". Əgər axtardığın məhsul bizim bazada yoxdursa, xarici bir bazadan (OpenFoodFacts) çəkir.

### Texniki dildə
Port `:8082`. Qida məhsulları CRUD, axtarış, xarici API inteqrasiyası (barcode scan), Redis cache, GraphQL endpoint.

### Fayllar

**`entity/Product.java`**:
```java
Long id;
String name;
String brand;
String category;
String barcode;
Double calories, proteinG, fatG, carbsG;
boolean enriched;  // OpenFoodFacts-dən gəlibsə true
```

**`repo/ProductRepository.java`** — JPA + custom queries:
```java
Optional<Product> findByBarcode(String barcode);
List<Product> findByNameContainingIgnoreCase(String q);
```

**`web/ProductController.java`** — REST:
- `GET /api/products?q=alma` — axtarış
- `GET /api/products/{id}` — ID ilə
- `GET /api/products/barcode/{code}` — barcode ilə (yoxdursa OpenFoodFacts-dən çəkir)
- `POST /api/products` — yeni məhsul əlavə et

**`web/ProductGraphQL.java`** — GraphQL endpoint (`/graphql`):
```graphql
query { products(q: "alma") { id name calories } }
```

**`client/OpenFoodFactsClient.java`** — barcode ilə xarici API-dən məhsul çəkir:
```
GET https://world.openfoodfacts.org/api/v0/product/{barcode}.json
```

**`client/EnrichmentService.java`** — bazada olan naməlum məhsulları avtomatik doldurur.

**`config/DataSeeder.java`** — start-da CSV-dən 238 məhsul yükləyir (`seed/products.csv`).

### Redis cache
```java
@Cacheable(value = "products", key = "#q")
public List<Product> search(String q) { ... }
```
Eyni sorğuya cavab Redis-də saxlanılır → 2-ci dəfə DB-yə getmir → sürətli.

---

## 9. NUTRITION SERVICE

### Sadə dildə
Bu bir **şəxsi qidalanma məsləhətçisidir**. Sən ona deyirsən "25 yaşındayam, 70 kq, 175 sm, kişi, orta aktiv, arıqlamaq istəyirəm". O sənə hesablayıb deyir: "Sənə gündə 2000 kalori lazımdır, 126 g zülal, 55 g yağ, 200 g karbohidrat". Formullar məşhurdur — Mifflin-St Jeor.

### Texniki dildə
Port `:8084`. BMR/TDEE hesablaması, diet plan idarəetməsi, **gRPC server**.

### Fayllar

**`entity/DietPlan.java`** — istifadəçi üçün aktiv qidalanma planı:
```java
Long userId;
Double dailyCalorieTarget;
Double proteinTarget, fatTarget, carbsTarget;
Long doctorId;      // planı təyin edən həkim (varsa)
boolean active;
```

**`grpc/NutritionGrpcService.java`** — gRPC service (protobuf ilə):

**BMR (Bazal Metabolizma) formulu — Mifflin-St Jeor**:
```
BMR = 10×kg + 6.25×sm − 5×yaş + (kişi ? 5 : -161)
```

**TDEE (Gündəlik enerji sərfi)**:
```
TDEE = BMR × aktivlik_əmsalı
  SEDENTARY:  1.2   (oturaq)
  LIGHT:      1.375 (həftədə 1-3 gün idman)
  MODERATE:   1.55  (3-5 gün)
  ACTIVE:     1.725 (6-7 gün)
  VERY_ACTIVE:1.9   (çox intensiv)
```

**Məqsədə görə düzəliş**:
```
LOSE (arıqla): TDEE × 0.85  (−15%)
MAINTAIN:      TDEE × 1.00
GAIN (kök al): TDEE × 1.15  (+15%)
```

**Makro-nutrient bölgüsü**:
```
Protein: kg × 1.8         (hər qramı = 4 kal)
Yağ:     dailyCal × 25% / 9  (yağın hər qramı = 9 kal)
Karb:    (dailyCal - protein×4 - yağ×9) / 4
```

**`web/GoalController.java`** — REST wrapper, gRPC-yə çağırır:
```
POST /api/goals/calculate
Body: { age, weightKg, heightCm, sex, activityLevel, goal }
Response: { bmr, tdee, dailyCalories, proteinG, fatG, carbsG }
```

**`web/DietPlanController.java`** — diet plan CRUD:
- `POST /api/plans/upsert` — plan yarat/yenilə
- `GET /api/plans/active/{userId}` — aktiv plan (Tracking bunu çağırır)

**`web/AuthClient.java`** — Auth-service-i çağırır (`@FeignClient`), user məlumatını alır.

### Sual: Niyə gRPC?
**Cavab**: gRPC **HTTP/2 + Protocol Buffers** üzərində işləyir — JSON-dan **10x sürətli, daha az trafik**. Servislər arası "gizli" çağırışlar üçün ideal. Frontend-ə REST verilir çünki brauzerlər gRPC dəstəkləmir (birbaşa).

---

## 10. TRACKING SERVICE

### Sadə dildə
Bu bizim əsas **qeydiyyat dəftərimizdir**. Nə vaxt bir qida yeyirsən — burada qeyd olunur. Gün sonunda toplam kaloriyi göstərir. Əgər hədəfi keçsən — həkimə **avtomatik xəbərdarlıq** göndərir. Həkimlə söhbət də burada olur (chat).

### Texniki dildə
Port `:8083`. Qida qeydləri, gündəlik hesabat, WebSocket real-time push, həkim alertləri, chat, Feign clientlər.

### Fayllar

**Entity-lər**:
- **`FoodLog.java`** — bir qida qeydi (userId, productId, calories, macros, logDate)
- **`DailySummary.java`** — gündəlik toplam (userId, day, calories, protein, fat, carbs, targetCalories, doctorId)
- **`Alert.java`** — həkimə xəbərdarlıq (doctorId, patientId, percent)
- **`ChatMessage.java`** — həkim-xəstə mesajı (senderId, receiverId, text, createdAt)

**Repositories** — JPA interfeys-lər:
- `FoodLogRepository`
- `DailySummaryRepository` — `findByUserIdAndDay(userId, day)`
- `AlertRepository`
- `ChatMessageRepository`

**`web/FoodLogController.java`** — qida qeydi endpoint-ləri:
- `POST /api/logs` — yeni qeyd (SummaryService-ə göndərir → summary yenilənir → WebSocket push)
- `GET /api/logs?day=2026-08-28` — gündəlik qeydlər

**`web/SummaryController.java`** — gündəlik hesabat:
- `GET /api/summary?day=...`
- `GET /api/summary/alerts` — həkim: gələn alertlər

**`web/ChatController.java`** — həkim-xəstə chat:
- `POST /api/chat` — mesaj göndər
- `GET /api/chat?with={userId}` — söhbət tarixçəsi

**`web/CatalogClient.java`** — `@FeignClient("CATALOG-SERVICE")` — məhsul məlumatını çəkir.

**`web/NutritionClient.java`** — `@FeignClient("NUTRITION-SERVICE")` — aktiv plan çəkir.

**`web/AuthClient.java`** — `@FeignClient("AUTH-SERVICE")` — user məlumatı.

**`service/SummaryService.java`** — **əsas biznes məntiq**:
```java
@Transactional
public DailySummary apply(Long userId, FoodLog entry) {
    // 1. Bugünkü summary tap (yoxsa yeni yarat + plandan target çək)
    DailySummary summary = summaries.findByUserIdAndDay(userId, day)
            .orElseGet(() -> newSummary(userId, day));

    // 2. Kalorini + makro əlavə et
    summary.setCalories(summary.getCalories() + safe(entry.getCalories()));
    // ... eyni protein/yağ/karb üçün

    // 3. DB-ə save
    DailySummary saved = summaries.save(summary);

    // 4. WebSocket ilə frontend-ə push
    push(saved);
    return saved;
}

private void push(DailySummary s) {
    double percent = 100.0 * s.getCalories() / s.getTargetCalories();
    String level = percent >= 100 ? "LIMIT" : percent >= 80 ? "WARN" : "OK";

    // Frontend widget-i canlı yenilə
    ws.convertAndSendToUser(userId, "/queue/calories", Map.of(
        "calories", ..., "percent", ..., "level", level));

    // Əgər limit aşılıb VƏ həkim təyin olunub → alert
    if (percent >= 100 && s.getDoctorId() != null) {
        alerts.save(Alert.builder()...);  // DB-də qaldı
        ws.convertAndSendToUser(doctorId, "/queue/alerts", ...);  // canlı
    }
}
```

**`service/ChatAccessService.java`** — icazə yoxlaması: xəstə yalnız öz həkimi ilə, həkim yalnız öz xəstələri ilə yaza bilər.

**`service/NutritionPlanClient.java`** — Nutrition-a Feign ilə çağırış.

**`ws/WebSocketConfig.java`** — WebSocket + STOMP:
```java
@Override
public void configureMessageBroker(MessageBrokerRegistry config) {
    config.enableSimpleBroker("/queue", "/topic");
    config.setApplicationDestinationPrefixes("/app");
    config.setUserDestinationPrefix("/user");
}

@Override
public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
}
```

**`ws/JwtChannelInterceptor.java`** — WebSocket üçün JWT auth:
- Frontend CONNECT edəndə `Authorization: Bearer <token>` göndərir
- Interceptor tokeni yoxlayır → userId çıxarır → `Principal` təyin edir
- Bu userId `convertAndSendToUser(userId, ...)` üçün lazımdır

### WebSocket işləmə prinsipi
1. Frontend `wss://kalorikolik.xyz/ws` ilə bağlanır (SockJS fallback ilə)
2. STOMP CONNECT + JWT
3. Frontend `/user/queue/calories`-ə subscribe olur
4. Tracking-service `convertAndSendToUser(userId, "/queue/calories", data)` çağırır
5. Frontend real-time yenilənir — heç bir refresh olmadan

---

## 11. FRONTEND

### Sadə dildə
Bu istifadəçinin gördüyü səhifədir — düymələr, formalar, qrafiklər. Sadə HTML+CSS+JavaScript-dir, heç bir React/Vue yoxdur. Server-dən JSON istəyir, ekranda göstərir. WebSocket vasitəsilə real-time yenilənir (bir qida əlavə etsən, dərhal göstərici artır).

### Texniki dildə
**Vanilla HTML/CSS/JS** — SPA-ya bənzər lakin router yoxdur, bütün UI `index.html`-dədir. Nginx serve edir.

### Fayllar
- **`frontend/index.html`** — bütün səhifə (login, register, dashboard, admin panel, doctor panel, chat, chatbot)
- **`frontend/js/app.js`** — əsas biznes logic (auth, API çağırışları, WebSocket)
- **`frontend/js/chatbot.js`** — RAG chatbot UI (n8n webhook-a bağlanır)
- **`frontend/js/vendor/sockjs.min.js`** — SockJS client
- **`frontend/js/vendor/stomp.umd.min.js`** — STOMP client
- **`frontend/nginx.conf`** — nginx config:
  - `/api/**` → api-gateway:8080-ə proxy
  - `/ws` → api-gateway:8080-ə upgrade (WebSocket)
  - qalanı → static file

### Chatbot inteqrasiyası
Webhook ünvanı **mühitə görə avtomatik** seçilir (lokal dev vs deploy):
```javascript
// frontend/js/chatbot.js
const N8N_CHAT_WEBHOOK = (function () {
  const h = location.hostname;
  if (h === "localhost" || h === "127.0.0.1")
    return "http://localhost:5678/webhook/azerkalori-chat-0001/chat";  // lokal n8n
  const root = h.replace(/^www\./, "");
  return location.protocol + "//n8n." + root + "/webhook/azerkalori-chat-0001/chat";  // droplet
})();

async function send(question) {
    const res = await fetch(N8N_CHAT_WEBHOOK, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
            action: "sendMessage",
            sessionId: sessionId,     // localStorage-də sabit (söhbət yaddaşı)
            chatInput: question
        })
    });
    const data = await res.json();
    // n8n cavabı müxtəlif açarda gələ bilər: output/text/response/answer
    addMsg(extractAnswer(data), "bot");
}
```
**Diqqət**: `sessionId` `localStorage`-də saxlanır → n8n hər sessiyanın söhbət tarixçəsini xatırlayır.

---

## 12. DOCKER

### Sadə dildə
Docker — proqramları **konteynerə** qoyur. Konteyner elə bir qutudur ki, içində proqram və onun ehtiyacı olan bütün şeylər var. Bu qutunu istənilən kompüterə aparıb açsan — eyni şəkildə işləyəcək. "Mənim maşınımda işləyirdi axı!" problemini həll edir.

### Texniki dildə

**Docker Compose** — çox konteyneri bir yerdə idarə edir. `docker-compose.yml`:

```yaml
services:
  postgres:            # DB konteyneri
  redis:               # Cache konteyneri
  discovery-server:    # Eureka
  api-gateway:         # Gateway
  auth-service:        # Auth
  catalog-service:     # Catalog
  nutrition-service:   # Nutrition
  tracking-service:    # Tracking
  frontend:            # Nginx
```

Hər servisdə:
- `restart: unless-stopped` — server reboot olsa avtomatik qalxsın
- `depends_on` — qaldırma sırası (əvvəlcə DB, sonra servislər)
- `environment` — dəyişənlər (`.env`-dən çəkilir)
- `JAVA_TOOL_OPTIONS: -Xmx256m` — hər servis maks. 256MB RAM (4GB droplet-də sığsın)

**Dockerfile** (hər servisdə var):
```dockerfile
FROM eclipse-temurin:17-jre-alpine
COPY build/libs/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","/app.jar"]
```

### Əsas komandalar
```bash
docker compose up -d              # arxa fonda başlat
docker compose down               # dayandır
docker compose logs -f auth-service  # logları izlə
docker compose ps                 # işləyən konteynerləri gör
docker compose up -d --build      # yenidən build et və başlat
```

---

## 13. N8N (AI CHATBOT + RAG)

### Sadə dildə
Sayta girib "Alma neçə kaloridir?" soruşursan. Bu sual sənin tətbiqindəki chatbot-a gedir → chatbot n8n adlı bir vasitəyə göndərir → n8n Qdrant adlı **vektor bazadan** oxşar məlumat tapır (məs. "Alma: 52 kal") → bu məlumatı bir AI-a (Groq/Llama 3.3) göndərir → AI insana bənzər cavab formalaşdırır → cavab geri qayıdır. Bütün bu proseslər 1-2 saniyəyə baş verir.

### Texniki dildə
**RAG** = **R**etrieval **A**ugmented **G**eneration. LLM-ə context vermək üçün istifadə olunur:

```
User: "Alma neçə kaloridir?"
   ↓
n8n webhook (Chat Trigger)
   ↓
1) Embed sualı (OpenAI embeddings)
   ↓
2) Qdrant-da semantic search (yaxın chunk-lar)
   ↓
3) System prompt + context + question → Groq (Llama 3.3)
   ↓
4) Cavab qaytar
```

### Komponent-lər
- **n8n** — workflow avtomasiya platforması (visual node-based)
- **Qdrant** — vector DB (embedding-lər saxlanır)
- **Groq** — LLM inference (Llama 3.3 çox sürətli, pulsuz tier var)
- **OpenAI Embeddings** — mətni vektora çevirir (ölçü: 1536)

### Fayllar
- `docker-compose.ai.yml` — n8n + qdrant konteynerləri
- `n8n/azerkalori-agent.workflow.json` — chatbot workflow
- `n8n/azerkalori-rag.workflow.json` — knowledge ingest workflow
- `n8n/telegram-calorie-bot.workflow.json` — Telegram bot
- `n8n/calorie-calculator.workflow.json` — kalori hesablayıcı workflow
- `shared/azerkalori-knowledge.txt` — RAG knowledge base

### Setup addımları (necə qurdum)
1. `docker-compose.ai.yml`-i serverdə işə saldıq: `docker compose -f docker-compose.ai.yml up -d`
2. Caddy-də n8n üçün subdomain: `n8n.kalorikolik.xyz → localhost:5678`
3. DNS-də n8n üçün A Record: `n8n → 209.38.195.172`
4. n8n panelə (`https://n8n.kalorikolik.xyz`) girib owner hesabı yaradıldı
5. Workflow-lar import edildi
6. **Credentials**:
   - OpenAI API key (embeddings üçün)
   - Groq API key (LLM üçün)
   - Qdrant: `http://qdrant:6333` (container network)
7. Knowledge fayl `shared/azerkalori-knowledge.txt`-ə yazıldı, container-in içindəki `/data/shared/`-da mount olundu
8. **Ingest workflow** manuel işə salındı → mətn chunk-lara bölündü → embed olundu → Qdrant-a yazıldı
9. Chat Trigger node-da "Make Chat Publicly Available" aktivləşdirildi
10. Frontend-də chatbot bu webhook-a bağlandı

### N8N_RESTRICT_FILE_ACCESS_TO
Standart olaraq n8n təhlükəsizlik məqsədilə fayl oxumur. Bunu aktivləşdirmək lazım oldu:
```yaml
environment:
  - N8N_RESTRICT_FILE_ACCESS_TO=/data/shared
```

---

## 14. DNS VƏ DOMAIN

### Sadə dildə
`kalorikolik.xyz` — bu ünvan bir kompüterə (server) yönləndirilməlidir. Bu iş **DNS** (Domain Name System) vasitəsilə olur. DNS bir "telefon kitabçası" kimidir — ad → IP çevirir. Sən brauzerdə `kalorikolik.xyz` yazanda kompüter DNS-dən soruşur "bunun IP-si nədir?", DNS `209.38.195.172` cavab verir, brauzer həmin ünvana bağlanır.

### Texniki dildə
- Domain **Namecheap**-dan alındı (~$1/il, .xyz uzantısı)
- **A Record** əlavə edildi: `@ → 209.38.195.172`
- **A Record** subdomain: `n8n → 209.38.195.172`
- Standart nameserver-lər (Namecheap BasicDNS)
- DNS yayılması ~5-30 dəqiqə çəkir (`dig +short kalorikolik.xyz @8.8.8.8`)

### HTTPS (Caddy)
Caddy — reverse proxy, avtomatik Let's Encrypt SSL certificate alır. Konfiqurasiya:

**`/etc/caddy/Caddyfile`**:
```
kalorikolik.xyz {
    reverse_proxy localhost:80
}

n8n.kalorikolik.xyz {
    reverse_proxy localhost:5678
}
```

Caddy `systemctl reload caddy` sonra:
1. Let's Encrypt-dən SSL sertifikatını alır (avtomatik, pulsuz)
2. `:443` port-unda TLS terminate edir
3. Sertifikat 90 gün, avtomatik yenilənir

---

## 15. DEPLOYMENT (DIGITALOCEAN)

### Sadə dildə
Öz kompüterində proqramı yazırsan — amma istəyirsən ki, dünyanın hər yerindən girə bilsinlər. Öz kompüterini 24/7 açıq saxlaya bilməzsən. Ona görə internetdə bir "kompüter kirayələyirsən" — bu droplet-dir. DigitalOcean sənə Ubuntu üzərində virtual bir kompüter verir, sən oraya öz proqramını qoyursan, dünya girə bilir.

### Texniki dildə

### Droplet detayları
- **Provider**: DigitalOcean
- **Region**: Frankfurt (FRA1) — Azərbaycana yaxın latency
- **Plan**: $24/ay (2 vCPU, 4 GB RAM, 80 GB SSD)
- **OS**: Ubuntu 24.04 LTS
- **IP**: `209.38.195.172`

### İlkin setup addımları (necə qurdum)
1. **SSH key** yaratdım (öz Mac-da):
   ```bash
   ssh-keygen -t ed25519 -C "azerkalori"
   cat ~/.ssh/id_ed25519.pub  # public key
   ```
2. Public key-i DigitalOcean-a əlavə etdim, droplet yaradanda seçdim
3. Droplet-ə SSH:
   ```bash
   ssh root@209.38.195.172
   ```
4. **Docker qurmaq**:
   ```bash
   curl -fsSL https://get.docker.com | sh
   ```
5. **Repo clone**:
   ```bash
   git clone https://github.com/Vusal96-creator/azerkalori-microservicesdep.git
   cd azerkalori-microservicesdep
   ```
6. **`.env` yarat**:
   ```bash
   nano .env
   # POSTGRES_USER, POSTGRES_PASSWORD, JWT_SECRET, STRIPE_*, MAIL_*, APP_URL...
   ```
7. **Build və başlat**:
   ```bash
   docker compose up -d --build
   ```
8. **Yoxla**:
   ```bash
   docker compose ps
   curl -I http://localhost
   ```
9. **Caddy quraşdır** (yuxarıda göstərildi)

### Firewall (UFW)
```bash
ufw allow 22/tcp     # SSH
ufw allow 80/tcp     # HTTP (Caddy istifadə edir)
ufw allow 443/tcp    # HTTPS
ufw enable
```

Digər portlar (5440, 8080, 5678 və s.) yalnız `127.0.0.1`-ə bağlıdır — kənardan görünmür.

---

## 16. CI/CD (GITHUB ACTIONS)

### Sadə dildə
Sən kodu yazırsan → GitHub-a push edirsən. Robotlar dərhal:
1. Kodu yükləyir
2. Bütün testləri işlədir → əgər hər hansı test qırılsa DAYANDIRIR
3. Testlər keçdisə → serverə SSH edir → yeni kodu çəkir → tətbiqi yenidən başladır

Beləliklə **push et → sayt avtomatik yenilənir**. Əl işi yoxdur.

### Texniki dildə

**`.github/workflows/ci.yml`**:

```yaml
name: CI/CD

on:
  push:
    branches: [main]     # yalnız main-ə push-da deploy
  pull_request:          # PR-da yalnız test

jobs:
  build-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
      - run: chmod +x gradlew
      - name: Build & test
        run: ./gradlew test --no-daemon

  deploy:
    needs: build-test              # yalnız test KEÇSƏ
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    steps:
      - name: Droplet-ə deploy
        uses: appleboy/ssh-action@v1.2.0
        with:
          host: 209.38.195.172
          username: root
          key: ${{ secrets.DROPLET_SSH_KEY }}
          script: |
            cd /root/azerkalori-microservicesdep
            git stash push --include-untracked || true
            git pull --rebase origin main || true
            git stash pop || true
            docker compose up -d --build
```

### `DROPLET_SSH_KEY` (secret)
GitHub → Settings → Secrets → yeni SSH private key əlavə etmək lazımdır:
```bash
# Öz Mac-da
cat ~/.ssh/id_ed25519    # bu private key-in içindəkini
```
Bu kontent GitHub secret-ə yapışdırılır. Sonra GitHub Actions bu key-lə serverə SSH edir.

### Necə işləyir?
```
git push origin main
   ↓
GitHub triggers workflow
   ↓
build-test job:  ✓ (Java 17 setup → gradle test → 18 test keçdi)
   ↓
deploy job:  ✓ (SSH → git pull → docker compose up -d --build)
   ↓
kalorikolik.xyz yenilənir 🚀
```

---

## 17. TESTLƏR

### Sadə dildə
Test — kodun bir hissəsini "yoxlayan" başqa bir koddur. Məsələn: SummaryService-də hesablama qaydası var. Test deyir: "500 kalori əlavə et, sonra summary-də 500 olsun". Əgər olmadısa test qırılır — deməli kodda səhv var. Bizdə 18 test var.

### Texniki dildə

**JUnit 5 + Mockito**. Repository və external service-lər mock edilir, business logic təcrid olunmuş test olunur.

**`tracking-service/src/test/.../SummaryServiceTest.java`** (18-dən 8-i):

- `firstFoodLog_createsSummary_andSumsCalories` — ilk qeyd summary yaradır
- `nullMacros_areTreatedAsZero_noNpe` — null-lar safe() ilə 0-a çevrilir
- `apply_sendsWebSocketUpdateToUser` — WebSocket push işləyir (verify)
- `push_payload_hasCorrectPercentAndLevel` — payload düzgündür (ArgumentCaptor)
- `level_isComputedFromPercent` — parameterized test (OK/WARN/LIMIT sərhədləri)
- `doctorAlert_sent_whenLimitExceeded_andDoctorAssigned` — həkim alerti
- `doctorAlert_notSent_whenUnderLimit` — limit aşılmasa alert yox
- `doctorAlert_notSent_whenNoDoctorAssigned` — həkim yoxsa alert yox

**Testin əsas hissələri**:
```java
@ExtendWith(MockitoExtension.class)
class SummaryServiceTest {

    @Mock DailySummaryRepository summaries;
    @Mock AlertRepository alerts;
    @Mock NutritionPlanClient plans;
    @Mock SimpMessagingTemplate ws;

    @InjectMocks SummaryService service;

    @Test
    void test1() {
        // ARRANGE (hazırlıq)
        when(summaries.findByUserIdAndDay(...)).thenReturn(Optional.empty());

        // ACT (icra)
        DailySummary result = service.apply(userId, entry);

        // ASSERT (yoxlama)
        assertEquals(500.0, result.getCalories());
        verify(ws).convertAndSendToUser(...);
    }
}
```

**`auth-service/src/test/.../JwtServiceTest.java`** — JWT issue/parse testi.

### İşə salmaq
```bash
./gradlew test                    # bütün servislərdə testlər
./gradlew :auth-service:test      # yalnız auth
```

---

## 18. TEZ-TEZ VERİLƏN SUALLAR

### S1: Niyə Microservices? Monolit daha sadə olardı?
**C**: Layihə real prod-da böyüyə bilər — ayrı komandalar ayrı sahələrə görə cavabdeh olsun. Bir bug pusdusa hamısını çökdürməsin. Scale edərkən yalnız yüklü servisi çoxaltmaq olar (məs. Black Friday-də tracking-service 5 instance-a). Bundan başqa — akademik məqsəd: microservices akademik cəhətdən əhəmiyyətli mövzudur.

### S2: JWT nədir və niyə session yerinə istifadə edildi?
**C**: JWT (JSON Web Token) — imzalanmış token. Server session saxlamır — stateless. Hər istəkdə token gəlir, server imzanı yoxlayır. Üstünlüklər: (1) horizontal scale asan (session store lazım deyil), (2) microservices arasında token asan ötürülür.

### S3: gRPC ilə REST fərqi nədir?
**C**: gRPC — binary (Protocol Buffers), HTTP/2 üzərində, 10x sürətli. Servislər arası çağırışlar üçün ideal. REST — mətn (JSON), HTTP/1.1, brauzer dəstəkləyir. Frontend-lə REST, servislərarası gRPC.

### S4: WebSocket niyə lazımdır? Polling olar axı?
**C**: Polling — hər 5 saniyə "yeni məlumat var?" soruşmaq — trafik və server yükü. WebSocket — daimi bağlantı, server istədiyi vaxt push edir. Kalori widget-i real-time yenilənir, həkim alert dərhal görünür.

### S5: Redis niyə lazımdır?
**C**: PostgreSQL-də hər sorğu disk oxuyur — yavaş. Redis in-memory (RAM), 100x sürətli. Catalog-service-də məhsul axtarışları cache olunur.

### S6: Docker Compose-un üstünlüyü?
**C**: 9 servisin hamısını **1 komandayla** başladırıq (`docker compose up -d`). Konfiqurasiya bir fayldadır. Dev və prod arasında fərq yoxdur.

### S7: Eureka olmadan servislər bir-birini necə tapardı?
**C**: Hard-coded URL-lar: `http://auth-service:8081` — amma bu scale-də işləmir (bir servis 5 instance-da olsa). Eureka load balancing edir və health-check ilə çökən instance-ları çıxarır.

### S8: Nə üçün Postgres schema-per-service?
**C**: Microservices prinsipi — data isolation. Bir servis digərinin cədvəllərinə birbaşa girməməli. Ayrı DB instance ideal olardı, amma 4GB droplet-də sığmayacaqdı. Schema-lar kompromisidir.

### S9: n8n workflow-un iş prinsipi?
**C**: Visual programming — node-lar bir-birinə bağlanır. Chat Trigger → Embeddings → Qdrant Search → Groq LLM → Response. Hər node bir addım.

### S10: RAG olmasa AI kalori sualına cavab verə bilməzmi?
**C**: LLM-in özündə çoxlu ümumi bilik var, amma bizim tətbiqin **spesifik məlumatları** (biz olan qidalar, formullar, azərbaycanca izahlar) LLM-də yoxdur. RAG onları context kimi ötürür → cavab dəqiq və tətbiqə uyğun.

### S11: CI/CD niyə vacibdir?
**C**: Manuel deploy — səhv riski. CI/CD ilə: (1) hər push-da testlər — sınıq kod aşkar olunur, (2) test keçəndə avtomatik deploy — insan işi yoxdur, (3) versiya nəzarəti — hansı commit prod-da olduğu bilinir.

### S12: HTTPS nə üçün lazımdır?
**C**: HTTP — açıq, hər kəs oxuya bilər (parol, token). HTTPS — TLS ilə şifrələnmiş. SEO-ya, brauzer trust-a təsir edir. Caddy avtomatik Let's Encrypt-dən pulsuz sertifikat alır.

### S13: Feign Client nədir?
**C**: Spring Cloud OpenFeign. Servislərarası HTTP çağırışları interface kimi yazılır:
```java
@FeignClient(name="AUTH-SERVICE")
public interface AuthClient {
    @GetMapping("/api/auth/internal/users/{id}")
    User getUser(@PathVariable Long id);
}
```
Spring proxy generate edir. Eureka ilə birləşdirir — service name → real URL.

### S14: BMR formulu Harris-Benedict yerine niye Mifflin-St Jeor?
**C**: Mifflin-St Jeor 1990-cı ildə çıxıb, müasir populyasiya üçün Harris-Benedict-dən (1919) daha dəqiqdir. Akademik ədəbiyyat da bunu tövsiyə edir.

### S15: Layihədə istifadə etdiyin ən çətin hissə nə oldu?
**C**: WebSocket + JWT auth — STOMP-da Principal-i düzgün təyin etmək, `JwtChannelInterceptor`-un CONNECT frame-də tokeni oxuması. Bir də inter-service communication (Feign + Eureka + Circuit breaker düşüncəsi).

---

## 🎯 MÜDAFİƏ HAZIRLIĞI ÜÇÜN İP UCLARI

1. **Diagramı əzbərlə** — servislərin əlaqələrini lövhədə çəkməyi bacar
2. **Bir user flow-u tam izah et**: user login → JWT gəlir → qida əlavə edir → tracking-service → summary → WebSocket push
3. **Kod nümunəsi göstərə bilməlisən**:
   - `JwtService.issue()` — token necə yaradılır
   - `JwtAuthFilter.filter()` — gateway auth
   - `SummaryService.apply()` — biznes məntiq
4. **Docker anla**: konteyner ≠ VM (VM öz OS-i, konteyner host OS istifadə edir)
5. **Real vs mock testi bil**: nə üçün Repository mock etmişik? — DB-siz sürətli izolyasiya
6. **Deployment addımlarını təkrarla**: SSH → git clone → .env → docker compose up
7. **DNS anla**: A Record = ad → IP; TTL = cache müddəti

Uğurlar! 💪
