# AzərKalori — Layihəni Necə Qurdum (Class-by-Class Qurulma Jurnalı)

> Bu sənəd layihəni **hansı ardıcıllıqla** qurduğumu izah edir: hansı class-dan başladım, hər class-da hansı dəyişənləri/metodları/arqumentləri **niyə** yaratdım.
>
> Hər addımda: **🔧 TEXNİKİ SƏBƏB** (developer dilində) → **💡 FEYNMAN** (sadə, gündəlik dildə).
>
> Qurulma məntiqinin qızıl qaydası: **aşağıdan yuxarı** — əvvəl təməl (kimsə hamını tapsın: Eureka), sonra data (entity), sonra sorğu (repo), sonra məntiq (service), sonra qapı (controller), ən sonda birləşdirici (gateway, frontend).

---

# MƏRHƏLƏ 0 — Layihənin skeleti (niyə multi-module Gradle?)

**Başladığım yer**: `settings.gradle` + `build.gradle` (kök).

🔧 **TEXNİKİ SƏBƏB**: 6 ayrı servis bir repo-da (monorepo). Hər servis öz `build.gradle`-ına malikdir, amma kök `settings.gradle` hamısını bir yerə yığır (`include 'auth-service', 'catalog-service'...`). Beləliklə `./gradlew build` bir komandayla hamısını build edir. Ortaq versiyalar (Spring Boot, Java 17) kökdə mərkəzləşir — təkrar yoxdur.

💡 **FEYNMAN**: Bir binada 6 mənzil tikirəm. Hər mənzilin öz planı var, amma binanın ümumi bünövrəsi birdir. Bir düymə basıram — bütün mənzillər eyni anda tikilir.

---

# MƏRHƏLƏ 1 — DISCOVERY SERVER (niyə ƏN ƏVVƏL?)

**İlk servis: `discovery-server`**. Çünki qalan hər kəs ona qeydiyyatdan keçəcək — o olmadan servislər bir-birini tapa bilməz.

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

🔧 **TEXNİKİ SƏBƏB**: `@EnableEurekaServer` yazdım — bu tək annotasiya servisi registry-yə çevirir. `main()` metodu Spring context-i qaldırır. `application.yml`-də `port: 8761` (Eureka-nın standart portu), `register-with-eureka: false` (özünü qeyd etməsin, çünki O registry-dir), `fetch-registry: false`. Heç bir entity/controller yazmadım — çünki bu servisin işi kod deyil, infrastrukturdur.

💡 **FEYNMAN**: Yeni ofis binasına ilk gətirdiyim adam **reception**-dur. Hələ heç kim yoxdur, amma o oturmalıdır ki, sonra gələnlər "mən buradayam" deyə bilsinlər. Onun özünün işi sadədir — sadəcə "kim hardadır" siyahısını tutmaq.

---

# MƏRHƏLƏ 2 — AUTH SERVICE (niyə ikinci? Çünki hər şey istifadəçidən başlayır)

İstifadəçi olmadan nə qida qeydi, nə plan var. Ona görə əvvəl **kim var** sualını həll edirəm.

Auth-service-i **daxildən-çölə** qurdum: əvvəl data (Role, User), sonra sorğu (Repository), sonra token məntiqi (JwtService), sonra qapı (Controller).

## Addım 2.1 — `entity/Role.java` (ən kiçik parçadan başla)
```java
public enum Role { USER, DOCTOR, ADMIN }
```
🔧 **TEXNİKİ SƏBƏB**: İlk bunu yaratdım çünki `User` entity-si `Role`-a bağlıdır — asılılıq sırası. Enum seçdim (String yerinə) çünki rol dəyərləri **məhduddur və sabitdir** — 3 dəyər. Enum compile-time təhlükəsizliyi verir (səhv rol yaza bilməzsən). DB-də `@Enumerated(EnumType.STRING)` ilə oxunaqlı saxlanılır.

