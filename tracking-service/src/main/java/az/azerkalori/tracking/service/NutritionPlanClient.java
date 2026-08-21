package az.azerkalori.tracking.service;

import az.azerkalori.tracking.web.NutritionClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NutritionPlanClient {

    private final NutritionClient nutrition;

    @CircuitBreaker(name = "nutrition", fallbackMethod = "fallback")
    public Map<String, Object> activePlan(Long userId) {
        return nutrition.activePlan(userId);
    }

    public Map<String, Object> fallback(Long userId, Throwable t) {
        log.warn("nutrition-service unavailable ({}), no target for user {}",
                t.getClass().getSimpleName(), userId);
        return Map.of();
    }
}
