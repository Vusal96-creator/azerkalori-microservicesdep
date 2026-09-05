# AzərKalori — Backend Kodu, Class-by-Class (Tam Dərslik)

> Bu sənəd bütün backend Java class-larını **0-dan axıra** izah edir. Hər class üçün: **① nə edir (sadə dildə), ② sətir-sətir texniki izah, ③ müdafiədə deyəcəyin cümlə**.
>
> Struktur: `az.azerkalori.<servis>` paketləri. 6 servis: discovery, gateway, auth, catalog, nutrition, tracking.

---

# 📁 ÜMUMİ QAYDA — Hər servisdə eyni struktur

Hər Spring Boot servisində eyni qovluq strukturu var:
```
src/main/java/az/azerkalori/<servis>/
├── <Servis>Application.java   ← başlanğıc nöqtəsi (main)
├── entity/                    ← DB cədvəllərinin Java qarşılığı
├── repo/                      ← DB sorğuları (JPA)
├── web/                       ← REST controller-lər (endpoint)
├── service/                   ← biznes məntiq
├── config/                    ← konfiqurasiya, seed data
└── ...
src/main/resources/
└── application.yml            ← ayarlar (port, DB, Eureka)
```

**Lombok qeydi**: Bütün entity-lərdə `@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor` görəcəksən. Bunlar **Lombok** annotasiyalarıdır — get/set metodlarını, constructor-ları, builder pattern-i **avtomatik yaradır**, əl ilə yazmağa ehtiyac qalmır. Kod qısalır.

---

# 1️⃣ DISCOVERY SERVER (Eureka)

## `DiscoveryServerApplication.java`
```java
@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServerApplication.class, args);
    }
}
```

**① Sadə dildə**: Bu, tətbiqin "reception"-udur. Bütün servislər start olanda buraya "mən buradayam, ünvanım budur" deyir. Bir servis digərini axtaranda bura müraciət edir.

**② Texniki**:
- `@SpringBootApplication` — Spring Boot-un əsas annotasiyası (auto-config + component scan + configuration).
- `@EnableEurekaServer` — bu servisi **Eureka registry server**-ə çevirir. Tək bu annotasiya kifayətdir, qalanını Spring Cloud edir.
- `main()` metodu Spring context-i qaldırır, port `8761`-də dinləyir.

**③ Müdafiədə**: "Discovery Server Netflix Eureka-dır. `@EnableEurekaServer` ilə servis registry rolunu oynayır — bütün digər servislər ona register olur, bir-birini service name ilə tapır (hard-coded IP olmadan)."

---

# 2️⃣ API GATEWAY

## `GatewayApplication.java`
```java
@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
```
**① Sadə dildə**: Gateway-in başlanğıc nöqtəsi. Sadəcə tətbiqi qaldırır.

**② Texniki**: Routing konfiqurasiyası `application.yml`-dədir (koda yazılmır). Spring Cloud Gateway reactive-dir (Netty üzərində).

## `filter/JwtAuthFilter.java` ⭐ (ƏN VACİB)
```java
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/login", "/api/auth/register", "/api/auth/ping",
            "/api/goals/calculate", "/api/billing/webhook", "/ws");

    private final SecretKey key;

    public JwtAuthFilter(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (PUBLIC_PATHS.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);   // token yoxlamadan keç
        }
        String header = exchange.getRequest().getHeaders().getFirst(AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            return reject(exchange);          // token yoxdur → 401
        }
        try {
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(header.substring(7)).getPayload();
            ServerHttpRequest mutated = exchange.getRequest().mutate()
                    .header("X-User-Id", claims.getSubject())
                    .header("X-User-Role", claims.get("role", String.class))
                    .build();
            return chain.filter(exchange.mutate().request(mutated).build());
        } catch (Exception e) {
            return reject(exchange);          // token yanlış → 401
        }
    }
    @Override public int getOrder() { return -1; }  // ən əvvəl işləsin
}
```

**① Sadə dildə**: Binanın qapıçısı. Hər gələni yoxlayır. Bəzi yerlərə (login, qeydiyyat) hər kəs girə bilər — "public". Digər yerlərə girmək üçün "şəxsiyyət vəsiqəsi" (JWT token) lazımdır. Vəsiqə düzdürsə, qapıçı üstünə "bu 5 nömrəli istifadəçidir, rolu USER" yazıb içəri buraxır.

**② Texniki**:
- `implements GlobalFilter` — **bütün** istəklərdən keçən filter.
- `PUBLIC_PATHS` — token tələb etməyən yollar (login, register, Stripe webhook, WebSocket handshake).
- Constructor-da `jwt.secret` ilə HMAC-SHA key yaradılır (imza yoxlaması üçün).
- `filter()` məntiq:
  1. Path public-dirsə → birbaşa keç
  2. `Authorization: Bearer <token>` yoxdursa → 401
  3. Token varsa → `Jwts.parser().verifyWith(key)` imzanı yoxlayır
  4. **Vacib**: tokendən `X-User-Id` və `X-User-Role` header-ləri çıxarılıb backend servisə əlavə edilir. Beləliklə backend servislər tokeni yenidən parse etməli deyil — hazır header alır.
- `getOrder() = -1` — bu filter **hamıdan əvvəl** işləsin (aşağı order = yüksək prioritet).

**③ Müdafiədə**: "Bütün auth Gateway-də mərkəzləşib. `JwtAuthFilter` `GlobalFilter`-dir — hər istəyi tutur, JWT imzasını yoxlayır, tokendən userId və role çıxarıb `X-User-Id`/`X-User-Role` header kimi backend-ə ötürür. Backend servislər tokeni bilmir — sadəcə bu header-lərə güvənir (çünki yalnız Gateway-dən keçə bilər). Bu, **centralized authentication** pattern-idir."

---

# 3️⃣ AUTH SERVICE

## `AuthServiceApplication.java`
```java
@SpringBootApplication
@EnableAsync
public class AuthServiceApplication { ... }
```
**② Texniki**: `@EnableAsync` — `@Async` metodların (email göndərmə) arxa fonda, ayrı thread-də işləməsinə imkan verir.

## `entity/Role.java`
```java
public enum Role { USER, DOCTOR, ADMIN }
```
**① Sadə dildə**: İstifadəçinin 3 növü: adi istifadəçi, həkim, admin.
**② Texniki**: Java enum — məhdud sabit dəyərlər. DB-də STRING kimi saxlanılır (`@Enumerated(EnumType.STRING)`).

