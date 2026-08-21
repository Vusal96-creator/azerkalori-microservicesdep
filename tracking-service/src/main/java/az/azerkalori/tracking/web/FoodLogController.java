package az.azerkalori.tracking.web;

import az.azerkalori.tracking.entity.FoodLog;
import az.azerkalori.tracking.repo.FoodLogRepository;
import az.azerkalori.tracking.service.SummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class FoodLogController {

    private final FoodLogRepository logs;
    private final CatalogClient catalog;
    private final SummaryService summaryService;

    @PostMapping
    public FoodLog log(@RequestHeader("X-User-Id") Long userId,
                       @RequestBody LogRequest req) {

        Map<String, Object> p = catalog.product(req.productId());
        double factor = req.grams() / 100.0;

        FoodLog entry = FoodLog.builder()
                .userId(userId)
                .productId(req.productId())
                .productName((String) p.get("name"))
                .grams(req.grams())
                .calories(num(p.get("calories")) * factor)
                .proteinG(num(p.get("proteinG")) * factor)
                .fatG(num(p.get("fatG")) * factor)
                .carbsG(num(p.get("carbsG")) * factor)
                .logDate(LocalDate.now())
                .createdAt(Instant.now())
                .build();
        logs.save(entry);

        summaryService.apply(userId, entry);
        return entry;
    }

    @GetMapping("/today")
    public List<FoodLog> today(@RequestHeader("X-User-Id") Long userId) {
        return logs.findByUserIdAndLogDate(userId, LocalDate.now());
    }

    @GetMapping("/patient/{patientId}/today")
    public List<FoodLog> patientToday(@RequestHeader("X-User-Role") String role,
                                      @PathVariable Long patientId) {
        if (!"DOCTOR".equals(role) && !"ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return logs.findByUserIdAndLogDate(patientId, LocalDate.now());
    }

    private double num(Object o) { return o == null ? 0 : Double.parseDouble(o.toString()); }

    public record LogRequest(Long productId, Double grams) {}
}