💡 **FEYNMAN**: Klubun 3 növ üzvlük kartı var: adi, həkim, admin. Bunları əvvəldən müəyyən edirəm ki, sonra "sən hansı üzvsən?" sualına yalnız bu 3-dən biri cavab ola bilsin — "filankəs" kimi qarışıq bir söz yox.

## Addım 2.2 — `entity/User.java` (əsas data modeli)
```java
@Entity @Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true) private String email;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) private String password;
    private String fullName;
    @Enumerated(EnumType.STRING) private Role role;
    private Long doctorId;
    private boolean pro; private Instant proUntil;
    private boolean approved;
    private Integer age; private Double weightKg, heightCm;
    private String sex, activityLevel;
}
```

🔧 **TEXNİKİ SƏBƏB** — hər sahəni niyə əlavə etdim:
- `id` (Long, IDENTITY) — primary key, DB auto-increment. Long çünki milyonlarla user ola bilər.
- `email` (`unique=true`) — login identifikatoru; təkrarlana bilməz.
- `password` (`WRITE_ONLY`) — **qəsdən** JSON cavabından gizlətdim; parol heç vaxt API-dən çıxmamalıdır. DB-də BCrypt hash olacaq.
- `role` — authorization üçün.
- `doctorId` — xəstəni həkimə bağlamaq üçün (foreign key məntiqi, amma sadə Long saxladım çünki başqa schema-dadır).
- `pro` + `proUntil` — Stripe abunəsi üçün; sonradan Billing əlavə edəndə lazım oldu.
- `approved` — həkim qeydiyyatının admin təsdiqi üçün.
- `age, weightKg, heightCm, sex, activityLevel` — bunları əlavə etdim çünki BMR hesablaması (Nutrition) bu 5 dəyəri tələb edir. Əvvəldən User-ə qoydum ki, hesablama zamanı əlimdə olsun.

Lombok (`@Getter @Builder...`) — 50 sətir getter/setter/constructor əl ilə yazmamaq üçün.

💡 **FEYNMAN**: Bir üzvlük kartı düzəldirəm. Üstündə: nömrə, email, şifrə (amma şifrəni **görünməz mürəkkəblə** yazıram — heç kim oxuya bilməsin), adı, üzvlük növü, həkimi kimdir, premium üzvdürmü, yaşı-çəkisi (bunları yazıram çünki sonra "sənə gündə nə qədər kalori lazımdır" hesablayarkən lazım olacaq). Kartı bir dəfə tam düzəldirəm ki, sonra hər dəfə əlavə etməyim.

## Addım 2.3 — `repo/UserRepository.java` (data-ya çıxış)
```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    List<User> findByDoctorId(Long doctorId);
    List<User> findByRole(Role role);
    List<User> findByApprovedFalse();
}
```

🔧 **TEXNİKİ SƏBƏB**: User yaratdıqdan sonra ona DB-də **çatmaq** lazımdır. `JpaRepository` extends etdim → `save/findById/findAll/delete` pulsuz gəlir. Öz metodlarımı **ehtiyacdan** yaratdım:
- `findByEmail` — login üçün (email ilə user tap).
- `findByDoctorId` — həkimin xəstələrini siyahılamaq üçün.
- `findByRole` — admin bütün həkimləri görsün.
- `findByApprovedFalse` — admin təsdiq gözləyənləri görsün.
Metod adlarını Spring SQL-ə çevirir — mən SQL yazmıram. Hər metodu yalnız bir endpoint-ə ehtiyac olanda əlavə etdim (YAGNI prinsipi).

💡 **FEYNMAN**: Kartları saxladığım şkaf var. Şkafa "email-i bu olan kartı gətir" desəm, tapıb gətirir. Mən hər axtarış növü üçün bir "əmr" yazıram — amma yalnız həqiqətən lazım olanları. Boş yerə "saçının rənginə görə tap" yazmıram, çünki heç vaxt lazım olmayacaq.