## `entity/User.java` ⭐
```java
@Entity @Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)  // parol JSON-a çıxmır
    @Column(nullable = false)
    private String password;

    private String fullName;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Role role;

    private Long doctorId;        // xəstəyə təyin olunmuş həkim id-si
    private boolean pro;          // Stripe abunə
    private Instant proUntil;     // abunə bitmə tarixi
    private boolean approved;     // həkim üçün admin təsdiqi

    private Integer age;
    private Double weightKg, heightCm;
    private String sex, activityLevel;
}
```
**① Sadə dildə**: Bir istifadəçi kartı. Ad, email, şifrə, rol, yaşı, çəkisi və s. Hər sətir DB-də bir sütundur.

**② Texniki**:
- `@Entity` — bu class DB cədvəlinə map olunur (JPA/Hibernate).
- `@Id @GeneratedValue(IDENTITY)` — `id` primary key, auto-increment.
- `@Column(unique = true)` — email təkrarlana bilməz.
- `@JsonProperty(WRITE_ONLY)` — **təhlükəsizlik**: parol JSON cavabında **heç vaxt** göstərilmir (yalnız oxunur, yazılmır).
- Qalan sahələr BMR hesablaması üçün (yaş, çəki, boy, cins, aktivlik).

**③ Müdafiədə**: "User entity JPA ilə DB-yə map olunur. Parol `@JsonProperty(WRITE_ONLY)` ilə API cavabından gizlədilir — DB-də isə BCrypt hash saxlanılır, açıq mətn deyil."

## `repo/UserRepository.java`
```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    List<User> findByDoctorId(Long doctorId);
    List<User> findByRole(Role role);
    List<User> findByApprovedFalse();
}
```
**① Sadə dildə**: DB ilə danışan tərcüməçi. "Email-i bu olan istifadəçini tap" desən, o SQL-ə çevirib DB-dən gətirir.

**② Texniki**:
- `extends JpaRepository<User, Long>` — Spring Data JPA. `save`, `findById`, `findAll`, `delete` kimi metodlar **hazır gəlir**, yazmaq lazım deyil.
- **Query Methods**: `findByEmail` — Spring metod adından SQL generate edir (`SELECT * FROM users WHERE email = ?`). Kod yazmadan sorğu!
- `Optional<User>` — nəticə ola da bilər, olmaya da (null-safe).

**③ Müdafiədə**: "Spring Data JPA istifadə edirəm. Repository interface-dir, implementation-ı Spring runtime-da generate edir. Metod adı (`findByEmail`) → avtomatik SQL. CRUD metodları JpaRepository-dən miras qalır."

## `security/JwtService.java` ⭐
```java
@Service
public class JwtService {
    private final SecretKey key;
    public JwtService(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
    public String issue(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))     // sub = userId
                .claim("role", user.getRole().name())
                .claim("email", user.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(12, ChronoUnit.HOURS)))
                .signWith(key)
                .compact();
    }
}
```
**① Sadə dildə**: "Şəxsiyyət vəsiqəsi" istehsalçısı. İstifadəçi girişdən keçəndə ona bir token (şifrələnmiş kart) verir. Kartın üstündə: "sən kimsən (id), rolun nədir, nə vaxta qədər etibarlıdır".

**② Texniki**:
- **JWT** = header.payload.signature — 3 hissə, nöqtə ilə ayrılır.
- `subject` = userId (kartın sahibi).
- `claim` = əlavə məlumat (role, email).
- `expiration` = 12 saat sonra token etibarsız.
- `signWith(key)` = **HMAC-SHA imza**. Server öz gizli açarı ilə imzalayır. Kimsə tokeni dəyişsə imza uyğunsuz olur → rədd edilir.
- Server session saxlamır — **stateless**. Bütün məlumat tokendə.

**③ Müdafiədə**: "JWT stateless auth üçün. `issue()` userId-ni subject, role-u claim kimi qoyub HMAC-SHA ilə imzalayır. Server heç nə saxlamır — token özündə hər şeyi daşıyır. Gateway imzanı yoxlayaraq tokenin həqiqiliyini təsdiqləyir. 12 saatlıq expiry təhlükəsizlik üçün."

## `security/SecurityConfig.java`
```java
@Configuration
public class SecurityConfig {
    @Bean public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    @Bean public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
                .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                .build();
    }
}
```
**① Sadə dildə**: İki şey qurur: (1) parolları necə "gizlədək" (BCrypt), (2) təhlükəsizlik qaydaları.

**② Texniki**:
- `BCryptPasswordEncoder` — parolları **hash** edir. BCrypt hər dəfə fərqli salt istifadə edir → eyni parol fərqli hash verir. Geri qaytarmaq mümkün deyil (one-way).
- `csrf.disable()` — CSRF token lazım deyil (JWT var, cookie yoxdur).
- `STATELESS` — Spring session yaratmasın.
- `anyRequest().permitAll()` — bütün endpoint-lər açıq, çünki **auth Gateway-də olur**, burada təkrar lazım deyil.

**③ Müdafiədə**: "Auth-service-də Spring Security yalnız BCrypt encoder üçün lazımdır. Endpoint-lər `permitAll` çünki real auth Gateway-də mərkəzləşib. Parollar BCrypt ilə hash-lanır — DB sızsa belə parollar oxunmaz."

## `web/AuthController.java` ⭐
**① Sadə dildə**: Giriş və qeydiyyat gişəsi + admin idarəetməsi. Bütün "kim daxil olur, kim həkim təsdiqləyir" burada.

**② Texniki** — əsas endpoint-lər:
```java
@PostMapping("/register")   // qeydiyyat
public Map<String, Object> register(@RequestBody RegisterRequest req) {
    // 1. Email təkrardırsa → 409 CONFLICT
    // 2. Rol seç: DOCTOR isə approved=false (admin gözləyir), USER isə approved=true
    // 3. Parolu BCrypt hash et
    // 4. DB-ə save
    // 5. Xoş gəldin məktubu göndər (@Async)
    // 6. USER-ə dərhal token ver, həkimə vermə (təsdiq gözləsin)
}

@PostMapping("/login")      // giriş
public Map<String, Object> login(@RequestBody LoginRequest req) {
    // 1. Email ilə tap
    // 2. encoder.matches(gələn parol, DB-dəki hash) → yoxla
    // 3. Uyğunsuz → 401. Təsdiqlənməmiş həkim → 403
    // 4. JWT token ver
}
```
Digər endpoint-lər (hamısı `X-User-Role` header ilə rol yoxlayır):
- `GET /admin/pending` — təsdiq gözləyən həkimlər (ADMIN)
- `PUT /admin/approve/{id}` — həkim təsdiqi (ADMIN)
- `POST /admin/doctors` — admin həkim yaradır
- `PUT /admin/patients/{p}/doctor/{d}` — xəstəyə həkim təyin
- `GET /doctor/patients` — həkimin xəstələri
- `GET /internal/users/{id}` — **servislərarası** çağırış (Nutrition, Tracking bunu Feign ilə çağırır)

