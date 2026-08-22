package az.azerkalori.tracking.service;

import az.azerkalori.tracking.entity.DailySummary;
import az.azerkalori.tracking.entity.FoodLog;
import az.azerkalori.tracking.repo.DailySummaryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// @ExtendWith(MockitoExtension.class) -> JUnit 5-ə deyir ki, Mockito-nu bu test
// class-ında işə salsın (mock-ları avtomatik yaratsın, @InjectMocks-a yerləşdirsin).
@ExtendWith(MockitoExtension.class)
class SummaryServiceTest {

    // @Mock -> saxta (mock) obyekt yaradır. Real DB/şəbəkə YOXDUR.
    // Biz hər birinin nə qaytaracağını testdə özümüz deyəcəyik.
    @Mock
    private DailySummaryRepository summaries;

    @Mock
    private NutritionPlanClient plans;

    @Mock
    private SimpMessagingTemplate ws;

    // @InjectMocks -> real SummaryService yaradır və yuxarıdakı 3 mock-u
    // onun içinə (constructor vasitəsilə) yerləşdirir.
    @InjectMocks
    private SummaryService service;

    // @Captor -> ws-ə göndərilən arqumenti "tutmaq" (capture) üçün.
    // ws.convertAndSendToUser(user, dest, PAYLOAD) -> PAYLOAD Map-ini yaxalayacağıq.
    @Captor
    private ArgumentCaptor<Map<String, Object>> payloadCaptor;