## Addım 2.4 — `security/JwtService.java` (kimliyi sübut edən token)
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
                .subject(String.valueOf(user.getId()))
                .claim("role", user.getRole().name())
                .claim("email", user.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(12, ChronoUnit.HOURS)))
                .signWith(key).compact();
    }
}
```

🔧 **TEXNİKİ SƏBƏB**: İstifadəçi login olandan sonra hər sorğuda parol göndərmək **olmaz** (təhlükəsiz deyil, yavaş). Ona görə JWT token yaradıram.
- Constructor arqumenti `secret` — `@Value` ilə `application.yml`-dən oxunur; kodda **hard-code etmədim** (təhlükəsizlik). Bu secret-dən HMAC-SHA `key` qururam.
- `issue(User)` metodu — arqument User çünki tokenin içinə userId, role, email qoyacam.
- `subject` = userId — "bu token kimindir".
- `claim("role")` — sonra Gateway rol yoxlaması üçün.
- `expiration(12 saat)` — token əbədi qalmamalı (oğurlanarsa zərər məhdud).
- `signWith(key)` — imza. Kimsə tokeni dəyişsə imza uyğunsuz olar.

💡 **FEYNMAN**: Üzv girişdən keçəndən sonra ona **möhürlü bir bilet** verirəm. Biletdə: "bu 5 nömrəli üzvdür, həkimdir, 12 saat etibarlıdır". Möhür mənim gizli möhürümdür — kimsə bileti saxtalaşdırsa, möhür tutmaz və biletti rədd edərəm. Beləliklə üzv hər dəfə şifrəsini deməli olmur, sadəcə biletini göstərir.

## Addım 2.5 — `security/SecurityConfig.java` (parol necə gizlədilir)
```java
@Configuration
public class SecurityConfig {
    @Bean public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http.csrf(c -> c.disable())
                   .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
                   .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                   .build();
    }
}
```

🔧 **TEXNİKİ SƏBƏB**: JwtService-i yazdıqdan sonra parolları **necə saxlayacağımı** həll etməliyəm. `BCryptPasswordEncoder` bean yaratdım — bunu AuthController inject edəcək. `csrf.disable()` çünki cookie yox, token istifadə edirəm. `STATELESS` — Spring session yaratmasın (JWT var). `permitAll()` — bu servisdə auth yoxlaması etmirəm, çünki real yoxlama **Gateway-də** olacaq (təkrar iş görməyim).

💡 **FEYNMAN**: İki qayda qururam: (1) parolları **əzmə maşınından** keçirirəm — geri düzəldilə bilməyən şəkildə əzilir; DB oğurlansa belə parollar oxunmaz. (2) Bu binada qapıçı yoxdur — çünki qapıçı əsas girişdə (Gateway) dayanacaq, hər mənzildə ayrıca qapıçı lazım deyil.

## Addım 2.6 — `web/AuthController.java` (giriş/qeydiyyat qapısı)

🔧 **TEXNİKİ SƏBƏB**: İndi bütün parçalar hazırdır (User, Repository, JwtService, encoder) — onları **birləşdirən qapı** yaradıram. `@RestController` + `@RequestMapping("/api/auth")`. Dependency-ləri `@RequiredArgsConstructor` ilə inject etdim (users, encoder, jwt, mail).
- `register()` — arqument `RegisterRequest` (bir `record` DTO). Niyə record? Çünki bu, sadəcə giriş datası daşıyır — immutable, qısa. Metod içində: email təkrar yoxla → parolu **encode et** → rol seç (həkimsə approved=false) → save → email göndər → token qaytar (yalnız təsdiqli user-ə).
- `login()` — `encoder.matches(gələn, hash)` ilə parol yoxla, uğurlu olsa `jwt.issue()`.
- `admin/*` metodları — `X-User-Role` header ilə (Gateway qoyur) rol yoxlaması.
- `internal/users/{id}` — bunu **sonradan** əlavə etdim, Nutrition və Tracking servisləri user məlumatını Feign ilə çəkməli olanda.

💡 **FEYNMAN**: İndi bütün alətlərim var — kart şkafı, əzmə maşını, bilet çapı. Bunları bir **gişəyə** yığıram. Gişədə: yeni gələn qeydiyyatdan keçir (şifrəsi əzilir, xoş gəldin məktubu gedir, bilet verilir), köhnə üzv giriş edir (şifrəsi yoxlanır, bilet verilir). Admin üçün ayrı pəncərələr — "həkimləri təsdiqlə", "xəstəyə həkim təyin et".

## Addım 2.7 — `mail/MailService.java` (sonradan əlavə — "yaxşı olar" xüsusiyyət)
🔧 **TEXNİKİ SƏBƏB**: Əsas axın işlədikdən sonra "user experience" üçün xoş gəldin məktubu əlavə etdim. `@Async` qoydum çünki SMTP yavaşdır — qeydiyyat cavabını gözlətməsin. try-catch ilə xəta udulur — email getməsə qeydiyyat sınmasın (email kritik deyil).

💡 **FEYNMAN**: Qeydiyyatı bitirmək üçün email vacib deyil — ona görə məktubu **poçtalyona** verirəm (arxa fonda getsin), üzvü gözlətmirəm. Poçt itsə də üzvlük qalır.

## Addım 2.8 — `config/DataSeeder.java` (test datası)
🔧 **TEXNİKİ SƏBƏB**: Boş DB ilə test etmək çətindir. `CommandLineRunner` ilə start-da 3 user yaradıram (admin/doctor/user). `if (count>0) return` — idempotent, təkrar yaratmır.

💡 **FEYNMAN**: Yeni açılan klubda heç kim yoxdursa test edə bilmərəm. Ona görə 3 "nümunə üzv" qoyuram ki, dərhal giriş edib yoxlaya bilim.

## Addım 2.9 — `web/BillingController.java` (ən sonda — Pro abunə)
🔧 **TEXNİKİ SƏBƏB**: Bu, əlavə xüsusiyyətdir, ona görə axıra saxladım. Stripe Checkout inteqrasiyası. `checkout()` sessiya yaradır, `webhook()` ödəniş təsdiqini imza yoxlaması ilə qəbul edir, user.pro=true edir. `simulate()` — demo üçün ödənişsiz Pro. `@Value` ilə Stripe açarları config-dən.

💡 **FEYNMAN**: Klub işlədikdən sonra "VIP üzvlük" satışı əlavə edirəm. Pulu **bank** (Stripe) alır — mən kart nömrəsinə toxunmuram. Bank "ödəniş oldu" deyə möhürlü kağız göndərir, mən yoxlayıb üzvü VIP edirəm.

## Addım 2.10 — `AuthServiceApplication.java`
🔧 `@SpringBootApplication @EnableAsync` — `@EnableAsync` MailService-in `@Async`-ı işləsin deyə. Bu faylı əslində əvvəl yaradıram (skelet), amma `@EnableAsync`-ı MailService əlavə edəndə qoyuram.

---

# MƏRHƏLƏ 3 — API GATEWAY (niyə auth-dan sonra?)

Auth token verə bilir. İndi o tokeni **yoxlayan** mərkəzi lazımdır.

## `filter/JwtAuthFilter.java`
🔧 **TEXNİKİ SƏBƏB**: Hər servisdə ayrıca token yoxlaması yazmaq **təkrar** olardı. Ona görə mərkəzləşdirdim. `implements GlobalFilter` — bütün trafik buradan keçir.
- `PUBLIC_PATHS` siyahısı — bunları `List.of` ilə yaratdım çünki login/register token tələb etməməlidir (yumurta-toyuq problemi: token almaq üçün login lazımdır).
- Constructor `secret` — Auth-dakı **eyni** JWT secret (imzanı yoxlamaq üçün eyni açar olmalı).
- `filter()` — public isə keç, token yoxdursa 401, token varsa parse et və `X-User-Id`/`X-User-Role` header əlavə et. Bu header-ləri qoyuram ki, backend servislər tokeni yenidən açmasın — hazır alsınlar.
- `getOrder() = -1` — hamıdan əvvəl işləsin.

Routing `application.yml`-də (`lb://AUTH-SERVICE` — Eureka load balancing).

💡 **FEYNMAN**: Binanın **əsas girişinə** bir qapıçı qoyuram. Bütün gələnlər ondan keçir. Bilet yoxlayır, biletdən "bu 5 nömrəli üzv, həkimdir" oxuyub gələnin əlinə **rəngli bilərzik** taxır. İçəridəki mənzillər bileti yenidən yoxlamır — sadəcə bilərziyin rənginə baxır. Beləliklə hər mənzildə ayrıca qapıçı saxlamıram — bir qapıçı hamıya bəs edir.

---

# MƏRHƏLƏ 4 — NUTRITION SERVICE (niyə indi? Kalori məntiqi Tracking-dən əvvəl lazımdır)

Tracking "hədəfin nə qədərdir?" soruşacaq — o hədəfi hesablayan servis əvvəl hazır olmalıdır.

## Addım 4.1 — Protobuf müqaviləsi (`.proto`) + gRPC
🔧 **TEXNİKİ SƏBƏB**: Kalori hesablaması sırf riyazi çağırışdır, tez-tez olur, servisdən-servisə gedir. gRPC seçdim çünki binary + HTTP/2 = REST-dən sürətli. Əvvəl `.proto` faylında müqaviləni təyin etdim (request/response strukturu, enum-lar: Sex, ActivityLevel, GoalType). Gradle protobuf plugin bundan Java class-ları generate edir.

💡 **FEYNMAN**: İki nəfərin danışacağı dili **əvvəldən** kağıza yazıram: "sən mənə yaş, çəki, boy göndər, mən sənə kalori qaytaracam". Bu "lüğəti" (proto) bir dəfə yazıram, sonra maşın ondan hər iki tərəf üçün tərcüməçi düzəldir.

## Addım 4.2 — `grpc/NutritionGrpcService.java` (formullar)
🔧 **TEXNİKİ SƏBƏB**: `@GrpcService` ilə server metodunu yazdım. `calculateGoal(request, observer)` — arqument `observer` gRPC-nin cavab qaytarma üsuludur (`onNext` + `onCompleted`). İçində Mifflin-St Jeor formulu: BMR → `switch` ilə aktivlik əmsalı → TDEE → `switch` ilə məqsəd düzəlişi → makrolar. `switch expression` istifadə etdim (Java 17) çünki oxunaqlı və exhaustive.

💡 **FEYNMAN**: Bura "hesablama mətbəxidir". Sənin yaşını, çəkini alır, məşhur bir düsturla (elm adamları tapıb) hesablayır: "istirahətdə nə qədər yanırsan, hərəkətlə nə qədər, arıqlamaq üçün nə qədər yeməlisən". Cavabı bir-bir təhvil verir.

## Addım 4.3 — `web/GoalController.java` (REST körpüsü)
🔧 **TEXNİKİ SƏBƏB**: Frontend gRPC edə bilmir (brauzer məhdudiyyəti). Ona görə REST wrapper yaratdım. `@GrpcClient` stub inject edir, controller REST istəyini gRPC-yə çevirir. `/api/goals/calculate` public-dir (qeydiyyatsız da kalkulyator işləsin).

💡 **FEYNMAN**: Mətbəx gizli dildə danışır (gRPC), amma müştəri (brauzer) o dili bilmir. Ona görə bir **tərcüməçi** qoyuram: müştəri adi dildə sifariş verir, tərcüməçi mətbəxə çatdırır, cavabı geri tərcümə edir.

## Addım 4.4 — `entity/DietPlan.java` + `repo` + `web/DietPlanController.java`
🔧 **TEXNİKİ SƏBƏB**: Kalkulyatordan sonra "həkim xəstəyə plan yazsın" xüsusiyyətini əlavə etdim. `DietPlan` entity (patientId, doctorId, target, active). Repository-də `findFirstБy...OrderByIdDesc` — ən son aktiv planı götürür (defensive: bir neçə aktiv olsa çökmür). Controller-də həkim yalnız **öz** xəstəsinə plan yaza bilər — `authClient.getUser()` ilə xəstənin doctorId-si yoxlanır. Yeni plan köhnələri deaktiv edir.

💡 **FEYNMAN**: Həkim xəstəyə "reçet" yazır. Amma qayda qoyuram: həkim yalnız **öz** xəstəsinə yaza bilər (başqasının xəstəsinə yox). Yeni reçet yazılanda köhnəsi ləğv olur — həmişə bir etibarlı reçet olsun.

## Addım 4.5 — `web/AuthClient.java` (Feign)
🔧 `@FeignClient(name="auth-service")` — Eureka service name ilə Auth-a çağırış. Xəstənin doctorId-sini yoxlamaq üçün.

---

# MƏRHƏLƏ 5 — CATALOG SERVICE (qidalar bazası)

Tracking "hansı qidanı yedin?" soruşacaq — qida bazası hazır olmalıdır.

## Addım 5.1 — `entity/Product.java`
🔧 **TEXNİKİ SƏBƏB**: `implements Serializable` **qəsdən** qoydum — Redis cache obyekti byte-a çevirir, Serializable olmadan cache işləməz. Bütün dəyərlər 100g bazasında (standartlaşma).

💡 **FEYNMAN**: Hər qidanın kartı — 100 qramında nə qədər kalori. "Serializable" — kartı **qutuya qablaşdırıla bilən** edirəm ki, sürətli anbara (Redis) qoyum.

## Addım 5.2 — `repo/ProductRepository.java`
🔧 `findByNameContainingIgnoreCase` (axtarış), `findByBarcode` (barcode scan), `JpaSpecificationExecutor` (GraphQL dinamik sorğu üçün).

## Addım 5.3 — `client/OpenFoodFactsClient.java` (xarici API)
🔧 **TEXNİKİ SƏBƏB**: Bizim bazada hər qida yoxdur. Feign ilə dünya bazasına (OpenFoodFacts) bağlandım — barcode və ad ilə axtarış. Interface yazdım, Spring implement etdi.

💡 **FEYNMAN**: Öz siyahımda olmayan qidanı **dünya kataloqundan** soruşuram — telefon açıb "bu barkodun məlumatı nədir?" deyirəm.

## Addım 5.4 — `client/FoodSearchService.java` + `EnrichmentService.java` (ağıllı + dözümlü)
🔧 **TEXNİKİ SƏBƏB**: `search()` — əvvəl lokal, yoxdursa xarici, sonra lokala save (cache-aside). `@CircuitBreaker` + `fallbackMethod` qoydum çünki xarici API çökə bilər — o zaman fallback boş qaytarır, bütün tətbiq dağılmasın. Bu, **fault tolerance**.

💡 **FEYNMAN**: Əvvəl öz cibimə baxıram (sürətli), yoxdursa dünya kataloqundan çəkib **öz dəftərimə köçürürəm** (növbəti dəfə cibimdə olsun). Əgər dünya kataloqu cavab vermirsə — əl çəkirəm, "tapılmadı" deyirəm, amma özümü öldürmürəm (tətbiq çökmür).

## Addım 5.5 — `web/ProductController.java` + `ProductGraphQL.java`
🔧 REST (`@Cacheable` ilə cache), GraphQL (çoxparametrli axtarış). GraphQL-i əlavə etdim çünki "50-200 kalori arası, 5g+ zülal" kimi çevik filtrlər REST-də çətindir.

## Addım 5.6 — `config/DataSeeder.java` (CSV-dən)
🔧 **TEXNİKİ SƏBƏB**: Əvvəlcə 238 qidanı **kodda** yazmışdım (çox uzun). Sonra CSV-yə köçürdüm — data koddan ayrıldı. Yeni qida = CSV sətri (rebuild lazım, amma kod dəyişmir).

💡 **FEYNMAN**: Əvvəl 238 qidanı bir-bir koda yazırdım — kod nəhəng oldu. Sonra hamısını **Excel cədvəlinə** (CSV) köçürdüm, kod isə cədvəli oxuyur. İndi yeni qida əlavə etmək = cədvələ sətir yazmaq.

---

# MƏRHƏLƏ 6 — TRACKING SERVICE (ürək — hər şeyi birləşdirir)

İndi bütün əsaslar hazırdır (user, hədəf, qida). Tracking bunları birləşdirir.

## Addım 6.1 — Entity-lər (data əvvəl)
🔧 **TEXNİKİ SƏBƏB** — 4 entity yaratdım, hər biri bir ehtiyacdan:
- `FoodLog` — bir qida qeydi (nə, nə qədər, nə vaxt).
- `DailySummary` — günün cəmi. `@UniqueConstraint(userId, day)` qoydum çünki bir gün üçün yalnız bir hesabat olmalı (DB-səviyyəsində qorunma).
- `Alert` — həkimə xəbərdarlıq (limit aşılanda).
- `ChatMessage` — həkim-xəstə mesajı.

💡 **FEYNMAN**: Əsas dəftərimin səhifələri: (1) hər yeməyin qeydi, (2) günün yekunu — hər günə bir səhifə (təkrar olmasın), (3) həkimə siqnal kağızları, (4) yazışma vərəqləri.

## Addım 6.2 — Repositories
🔧 Hər entity üçün sorğu: `findByUserIdAndLogDate` (bugünkü qeydlər), `findByUserIdAndDay` (günün summary), `findTop50ByDoctorIdOrderByCreatedAtDesc` (son 50 alert), custom `@Query conversation` (iki nəfərin söhbəti — iki istiqamətli).

## Addım 6.3 — Feign Client-lər (üç servisə də bağlan)
🔧 **TEXNİKİ SƏBƏB**: Tracking tək başına işləyə bilməz — məlumat üçün 3 servisə müraciət edir:
- `CatalogClient` — qidanın kalori məlumatı.
- `NutritionClient` — istifadəçinin hədəfi.
- `AuthClient` — user rol/pro/doctorId (chat icazəsi üçün).
Hamısı `@FeignClient(name="...")` — Eureka ilə tapır.

💡 **FEYNMAN**: Bu dəftərxana özü hər şeyi bilmir. Qidanın kalorisini **kataloqdan**, hədəfi **məsləhətçidən**, kimliyi **qeydiyyatdan** soruşur. Telefon açıb hərəsindən lazım olanı alır.

## Addım 6.4 — `service/SummaryService.java` (biznes məntiqin ürəyi)
🔧 **TEXNİKİ SƏBƏB**: Bu, layihənin **ən vacib** class-ıdır. `@Transactional` qoydum — kalori toplama + save + push hamısı atomik olmalı. `apply()` metodu:
1. `orElseGet(newSummary)` — summary yoxdursa yaradır (lazy).
2. `newSummary` içində Nutrition-dan hədəfi çəkir.
3. Kalorini toplayır, `safe()` ilə null→0 (NPE qarşısı).
4. `push()` — faizi hesablayır, level təyin edir (OK/WARN/LIMIT), WebSocket ilə istifadəçiyə göndərir.
5. Limit aşılıb + həkim varsa → alert (həm DB, həm canlı).
`push`-u ayrı private metod etdim çünki `apply` təmiz qalsın (single responsibility).

💡 **FEYNMAN**: Bura beyin. Sən bir yemək qeyd edəndə: bu günün səhifəsini tapır (yoxsa açıb məsləhətçidən hədəfini yazır), yeni kalorini əvvəlkinə əlavə edir, faizini hesablayır ("hədəfin 90%-nə çatdın!"), ekranını **dərhal** yeniləyir. Əgər limiti keçmisənsə və həkimin varsa — həkimə zəng vurur ("xəstən çox yeyib!").

## Addım 6.5 — `service/NutritionPlanClient.java` + `ChatAccessService.java`
🔧 `NutritionPlanClient` — Nutrition çağırışını `@CircuitBreaker` ilə bükür (çöksəydə boş plan). `ChatAccessService` — chat icazə qaydası (Pro + öz həkimi), fail-secure (xəta=deny).

## Addım 6.6 — Controller-lər
🔧 **TEXNİKİ SƏBƏB**:
- `FoodLogController.log()` — Catalog-dan məhsul çək, qramlıq faktoru ilə vur, save, `summaryService.apply()`.
- `SummaryController` — hesabat/qrafik/alert endpoint-ləri. `history()` boş günləri 0-la doldurur (qrafik davamlı).
- `ChatController` — `@MessageMapping` (WebSocket mesaj) + REST (tarixçə).

💡 **FEYNMAN**: Qapılar. Qida qapısı: "150g toyuq" → kataloqdan toyuğu tapır, 1.5-ə vurur, qeyd edir, beyni işə salır. Hesabat qapısı: qrafik və yekunlar. Chat qapısı: canlı yazışma.

## Addım 6.7 — WebSocket (`WebSocketConfig` + `JwtChannelInterceptor`)
🔧 **TEXNİKİ SƏBƏB**: Real-time yeniləmə üçün son əlavə etdim. `WebSocketConfig` STOMP qurur (`/ws` endpoint, `/user` prefix şəxsi kanal üçün). **Problem**: Gateway HTTP filter WebSocket mesajlarına işləmir. Ona görə `JwtChannelInterceptor` yaratdım — STOMP CONNECT-də tokeni yoxlayır, userId-ni Principal təyin edir. Bu Principal olmadan `convertAndSendToUser` işləməz.

💡 **FEYNMAN**: Adi məktublaşma (HTTP) — hər dəfə yeni zərf. Canlı danışıq (WebSocket) — açıq telefon xətti, istədiyim an danışıram. Amma xətti açanda kimliyi bir dəfə yoxlamalıyam (JwtChannelInterceptor) ki, sonra "5 nömrəli xəttə" düzgün mesaj göndərim.

---

# MƏRHƏLƏ 7 — FRONTEND

🔧 **TEXNİKİ SƏBƏB**: Backend REST/WebSocket hazır olandan sonra vanilla HTML/JS ilə UI qurdum. `app.js` — auth, API çağırışları, WebSocket subscribe. `chatbot.js` — n8n webhook. Nginx `/api` və `/ws`-i Gateway-ə proxy edir. React işlətmədim çünki layihə üçün sadəlik kifayət idi.

💡 **FEYNMAN**: Bütün mətbəx-anbar-hesablama hazır olandan sonra **vitrin** düzəltdim — düymələr, formalar, qrafiklər. Sadə tutdum ki, işləsin.

---

# MƏRHƏLƏ 8 — TESTLƏR (ən sonda, məntiqi qorumaq üçün)

🔧 **TEXNİKİ SƏBƏB**: SummaryService-in məntiqi kritikdir (kalori, alert). JUnit 5 + Mockito ilə 18 test yazdım. Repository/WS mock etdim — real DB olmadan sürətli, təcrid olunmuş test. `@ParameterizedTest` ilə OK/WARN/LIMIT sərhədlərini bir metodda yoxladım. `ArgumentCaptor` ilə WebSocket payload-un düzgünlüyünü tutdum.

💡 **FEYNMAN**: Bütün maşın işləyəndən sonra **yoxlama robotları** qoyuram: "500 kalori at, cəm 500 olsun; limiti keç, həkimə siqnal getsin". Robot səhv taparsa dərhal xəbərdar edir — mən bilmədən nəsə sınmasın.

---

# 🎯 QURULMA MƏNTİQİNİN XÜLASƏSİ (müdafiədə bir cümlə ilə)

> "Layihəni **aşağıdan yuxarı** qurdum: əvvəl infrastruktur (Eureka), sonra kimlik (Auth — entity→repo→service→controller sırası ilə), sonra bu tokeni yoxlayan Gateway, sonra hesablama (Nutrition/gRPC), sonra qida bazası (Catalog), sonra hamısını birləşdirən Tracking (əsas biznes məntiq + WebSocket), ən sonda frontend və testlər. Hər servisdə **data → sorğu → məntiq → qapı** ardıcıllığını izlədim, çünki hər qat özündən aşağıdakına söykənir."

Bu ardıcıllıq təsadüfi deyil — **asılılıq sırasıdır**: hər addım özündən əvvəlkinə möhtacdır.