**Java record** qeydi:
```java
public record RegisterRequest(String email, String password, ...) {}
```
`record` — dəyər daşıyan immutable class. Constructor, getter, equals/hashCode avtomatik. DTO üçün ideal.

**③ Müdafiədə**: "AuthController REST controller-dir. Register-də parol BCrypt hash-lanır, həkim rolu admin təsdiqi tələb edir (`approved=false`). Login-də `encoder.matches()` ilə parol yoxlanılır, uğurlu olsa JWT verilir. `X-User-Role` header ilə admin/doctor endpoint-ləri qorunur. `/internal/**` endpoint-i servisdən-servisə çağırış üçündür."

## `mail/MailService.java`
```java
@Service
public class MailService {
    @Async
    public void sendWelcome(String toEmail, String fullName) {
        if (!enabled) return;
        try {
            // MimeMessage yarat, HTML template doldur, göndər
        } catch (Exception e) {
            log.warn(...);  // xəta olsa qeydiyyat sınmır
        }
    }
}
```
**① Sadə dildə**: Qeydiyyatdan sonra "xoş gəldin" məktubu göndərir. HTML formada, yaşıl dizaynlı.

**② Texniki**:
- `@Async` — arxa fonda, ayrı thread-də işləyir. İstifadəçi cavabı gözləmir.
- Gmail SMTP (`smtp.gmail.com:587`, App Password ilə).
- **Xəta udulur**: SMTP çöksəydi belə qeydiyyat uğurlu olur (try-catch). Email kritik deyil.
- HTML template Java Text Block (`"""..."""`) ilə.

**③ Müdafiədə**: "MailService Gmail SMTP ilə xoş gəldin məktubu göndərir. `@Async` sayəsində qeydiyyat cavabı gecikmir. Xəta tolerantdır — email getməsə belə qeydiyyat pozulmur."

## `web/BillingController.java` ⭐ (Stripe)
**① Sadə dildə**: Pro abunə ödənişi. İstifadəçi "Pro almaq istəyirəm" deyir → Stripe-ın öz səhifəsinə yönlənir → kartı orada daxil edir (bizim serverə **heç vaxt kart gəlmir**) → ödəniş uğurlu olsa Stripe bizə xəbər verir → biz istifadəçini Pro edirik.

**② Texniki** — 4 endpoint:
```java
@PostMapping("/checkout")   // ödəniş sessiyası yarat
// Stripe Checkout Session yaradır, ödəniş URL-i qaytarır.
// clientReferenceId = userId (webhook-da kimin ödədiyini bilmək üçün)

@PostMapping("/webhook")    // Stripe geri çağırışı
// Webhook.constructEvent(payload, signature, secret) → İMZA YOXLA
// "checkout.session.completed" event-i gəlsə → user.pro = true
// İmza yoxlaması vacib: saxta webhook göndərilə bilməz

@PostMapping("/simulate")   // test: ödənişsiz Pro (demo üçün)
@GetMapping("/status")      // Pro statusu
```

**Təhlükəsizlik**: Kart məlumatı Stripe-ın hosted səhifəsində daxil olunur — PCI compliance Stripe-ın üzərindədir. Webhook imza ilə qorunur (`webhook-secret`).

**③ Müdafiədə**: "Stripe Checkout istifadə edirəm — kart məlumatı bizim backend-ə heç vaxt gəlmir, Stripe-ın öz səhifəsində daxil olunur. Ödəniş uğurundan sonra Stripe webhook göndərir, imza yoxlanılır (saxta webhook mümkün deyil), sonra user Pro edilir. Test rejimindədir. Demo üçün `/simulate` endpoint-i də var."

## `config/DataSeeder.java`
```java
@Component
public class DataSeeder implements CommandLineRunner {
    @Override public void run(String... args) {
        if (users.count() > 0) return;   // artıq varsa təkrar yaratma
        // admin@azerkalori.az / admin123 (ADMIN)
        // doctor@azerkalori.az / doctor123 (DOCTOR)
        // user@azerkalori.az / user123 (USER, doctorId=həkim)
    }
}
```
**① Sadə dildə**: İlk dəfə işə düşəndə DB-yə 3 nümunə istifadəçi qoyur (admin, həkim, istifadəçi) ki, boş ekran olmasın, test edə biləsən.

**② Texniki**: `CommandLineRunner` — Spring start olduqdan sonra `run()` avtomatik çağırılır. `if (count > 0) return` — idempotent (təkrar işləməz).

**③ Müdafiədə**: "DataSeeder start-da default admin/doctor/user yaradır — demo və test üçün. `count() > 0` yoxlaması ilə yalnız boş DB-də işləyir."

---

# 4️⃣ CATALOG SERVICE (Qidalar)

## `CatalogServiceApplication.java`
```java
@SpringBootApplication
@EnableCaching        // Redis cache aktiv
@EnableFeignClients   // xarici API çağırışları üçün
public class CatalogServiceApplication { ... }
```
**② Texniki**: `@EnableCaching` — `@Cacheable` işləsin. `@EnableFeignClients` — OpenFoodFacts client-i işləsin.

## `entity/Product.java`
```java
@Entity @Table(name = "products")
public class Product implements Serializable {
    @Id @GeneratedValue(IDENTITY) private Long id;
    @Column(nullable = false) private String name;
    private String brand, category, barcode;
    private Double calories, proteinG, fatG, carbsG;  // 100 qrama görə
    private boolean enriched;   // xarici API-dən doldurulubsa true
}
```
**① Sadə dildə**: Bir qida məhsulu. Adı, markası, 100 qramında neçə kalori/zülal/yağ/karb.

**② Texniki**: `implements Serializable` — **Redis cache üçün lazımdır** (obyekt byte-a çevrilib cache-ə yazılır). `enriched` — məlumatın mənbəyini göstərir.

**③ Müdafiədə**: "Product `Serializable`-dır çünki Redis-də cache olunur. Bütün qidalar 100 qram bazasında saxlanılır — log zamanı qramlıq faktoru ilə vurulur."