    @Test
    void firstFoodLog_createsSummary_andSumsCalories() {
        // ---------- ARRANGE (hazırlıq) ----------
        Long userId = 1L;
        LocalDate today = LocalDate.of(2026, 8, 22);

        // İstifadəçinin bir qida qeydi (500 kcal, 30q protein)
        FoodLog entry = FoodLog.builder()
                .userId(userId)
                .productId(10L)
                .calories(500.0)
                .proteinG(30.0)
                .fatG(10.0)
                .carbsG(40.0)
                .logDate(today)
                .build();

        // Bu istifadəçi üçün həmin gün HƏLƏ summary yoxdur -> Optional.empty()
        when(summaries.findByUserIdAndDay(eq(userId), eq(today)))
                .thenReturn(java.util.Optional.empty());

        // Yeni summary yaradılarkən nutrition-plan sorğusu boş qayıtsın
        // (hədəf kalori təyin olunmasın)
        when(plans.activePlan(userId)).thenReturn(Map.of());

        // save(...) çağırılanda -> ötürülən obyekti olduğu kimi geri qaytar
        when(summaries.save(any(DailySummary.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // ---------- ACT (icra) ----------
        DailySummary result = service.apply(userId, entry);

        // ---------- ASSERT (yoxlama) ----------
        assertEquals(500.0, result.getCalories());
        assertEquals(30.0, result.getProteinG());
        assertEquals(10.0, result.getFatG());
        assertEquals(40.0, result.getCarbsG());
        assertEquals(userId, result.getUserId());
        assertEquals(today, result.getDay());
    }

    // ===================== T2: null edge-case + verify =====================

    @Test
    void nullMacros_areTreatedAsZero_noNpe() {
        // ARRANGE: dəyərlərin BƏZİSİ null (məs. protein/fat/carbs qeyd olunmayıb)
        Long userId = 1L;
        LocalDate today = LocalDate.of(2026, 8, 22);

        FoodLog entry = FoodLog.builder()
                .userId(userId)
                .productId(10L)
                .calories(200.0)   // yalnız kalori var
                .proteinG(null)    // null
                .fatG(null)        // null
                .carbsG(null)      // null
                .logDate(today)
                .build();

        when(summaries.findByUserIdAndDay(eq(userId), eq(today)))
                .thenReturn(java.util.Optional.empty());
        when(plans.activePlan(userId)).thenReturn(Map.of());
        when(summaries.save(any(DailySummary.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // ACT: null-lar NPE atmamalıdır (safe() 0-a çevirir)
        DailySummary result = service.apply(userId, entry);

        // ASSERT: null-lar 0 kimi hesablanıb
        assertEquals(200.0, result.getCalories());
        assertEquals(0.0, result.getProteinG());
        assertEquals(0.0, result.getFatG());
        assertEquals(0.0, result.getCarbsG());
    }

    @Test
    void apply_sendsWebSocketUpdateToUser() {
        // ARRANGE
        Long userId = 7L;
        LocalDate today = LocalDate.of(2026, 8, 22);

        FoodLog entry = FoodLog.builder()
                .userId(userId).productId(1L)
                .calories(300.0).proteinG(20.0).fatG(5.0).carbsG(25.0)
                .logDate(today).build();

        when(summaries.findByUserIdAndDay(eq(userId), eq(today)))
                .thenReturn(java.util.Optional.empty());
        when(plans.activePlan(userId)).thenReturn(Map.of());
        when(summaries.save(any(DailySummary.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // ACT
        service.apply(userId, entry);

        // ASSERT (verify): istifadəçiyə /queue/calories-ə DƏQİQ 1 dəfə mesaj gedib
        verify(ws, times(1))
                .convertAndSendToUser(eq("7"), eq("/queue/calories"), any());
    }

    // ===================== T3: ArgumentCaptor =====================
    // verify() yalnız "çağırıldımı?" deyir. ArgumentCaptor isə çağırışın
    // İÇİNDƏKİ dəyəri tutub yoxlamağa imkan verir (payload düzgündürmü?).

    @Test
    void push_payload_hasCorrectPercentAndLevel() {
        // ARRANGE: istifadəçinin gündəlik hədəfi 2000 kcal
        Long userId = 5L;
        LocalDate today = LocalDate.of(2026, 8, 22);

        FoodLog entry = FoodLog.builder()
                .userId(userId).productId(1L)
                .calories(500.0).proteinG(20.0).fatG(5.0).carbsG(25.0)
                .logDate(today).build();

        when(summaries.findByUserIdAndDay(eq(userId), eq(today)))
                .thenReturn(java.util.Optional.empty());
        // dailyCalorieTarget = 2000 -> targetCalories təyin olunur
        when(plans.activePlan(userId))
                .thenReturn(Map.of("dailyCalorieTarget", 2000));
        when(summaries.save(any(DailySummary.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // ACT
        service.apply(userId, entry);

        // ASSERT: ws-ə gedən payload-u TUT
        verify(ws).convertAndSendToUser(eq("5"), eq("/queue/calories"), payloadCaptor.capture());
        Map<String, Object> payload = payloadCaptor.getValue();

        // 500 / 2000 = 25% -> level "OK" (80%-dən aşağı)
        assertEquals(500.0, payload.get("calories"));
        assertEquals(2000.0, payload.get("targetCalories"));
        assertEquals(25L, payload.get("percent"));   // Math.round(...) long qaytarır
        assertEquals("OK", payload.get("level"));
    }

    // ===================== T4: @ParameterizedTest =====================
    // Bir metod, çox hal. Aşağıdakı hər sətir AYRI test kimi işləyir.
    // Belə OK/WARN/LIMIT sərhədlərini (80% və 100%) bir yerdə yoxlayırıq.

    @ParameterizedTest(name = "{0} kcal / {1} hədəf -> {2}")
    @CsvSource({
            "500,  2000, OK",     // 25%  -> 80%-dən aşağı
            "1599, 2000, OK",     // 79.95% -> hələ OK
            "1600, 2000, WARN",   // 80%  -> sərhəd: WARN başlayır
            "1900, 2000, WARN",   // 95%  -> WARN
            "2000, 2000, LIMIT",  // 100% -> sərhəd: LIMIT başlayır
            "2500, 2000, LIMIT"   // 125% -> LIMIT
    })
    void level_isComputedFromPercent(double calories, double target, String expectedLevel) {
        Long userId = 9L;
        LocalDate today = LocalDate.of(2026, 8, 22);

        FoodLog entry = FoodLog.builder()
                .userId(userId).productId(1L)
                .calories(calories).proteinG(0.0).fatG(0.0).carbsG(0.0)
                .logDate(today).build();

        when(summaries.findByUserIdAndDay(eq(userId), eq(today)))
                .thenReturn(java.util.Optional.empty());
        when(plans.activePlan(userId))
                .thenReturn(Map.of("dailyCalorieTarget", target));
        when(summaries.save(any(DailySummary.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // ACT
        service.apply(userId, entry);

        // CAPTURE + ASSERT: /queue/calories payload-unun level-i gözlənilənlə üst-üstə düşür
        verify(ws).convertAndSendToUser(eq("9"), eq("/queue/calories"), payloadCaptor.capture());
        assertEquals(expectedLevel, payloadCaptor.getValue().get("level"));
    }
}
