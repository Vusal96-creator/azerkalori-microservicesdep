package az.azerkalori.nutrition.repo;

import az.azerkalori.nutrition.entity.DietPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DietPlanRepository extends JpaRepository<DietPlan, Long> {
    Optional<DietPlan> findByPatientIdAndActiveTrue(Long patientId);
    List<DietPlan> findByDoctorId(Long doctorId);
}