## `repo/ProductRepository.java`
```java
public interface ProductRepository extends JpaRepository<Product, Long>,
        JpaSpecificationExecutor<Product> {
    Optional<Product> findByBarcode(String barcode);
    List<Product> findByNameContainingIgnoreCase(String name);
}
```
**② Texniki**:
- `findByNameContainingIgnoreCase` — `WHERE LOWER(name) LIKE %?%` (böyük/kiçik hərf fərqsiz axtarış).
- `JpaSpecificationExecutor` — **dinamik sorğular** üçün (GraphQL-də istifadə olunur).

## `web/ProductController.java`
```java
@RestController @RequestMapping("/api/products")
public class ProductController {
    @GetMapping                          // bütün məhsullar
    @GetMapping("/search")               // ada görə axtarış (lokal → xarici)
    @GetMapping("/{id}") @Cacheable(...)  // ID ilə (CACHE olunur)
    @PostMapping                          // yeni məhsul (ADMIN)
    @PostMapping("/{id}/enrich")          // xarici API-dən doldur (ADMIN)
}
```
**① Sadə dildə**: Qida axtarışının qapısı. "alma" yazsan tapır. Bir məhsulu ID ilə istəsən — cache-dən sürətli verir.

**② Texniki**:
- `@Cacheable(value="products", key="#id")` — həmin ID üçün cavab Redis-də saxlanılır. 2-ci sorğu DB-yə getmir → sürətli.
- `@RequestHeader("X-User-Role")` ilə ADMIN yoxlaması (məhsul əlavə/enrich üçün).

**③ Müdafiədə**: "byId `@Cacheable` ilə Redis cache istifadə edir — eyni məhsul təkrar sorğusu DB-yə getmir. Axtarış əvvəlcə lokal DB, tapılmasa OpenFoodFacts-a düşür."

## `web/ProductGraphQL.java`
```java
@Controller
public class ProductGraphQL {
    @QueryMapping
    public List<Product> searchProducts(@Argument String name, @Argument String category,
            @Argument Double minCalories, @Argument Double maxCalories, @Argument Double minProtein) {
        Specification<Product> spec = Specification.where(null);
        if (name != null) spec = spec.and((r,q,cb) -> cb.like(...));
        // ... dinamik filter-lər
        return products.findAll(spec);
    }
}
```
**① Sadə dildə**: REST-dən fərqli bir sorğu üsulu. İstifadəçi **dəqiq nə istədiyini** deyir: "adında alma olan, 50-200 kalori arası, ən azı 5g zülallı məhsullar". GraphQL bu çevik axtarışı təmin edir.

**② Texniki**:
- **GraphQL** — REST alternativi. Bir endpoint (`/graphql`), sorğunu client formalaşdırır.
- `@QueryMapping` — GraphQL query handler.
- **Specification** — JPA Criteria API. Şərtlər dinamik yığılır (`spec.and(...)`) → tək SQL.
- Yalnız verilmiş filter-lər tətbiq olunur (null olanlar atlanır).

**③ Müdafiədə**: "GraphQL çoxparametrli axtarış üçün — client istədiyi filter kombinasiyasını göndərir. JPA Specification ilə dinamik `WHERE` şərtləri qurulur. REST-də hər kombinasiya üçün ayrı endpoint lazım olardı."

## `client/OpenFoodFactsClient.java`
```java
@FeignClient(name = "openfoodfacts", url = "${openfoodfacts.url}")
public interface OpenFoodFactsClient {
    @GetMapping("/api/v2/product/{barcode}.json")
    Map<String, Object> byBarcode(@PathVariable String barcode);

    @GetMapping("/cgi/search.pl?...")
    Map<String, Object> searchByName(@RequestParam("search_terms") String name, ...);
}
```
**① Sadə dildə**: Dünya qida bazası (OpenFoodFacts) ilə danışan telefon. Bizdə olmayan məhsulu oradan soruşur.

**② Texniki**:
- `@FeignClient` — HTTP client-i **interface** kimi yazırsan, Spring implementation-ı generate edir.
- `url` — xarici API ünvanı (`application.yml`-dən).
- Barcode və ad ilə axtarış.

**③ Müdafiədə**: "OpenFoodFacts inteqrasiyası Feign ilə — deklarativ HTTP client. Interface yazıram, Spring çağırışı özü edir. Barcode scan üçün real dünya məhsul bazasına çıxış."

## `client/FoodSearchService.java` ⭐ (Circuit Breaker)
```java
@Service
public class FoodSearchService {
    public List<Product> search(String name) {
        List<Product> local = products.findByNameContainingIgnoreCase(name);
        if (!local.isEmpty()) return local;    // 1) əvvəlcə lokal
        return searchExternal(name);           // 2) yoxdursa xarici
    }

    @CircuitBreaker(name = "openfoodfacts", fallbackMethod = "externalFallback")
    public List<Product> searchExternal(String name) {
        // OpenFoodFacts-dan gətir, parse et, DB-ə saxla (növbəti dəfə lokaldan gəlsin)
    }

    public List<Product> externalFallback(String name, Throwable t) {
        return List.of();   // API çökübsə boş qaytar, app dağılmasın
    }
}
```
**① Sadə dildə**: Ağıllı axtarış. Əvvəlcə öz bazasına baxır (sürətli). Yoxdursa dünya bazasından çəkir və **yadda saxlayır** (növbəti dəfə öz bazasında olsun). Əgər dünya bazası cavab vermirsə — boş qaytarır, tətbiq çökmür.

**② Texniki**:
- **Cache-aside strategiya**: lokal → xarici → lokala save.
- `@CircuitBreaker` (Resilience4j) — xarici API çox xəta versə "dövrəni açır", çağırışı dayandırır, `fallbackMethod` işə düşür. Bir servisin çökməsi bütün sistemi çökdürmür (**fault tolerance**).

**③ Müdafiədə**: "FoodSearchService cache-aside pattern istifadə edir — lokal, sonra xarici, sonra lokala yazır. `@CircuitBreaker` ilə OpenFoodFacts çökəndə fallback boş nəticə verir — cascade failure qarşısı alınır. Bu **resilience** pattern-idir."

## `client/EnrichmentService.java`
Eyni məntiq — barcode ilə naməlum məhsulun kalori məlumatını doldurur, `@CircuitBreaker` ilə qorunur.

