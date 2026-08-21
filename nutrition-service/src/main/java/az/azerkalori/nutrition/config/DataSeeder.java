package az.azerkalori.nutrition.config;

import az.azerkalori.nutrition.entity.DietPlan;
import az.azerkalori.nutrition.repo.DietPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final DietPlanRepository plans;

    @Override
    public void run(String... args) {
        if (plans.count() > 0) return;
        plans.save(DietPlan.builder()
                .patientId(3L)
                .doctorId(2L)
                .dailyCalorieTarget(2200.0)
                .proteinG(150.0).fatG(65.0).carbsG(240.0)
                .notes("Balanced plan. Reduce sweets, increase protein.")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(1))
                .active(true)
                .build());
    }
}
