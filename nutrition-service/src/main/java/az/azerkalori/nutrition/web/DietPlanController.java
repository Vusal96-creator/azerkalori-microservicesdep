package az.azerkalori.nutrition.web;

import az.azerkalori.nutrition.entity.DietPlan;
import az.azerkalori.nutrition.repo.DietPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class DietPlanController {

    private final DietPlanRepository plans;
    private final AuthClient authClient;

    @PostMapping
    public DietPlan create(@RequestHeader("X-User-Role") String role,
                           @RequestHeader("X-User-Id") Long doctorId,
                           @RequestBody DietPlan plan) {
        requireRole(role, "DOCTOR");

        Map<String, Object> patient = authClient.getUser(plan.getPatientId());
        Object assignedDoctor = patient.get("doctorId");
        if (assignedDoctor == null || !doctorId.equals(Long.valueOf(assignedDoctor.toString()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your patient");
        }

        // Pasiyentin bütün əvvəlki aktiv planlarını deaktiv et (bir neçə ola bilər).
        List<DietPlan> olds = plans.findAllByPatientIdAndActiveTrue(plan.getPatientId());
        olds.forEach(old -> old.setActive(false));
        plans.saveAll(olds);

        plan.setDoctorId(doctorId);
        plan.setActive(true);
        return plans.save(plan);
    }

    @GetMapping("/my")
    public DietPlan myPlan(@RequestHeader("X-User-Id") Long userId) {
        return plans.findFirstByPatientIdAndActiveTrueOrderByIdDesc(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No active plan"));
    }

    @GetMapping("/patient/{patientId}")
    public DietPlan patientPlan(@RequestHeader(value = "X-User-Role", required = false) String role,
                                @RequestHeader(value = "X-User-Id", required = false) Long callerId,
                                @PathVariable Long patientId) {
        DietPlan plan = plans.findFirstByPatientIdAndActiveTrueOrderByIdDesc(patientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No active plan"));
        if ("DOCTOR".equals(role) && !plan.getDoctorId().equals(callerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your patient");
        }
        return plan;
    }

    @GetMapping("/mine-as-doctor")
    public List<DietPlan> doctorPlans(@RequestHeader("X-User-Role") String role,
                                      @RequestHeader("X-User-Id") Long doctorId) {
        requireRole(role, "DOCTOR");
        return plans.findByDoctorId(doctorId);
    }

    private void requireRole(String actual, String required) {
        if (!required.equals(actual)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Requires role " + required);
        }
    }
}