## `config/DataSeeder.java` (Catalog)
```java
@Component
public class DataSeeder implements CommandLineRunner {
    private static final String CSV_PATH = "seed/products.csv";
    @Override public void run(String... args) {
        if (products.count() > 0) return;
        List<Product> seed = load();   // CSV oxu
        products.saveAll(seed);
    }
    private List<Product> load() {
        // resources/seed/products.csv-i oxu, sətir-sətir Product-a çevir
    }
}
```
**① Sadə dildə**: İlk işə düşəndə 238 qidanı CSV faylından oxuyub DB-yə yükləyir. Yeni qida əlavə etmək = CSV-yə sətir yazmaq (kod dəyişmir).

**② Texniki**: `BufferedReader` ilə CSV oxunur, başlıq atlanır, hər sətir `split(",")` ilə parse olunub Product-a çevrilir. Xəta tolerant (səhv sətir atlanır, log yazılır).

**③ Müdafiədə**: "Kataloq CSV-dən seed olunur — 238 məhsul. Data koddan ayrıdır (separation of concerns): yeni məhsul üçün yalnız CSV redaktə olunur, rebuild lazımdır amma kod dəyişmir."

---

# 5️⃣ NUTRITION SERVICE (Kalori Hesablama)

## `NutritionServiceApplication.java`
```java
@SpringBootApplication
@EnableFeignClients   // auth-service çağırışı üçün
public class NutritionServiceApplication { ... }
```

## `grpc/NutritionGrpcService.java` ⭐ (gRPC + Formullar)
```java
@GrpcService
public class NutritionGrpcService extends NutritionCalculationServiceGrpc.NutritionCalculationServiceImplBase {
    @Override
    public void calculateGoal(CalculateGoalRequest req, StreamObserver<CalculateGoalResponse> observer) {
        // BMR (Mifflin-St Jeor)
        double bmr = 10*req.getWeightKg() + 6.25*req.getHeightCm()
                   - 5*req.getAge() + (req.getSex()==Sex.MALE ? 5 : -161);
        // TDEE = BMR × aktivlik
        double multiplier = switch (req.getActivityLevel()) {
            case SEDENTARY -> 1.2; case LIGHT -> 1.375;
            case MODERATE -> 1.55; case ACTIVE -> 1.725; default -> 1.9;
        };
        double tdee = bmr * multiplier;
        // Məqsəd
        double daily = switch (req.getGoal()) {
            case LOSE -> tdee*0.85; case GAIN -> tdee*1.15; default -> tdee;
        };
        // Makrolar
        double proteinG = req.getWeightKg() * 1.8;
        double fatG = daily * 0.25 / 9;
        double carbsG = (daily - proteinG*4 - fatG*9) / 4;
        observer.onNext(CalculateGoalResponse.newBuilder()...build());
        observer.onCompleted();
    }
}
```
**① Sadə dildə**: Şəxsi qidalanma məsləhətçisi. Yaş, çəki, boy, cins, aktivlik alır → gündəlik neçə kalori lazım olduğunu hesablayır. Formullar tibbi ədəbiyyatdan (Mifflin-St Jeor).

**② Texniki**:
- `@GrpcService` — bu **gRPC server** metodudur (REST deyil).
- **BMR** — istirahətdə yanan enerji. **TDEE** — aktivliklə birlikdə ümumi.
- Məqsəd: arıqla (−15%), saxla (0%), kök al (+15%).
- Makro: protein=çəki×1.8, yağ=25% kaloridən, karb=qalan.
- `StreamObserver` — gRPC-nin cavab qaytarma üsulu (`onNext` → `onCompleted`).

**③ Müdafiədə**: "Kalori hesablaması gRPC ilə — Mifflin-St Jeor formulu ilə BMR, aktivlik əmsalı ilə TDEE, məqsədə görə düzəliş. gRPC seçdim çünki bu, servislərarası sırf hesablama çağırışıdır — binary protokol REST-dən sürətlidir. Protocol Buffers ilə tип-güvənli."

## `web/GoalController.java`
```java
@RestController @RequestMapping("/api/goals")
public class GoalController {
    @GrpcClient("nutrition")
    private NutritionCalculationServiceGrpc.NutritionCalculationServiceBlockingStub stub;

    @PostMapping("/calculate")
    public Map<String, Double> calculate(@RequestBody GoalRequest req) {
        CalculateGoalResponse resp = stub.calculateGoal(...);
        return Map.of("bmr", resp.getBmr(), "tdee", resp.getTdee(), ...);
    }
}
```
**① Sadə dildə**: Frontend REST istəyir, amma hesablama gRPC-dədir. Bu class "tərcüməçi"-dir — REST istəyini gRPC-yə çevirir, cavabı JSON kimi qaytarır.

**② Texniki**: `@GrpcClient` gRPC stub inject edir. Controller REST → gRPC bridge rolunu oynayır. Frontend gRPC bilmir, ona görə REST wrapper.

**③ Müdafiədə**: "GoalController REST-to-gRPC bridge-dir. Brauzer gRPC edə bilmir, ona görə frontend-ə REST verirəm, daxildə gRPC stub-a çağırıram. Bu, protokol adaptasiyasıdır."

## `entity/DietPlan.java`
```java
@Entity @Table(name = "diet_plans")
public class DietPlan {
    Long id, patientId, doctorId;
    Double dailyCalorieTarget, proteinG, fatG, carbsG;
    String notes;
    LocalDate startDate, endDate;
    boolean active;
}
```
**① Sadə dildə**: Həkimin xəstə üçün yazdığı qidalanma planı. "Gündə 2200 kalori, 150g zülal, tarix aralığı, qeydlər".

**② Texniki**: Həkim yaradır, xəstəyə bağlıdır (`patientId`, `doctorId`). `active` — yalnız bir aktiv plan olur.

## `repo/DietPlanRepository.java`
```java
Optional<DietPlan> findFirstByPatientIdAndActiveTrueOrderByIdDesc(Long patientId);
List<DietPlan> findAllByPatientIdAndActiveTrue(Long patientId);
List<DietPlan> findByDoctorId(Long doctorId);
```
**② Texniki**: `findFirst...OrderByIdDesc` — **ən son aktiv plan** (bir neçə aktiv olsa belə çökmür). Bu, defensive design.

