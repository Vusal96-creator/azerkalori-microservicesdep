package az.azerkalori.tracking.web;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "nutrition-service")
public interface NutritionClient {

    @GetMapping("/api/plans/patient/{patientId}")
    Map<String, Object> activePlan(@PathVariable("patientId") Long patientId);
}
