package az.azerkalori.tracking.web;

import az.azerkalori.tracking.entity.Alert;
import az.azerkalori.tracking.entity.DailySummary;
import az.azerkalori.tracking.repo.AlertRepository;
import az.azerkalori.tracking.repo.DailySummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/summary")
@RequiredArgsConstructor
public class SummaryController {

    private final DailySummaryRepository summaries;
    private final AlertRepository alerts;

    // Həkimin son xəbərdarlıqları (DB-dən — panelə girəndə göstərmək üçün).
    @GetMapping("/alerts")
    public List<Alert> alerts(@RequestHeader("X-User-Id") Long doctorId) {
        return alerts.findTop50ByDoctorIdOrderByCreatedAtDesc(doctorId);
    }

    @GetMapping("/today")
    public Map<String, Object> today(@RequestHeader("X-User-Id") Long userId) {
        return summaries.findByUserIdAndDay(userId, LocalDate.now())
                .map(this::toView)
                .orElseGet(() -> Map.of(
                        "calories", 0d, "proteinG", 0d, "fatG", 0d, "carbsG", 0d,
                        "targetCalories", 0d, "percent", 0L, "level", "OK"));
    }

    @GetMapping("/patient/{patientId}/today")
    public Map<String, Object> patientToday(@RequestHeader("X-User-Role") String role,
                                            @PathVariable Long patientId) {
        if (!"DOCTOR".equals(role) && !"ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return summaries.findByUserIdAndDay(patientId, LocalDate.now())
                .map(this::toView)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No summary yet"));
    }

    private Map<String, Object> toView(DailySummary s) {
        double target = s.getTargetCalories() == null ? 0 : s.getTargetCalories();
        double percent = target == 0 ? 0 : 100.0 * s.getCalories() / target;
        String level = percent >= 100 ? "LIMIT" : percent >= 80 ? "WARN" : "OK";
        return Map.of(
                "calories", s.getCalories(),
                "proteinG", s.getProteinG(),
                "fatG", s.getFatG(),
                "carbsG", s.getCarbsG(),
                "targetCalories", target,
                "percent", Math.round(percent),
                "level", level);
    }
}