## `web/DietPlanController.java`
```java
@PostMapping   // həkim plan yaradır — yalnız ÖZ xəstəsinə
public DietPlan create(...) {
    requireRole(role, "DOCTOR");
    // authClient.getUser(patientId) → bu xəstə həqiqətən bu həkimindirmi? yoxla
    // köhnə aktiv planları deaktiv et
    // yeni planı active=true ilə save et
}
@GetMapping("/my")               // xəstə öz planını görür
@GetMapping("/patient/{id}")     // həkim xəstənin planını görür
@GetMapping("/mine-as-doctor")   // həkim öz yazdığı planlar
```
**① Sadə dildə**: Həkim yalnız **öz** xəstəsinə plan yaza bilər. Yeni plan yaradanda köhnəsi avtomatik ləğv olunur.

**② Texniki**: `authClient.getUser()` (Feign) ilə xəstənin `doctorId`-si yoxlanılır — başqasının xəstəsinə plan yaza bilməzsən (authorization). Yeni plan köhnələri deaktiv edir (bir aktiv plan invariantı).

**③ Müdafiədə**: "DietPlanController-də həkim yalnız öz xəstəsinə plan yaza bilər — auth-service-dən xəstənin doctorId-sini Feign ilə çəkib yoxlayıram. Yeni plan köhnəni deaktiv edir ki, həmişə tək aktiv plan olsun."

## `web/AuthClient.java`
```java
@FeignClient(name = "auth-service")
public interface AuthClient {
    @GetMapping("/api/auth/internal/users/{id}")
    Map<String, Object> getUser(@PathVariable Long id);
}
```
**② Texniki**: Nutrition → Auth çağırışı. Eureka service name (`auth-service`) ilə tapır, load balancing edir.

---

# 6️⃣ TRACKING SERVICE (Əsas — Qeyd, Hesabat, Real-time, Chat)

## `TrackingServiceApplication.java`
`@SpringBootApplication @EnableFeignClients` — 3 servisi çağırır (auth, catalog, nutrition).

## Entity-lər

### `entity/FoodLog.java`
```java
@Entity @Table(name = "food_logs")
public class FoodLog {
    Long id, userId, productId;
    String productName; Double grams;
    Double calories, proteinG, fatG, carbsG;
    LocalDate logDate; Instant createdAt;
}
```
**① Sadə dildə**: Bir qida qeydi — "5 nömrəli istifadəçi bu gün 150g toyuq yedi = 247 kalori".

### `entity/DailySummary.java`
```java
@Entity @Table(name = "daily_summaries",
    uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "day"}))
public class DailySummary {
    Long id, userId; LocalDate day;
    Double calories, proteinG, fatG, carbsG;
    Double targetCalories; Long doctorId;
}
```
**① Sadə dildə**: Günün ümumi hesabı — "5 nömrəli istifadəçi bu gün cəmi 1850 kalori aldı, hədəf 2000 idi".
**② Texniki**: `@UniqueConstraint(userId, day)` — bir istifadəçi bir gün üçün **yalnız bir** summary. Təkrar qeyd olsa DB rədd edir.

### `entity/Alert.java`
Həkimə xəbərdarlıq — "xəstə kalori limitini keçdi" (doctorId, patientId, percent, tarix).

### `entity/ChatMessage.java`
Həkim-xəstə mesajı (senderId, recipientId, content, createdAt).

## Repositories
```java
// FoodLogRepository
List<FoodLog> findByUserIdAndLogDate(Long userId, LocalDate date);

// DailySummaryRepository
Optional<DailySummary> findByUserIdAndDay(Long userId, LocalDate day);
List<DailySummary> findByUserIdAndDayBetweenOrderByDayAsc(...);  // qrafik üçün

// AlertRepository
List<Alert> findTop50ByDoctorIdOrderByCreatedAtDesc(Long doctorId);  // son 50 alert

// ChatMessageRepository — custom @Query
@Query("SELECT m FROM ChatMessage m WHERE (senderId=:a AND recipientId=:b) OR (...) ORDER BY createdAt")
List<ChatMessage> conversation(Long a, Long b);
```
**① Sadə dildə**: DB sorğuları. "Bu istifadəçinin bugünkü qeydləri", "iki nəfər arasındakı bütün söhbət" və s.
**② Texniki**: `conversation` — custom JPQL query, iki istiqamətli söhbət (a→b VƏ b→a).

## `web/FoodLogController.java` ⭐
```java
@PostMapping   // qida əlavə et
public FoodLog log(@RequestHeader("X-User-Id") Long userId, @RequestBody LogRequest req) {
    Map<String, Object> p = catalog.product(req.productId());   // Catalog-dan məhsulu al
    double factor = req.grams() / 100.0;                        // qramlıq faktoru
    FoodLog entry = FoodLog.builder()
            .calories(num(p.get("calories")) * factor)          // 100g → real qram
            .proteinG(num(p.get("proteinG")) * factor)
            ... .build();
    logs.save(entry);
    summaryService.apply(userId, entry);    // ⭐ summary yenilə + WebSocket push
    return entry;
}
@GetMapping("/today")                       // bugünkü qeydlər
@GetMapping("/patient/{id}/today")          // həkim: xəstənin bugünkü qeydləri
```
**① Sadə dildə**: Qida əlavə etmənin əsas qapısı. "150g toyuq" deyirsən → Catalog-dan toyuğun 100g-lıq məlumatını çəkir → 1.5-ə vurur → qeyd edir → günün hesabını yeniləyir → ekranı real-time yeniləyir.

**② Texniki**:
- `catalog.product()` (Feign) — məhsulun 100g məlumatı.
- `factor = grams/100` — real qramlığa uyğunlaşdırma.
- `summaryService.apply()` — biznes məntiqin ürəyi (aşağıda).

**③ Müdafiədə**: "Qida qeyd olunanda Catalog-dan məhsulun 100g məlumatını Feign ilə alıram, qramlıq faktoru ilə vururam, log-u saxlayıram, sonra SummaryService günün cəmini yeniləyir və WebSocket ilə frontend-i canlı yeniləyir."

