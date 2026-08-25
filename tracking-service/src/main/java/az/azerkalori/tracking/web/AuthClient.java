package az.azerkalori.tracking.web;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * auth-service-dən istifadəçi məlumatı (rol, doctorId, pro) alır —
 * chat icazəsini yoxlamaq üçün.
 */
@FeignClient(name = "auth-service")
public interface AuthClient {

    @GetMapping("/api/auth/internal/users/{id}")
    Map<String, Object> getUser(@PathVariable("id") Long id);
}
