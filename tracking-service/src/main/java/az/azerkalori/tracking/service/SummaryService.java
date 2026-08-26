package az.azerkalori.tracking.service;

import az.azerkalori.tracking.entity.Alert;
import az.azerkalori.tracking.entity.DailySummary;
import az.azerkalori.tracking.entity.FoodLog;
import az.azerkalori.tracking.repo.AlertRepository;
import az.azerkalori.tracking.repo.DailySummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SummaryService {

    private final DailySummaryRepository summaries;
    private final NutritionPlanClient plans;
    private final SimpMessagingTemplate ws;
    private final AlertRepository alerts;

    @Transactional
    public DailySummary apply(Long userId, FoodLog entry) {
        LocalDate day = entry.getLogDate();
        DailySummary summary = summaries.findByUserIdAndDay(userId, day)
                .orElseGet(() -> newSummary(userId, day));

        summary.setCalories(summary.getCalories() + safe(entry.getCalories()));
        summary.setProteinG(summary.getProteinG() + safe(entry.getProteinG()));
        summary.setFatG(summary.getFatG() + safe(entry.getFatG()));
        summary.setCarbsG(summary.getCarbsG() + safe(entry.getCarbsG()));
        DailySummary saved = summaries.save(summary);

        push(saved);
        return saved;
    }

    private DailySummary newSummary(Long userId, LocalDate day) {
        Map<String, Object> plan = plans.activePlan(userId);
        return DailySummary.builder()
                .userId(userId).day(day)
                .calories(0d).proteinG(0d).fatG(0d).carbsG(0d)
                .targetCalories(asDouble(plan.get("dailyCalorieTarget")))
                .doctorId(asLong(plan.get("doctorId")))
                .build();
    }

    private void push(DailySummary s) {
        double target = s.getTargetCalories() == null ? 0 : s.getTargetCalories();
        double percent = target == 0 ? 0 : 100.0 * s.getCalories() / target;
        String level = percent >= 100 ? "LIMIT" : percent >= 80 ? "WARN" : "OK";

        ws.convertAndSendToUser(String.valueOf(s.getUserId()), "/queue/calories",
                Map.of("calories", round(s.getCalories()),
                        "proteinG", round(s.getProteinG()),
                        "fatG", round(s.getFatG()),
                        "carbsG", round(s.getCarbsG()),
                        "targetCalories", target,
                        "percent", Math.round(percent),
                        "level", level));

        if (percent >= 100 && s.getDoctorId() != null) {
            // 1) DB-də saxla (həkim sonra panelə girəndə də görsün, canlı timing-dən asılı olmasın)
            alerts.save(Alert.builder()
                    .doctorId(s.getDoctorId())
                    .patientId(s.getUserId())
                    .calories(round(s.getCalories()))
                    .targetCalories(target)
                    .percent(Math.round(percent))
                    .createdAt(Instant.now())
                    .build());
            // 2) Canlı göndər (həkim onlayndırsa dərhal görsün)
            ws.convertAndSendToUser(String.valueOf(s.getDoctorId()), "/queue/alerts",
                    Map.of("patientId", s.getUserId(),
                            "calories", round(s.getCalories()),
                            "targetCalories", target,
                            "percent", Math.round(percent)));
        }
    }

    private double safe(Double v) { return v == null ? 0 : v; }
    private double round(double v) { return Math.round(v * 10) / 10.0; }

    private Double asDouble(Object o) { return o == null ? null : Double.valueOf(o.toString()); }
    private Long asLong(Object o) { return o == null ? null : Long.valueOf(o.toString()); }
}