## `service/SummaryService.java` ⭐⭐⭐ (ƏN VACİB CLASS)
```java
@Service
public class SummaryService {
    @Transactional
    public DailySummary apply(Long userId, FoodLog entry) {
        // 1. Bugünkü summary tap, yoxdursa yarat
        DailySummary summary = summaries.findByUserIdAndDay(userId, day)
                .orElseGet(() -> newSummary(userId, day));
        // 2. Kalori + makroları TOPLA
        summary.setCalories(summary.getCalories() + safe(entry.getCalories()));
        // ... protein, fat, carbs
        DailySummary saved = summaries.save(summary);
        // 3. WebSocket push
        push(saved);
        return saved;
    }

    private DailySummary newSummary(Long userId, LocalDate day) {
        Map<String, Object> plan = plans.activePlan(userId);   // Nutrition-dan hədəf çək
        return DailySummary.builder()
                .calories(0d)...
                .targetCalories(asDouble(plan.get("dailyCalorieTarget")))
                .doctorId(asLong(plan.get("doctorId")))
                .build();
    }

    private void push(DailySummary s) {
        double percent = 100.0 * s.getCalories() / target;
        String level = percent >= 100 ? "LIMIT" : percent >= 80 ? "WARN" : "OK";
        // İstifadəçiyə canlı yenilə
        ws.convertAndSendToUser(userId, "/queue/calories", Map.of("percent", ..., "level", level));
        // Limit aşılıb + həkim var → ALERT
        if (percent >= 100 && s.getDoctorId() != null) {
            alerts.save(Alert.builder()...);                         // DB-ə yaz
            ws.convertAndSendToUser(doctorId, "/queue/alerts", ...);  // həkimə canlı
        }
    }
    private double safe(Double v) { return v == null ? 0 : v; }
}
```
**① Sadə dildə**: Tətbiqin **beyni**. Hər qida əlavəsində: bugünkü hesabı tapır (yoxsa yaradıb Nutrition-dan hədəfi çəkir), yeni kalorini toplayır, saxlayır, ekranı canlı yeniləyir. Əgər gündəlik limit aşılıbsa və istifadəçinin həkimi varsa — həkimə avtomatik xəbərdarlıq göndərir.

**② Texniki**:
- `@Transactional` — bütün əməliyyat atomik (hamısı olur, ya heç biri).
- `orElseGet(newSummary)` — lazy: summary yoxdursa yaradılır.
- **Level məntiq**: <80% OK, 80-99% WARN, ≥100% LIMIT.
- `convertAndSendToUser` — STOMP ilə **konkret istifadəçiyə** push (userId Principal ilə).
- Alert **həm DB-ə** (həkim sonra görsün), **həm canlı** (onlaynsa dərhal görsün).
- `safe()` — null-ları 0-a çevirir (NPE qarşısı).

**③ Müdafiədə**: "SummaryService biznes məntiqin mərkəzidir, `@Transactional`. Qida əlavəsində günlük cəmi yığır, Nutrition-dan hədəfi çəkir, faizi hesablayır (OK/WARN/LIMIT), WebSocket ilə istifadəçini canlı yeniləyir. Limit aşılanda həkimə həm DB-ə yazıb, həm canlı push edir — timing-dən asılı olmasın deyə ikisini də. Bu class-ın 8 unit testi var."

## `service/NutritionPlanClient.java`
```java
@CircuitBreaker(name = "nutrition", fallbackMethod = "fallback")
public Map<String, Object> activePlan(Long userId) {
    return nutrition.activePlan(userId);
}
public Map<String, Object> fallback(Long userId, Throwable t) {
    return Map.of();   // Nutrition çökübsə boş plan (hədəfsiz davam et)
}
```
**① Sadə dildə**: Nutrition-service-dən hədəf çəkir. Əgər o servis çökübsə — boş qaytarır, tətbiq işləməyə davam edir (hədəf göstərilməz sadəcə).
**②③ Müdafiədə**: "Circuit breaker ilə qorunur — Nutrition çökəndə fallback boş plan verir, qida qeydi yenə işləyir, sadəcə hədəf/faiz olmur. Graceful degradation."

## `service/ChatAccessService.java`
```java
public boolean canChat(Long meId, Long peerId) {
    // auth-service-dən hər iki tərəfin məlumatı
    // ADMIN → hər kəslə
    // DOCTOR → hər kəslə (öz mesajı xəstəyə çatsın)
    // USER ↔ DOCTOR → xəstə Pro olmalı VƏ doctorId uyğun olmalı
    // xəta olsa → false (təhlükəsiz default)
}
```
**① Sadə dildə**: Chat icazə yoxlayıcısı. Adi istifadəçi yalnız **öz həkimi** ilə və yalnız **Pro** abunəçidirsə yaza bilər.

**② Texniki**: Auth-service-dən rol/pro/doctorId çəkir. Fail-safe: xəta olsa icazə **vermir** (deny by default).

**③ Müdafiədə**: "Chat icazəsi ChatAccessService-də — USER yalnız Pro-dursa və öz təyin olunmuş həkimi ilə yaza bilər. Auth-service-dən doğrulanır. Xəta olsa deny edir — fail-secure."

## `web/ChatController.java` ⭐ (WebSocket + REST)
```java
@MessageMapping("/chat.send")   // WebSocket: mesaj göndər
public void send(@Payload ChatIn in, Principal principal) {
    Long senderId = Long.valueOf(principal.getName());   // JWT-dən userId
    if (!access.canChat(senderId, in.recipientId())) { /* icazə yoxdur */ return; }
    ChatMessage saved = messages.save(...);              // DB-ə yaz
    ws.convertAndSendToUser(recipientId, "/queue/chat", out);  // alıcıya
    ws.convertAndSendToUser(senderId, "/queue/chat", out);     // özünə (echo)
}
@GetMapping("/{peerId}")        // REST: keçmiş söhbət
public List<ChatMessage> history(...) { ... }
```
**① Sadə dildə**: Canlı söhbət. Mesaj yazanda dərhal həm qarşı tərəfə, həm özünə göndərilir (WhatsApp kimi). Köhnə mesajları REST ilə yükləyir.

**② Texniki**:
- `@MessageMapping` — WebSocket mesaj handler (REST `@PostMapping`-in WebSocket qarşılığı).
- `Principal` — JWT-dən gələn userId (JwtChannelInterceptor təyin edir).
- İki `convertAndSendToUser` — alıcıya və göndərənin digər cihazlarına.

**③ Müdafiədə**: "Chat WebSocket üzərində — `@MessageMapping` real-time mesaj alır, DB-yə yazır, hər iki tərəfə push edir. Principal JWT-dən gəlir. Keçmiş yazışma REST ilə. Hibrid: canlı üçün WS, tarixçə üçün REST."

## `ws/WebSocketConfig.java` ⭐
```java
@Configuration @EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
    }
    @Override public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue", "/topic");
        registry.setUserDestinationPrefix("/user");
        registry.setApplicationDestinationPrefixes("/app");
    }
    @Override public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(jwtChannelInterceptor);   // ⭐ JWT auth
    }
}
```
**① Sadə dildə**: Canlı bağlantının qaydalarını qurur. Frontend `/ws` ünvanına bağlanır. Mesajlar `/user/queue/...` kanalları ilə konkret istifadəçilərə gedir.

**② Texniki**:
- **STOMP** — WebSocket üzərində mesaj protokolu.
- `/ws` — handshake endpoint, SockJS fallback (WebSocket bloklanarsa polling).
- `enableSimpleBroker` — in-memory message broker (`/queue` şəxsi, `/topic` broadcast).
- `/user` prefix — `convertAndSendToUser` üçün.
- `/app` — client→server mesajları (`@MessageMapping`).
- **Interceptor** — hər CONNECT-də JWT yoxlanır.

**③ Müdafiədə**: "WebSocket STOMP ilə. `/ws` endpoint, SockJS fallback. Simple in-memory broker istifadə edirəm. `/user` prefix hər istifadəçiyə şəxsi kanal verir. JwtChannelInterceptor CONNECT-də auth edir."

## `ws/JwtChannelInterceptor.java` ⭐
```java
@Component
public class JwtChannelInterceptor implements ChannelInterceptor {
    @Override public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String header = accessor.getFirstNativeHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                Claims claims = Jwts.parser().verifyWith(key).build()
                        .parseSignedClaims(header.substring(7)).getPayload();
                accessor.setUser(new StompPrincipal(claims.getSubject()));  // userId → Principal
            }
        }
        return message;
    }
    private record StompPrincipal(String name) implements Principal { ... }
}
```
**① Sadə dildə**: WebSocket üçün qapıçı. Frontend canlı bağlantı qurmaq istəyəndə tokenini yoxlayır, "bu 5 nömrəli istifadəçidir" deyib qeyd edir. Bu qeyd sayəsində mesajları düzgün adama göndərə bilirik.

**② Texniki**:
- Gateway HTTP filter WebSocket-ə işləmir — ona görə **ayrı** JWT yoxlaması.
- `CONNECT` frame-də `Authorization` header oxunur.
- Token doğrulanıb `Principal` (userId) təyin olunur.
- Bu Principal `convertAndSendToUser(userId, ...)` və `@MessageMapping`-də `principal.getName()` üçün lazımdır.

**③ Müdafiədə**: "WebSocket auth ayrıdır çünki Gateway HTTP filter-i WS mesajlarına şamil olmur. JwtChannelInterceptor STOMP CONNECT frame-də tokeni yoxlayır, userId-ni Principal kimi təyin edir — bu, hər istifadəçiyə şəxsi mesaj göndərməyə imkan verir."

## `web/CatalogClient`, `NutritionClient`, `AuthClient`
3 Feign interface — Tracking digər servisləri belə çağırır:
```java
@FeignClient(name = "catalog-service")  // məhsul məlumatı
@FeignClient(name = "nutrition-service") // aktiv plan
@FeignClient(name = "auth-service")      // user rol/pro/doctorId
```
Eureka service name ilə tapır, load balancing edir.

## `web/SummaryController.java`
```java
@GetMapping("/history")   // son N günün kaloriləri (qrafik üçün, boş günlər 0)
@GetMapping("/alerts")    // həkimin son 50 xəbərdarlığı
@GetMapping("/today")     // bugünkü hesabat + faiz + level
@GetMapping("/patient/{id}/today")  // həkim: xəstənin bugünkü hesabatı
```
**① Sadə dildə**: Hesabatları göstərir. Qrafik üçün son 7-30 günün məlumatı, həkim üçün xəbərdarlıqlar, bugünkü ümumi vəziyyət.
**② Texniki**: `history` — boş günləri 0-la doldurur (qrafik davamlı olsun). `toView` — SummaryService.push-dakı eyni level məntiq (OK/WARN/LIMIT).

---

# 🎯 ÜMUMİ PATTERN-LƏR (müəllim soruşa bilər)

| Pattern | Harada | Nə üçün |
|---------|--------|---------|
| **Repository** | bütün `repo/` | DB-ni məntiqidən ayırır |
| **DTO (record)** | `RegisterRequest`, `LogRequest` | API-ə giriş/çıxış obyekti |
| **Feign Client** | servislərarası | deklarativ HTTP |
| **Circuit Breaker** | FoodSearch, NutritionPlanClient | fault tolerance |
| **Cache-aside** | FoodSearchService | performans |
| **Centralized Auth** | Gateway JwtAuthFilter | təhlükəsizlik |
| **Schema-per-service** | 4 schema | data isolation |
| **Builder** | bütün entity | təmiz obyekt yaratma |
| **@Transactional** | SummaryService | atomiklik |
| **Fail-secure** | ChatAccessService | xəta = deny |
| **Graceful degradation** | fallback metodlar | servis çöksəydə davam |

---

# ❓ MÜDAFİƏDƏ ÇƏTİN SUALLAR

**S: Bir istifadəçi başqasının məlumatını görə bilərmi?**
C: Xeyr. Gateway tokendən `X-User-Id` qoyur, controller-lər bu id ilə işləyir. Həkim endpoint-ləri əlavə `X-User-Role` + xəstə mülkiyyəti yoxlaması edir (DietPlan, ChatAccess).

**S: Token oğurlansa?**
C: 12 saat expiry var. HTTPS ilə şifrələnir (yol boyu oxunmaz). Prod-da refresh token + qısa expiry əlavə edilə bilər.

**S: İki servis eyni DB cədvəlinə girirmi?**
C: Xeyr. Hər servis öz schema-sına. Başqa servisin datası lazımsa — Feign HTTP çağırışı (məs. Tracking → Auth `getUser`).

**S: gRPC və Feign fərqi?**
C: Feign — REST üzərində HTTP/JSON (servisdən-servisə REST). gRPC — binary HTTP/2 (yalnız Nutrition hesablama). gRPC sürətli amma quraşdırma mürəkkəb; Feign sadə.

**S: WebSocket necə auth olunur?**
C: HTTP Gateway filter WS-ə işləmir. Ayrı `JwtChannelInterceptor` STOMP CONNECT-də tokeni yoxlayır.

**S: Bir servis çöksəydə?**
C: Circuit breaker + fallback. Məs. Nutrition çöksəydə qida qeydi yenə işləyir (hədəfsiz). Eureka çökən instance-ı siyahıdan çıxarır.

**S: Niyə DailySummary-də unique constraint?**
C: Bir istifadəçi bir gün üçün yalnız bir hesabat olmalı. Race condition-da DB-səviyyəsində qorunur.
