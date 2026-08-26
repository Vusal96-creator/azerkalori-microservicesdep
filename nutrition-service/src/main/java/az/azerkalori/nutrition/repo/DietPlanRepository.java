package az.azerkalori.nutrition.repo;

import az.azerkalori.nutrition.entity.DietPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DietPlanRepository extends JpaRepository<DietPlan, Long> {
    // Ən son aktiv plan (bir neçə aktiv plan olsa belə çökmür).
    Optional<DietPlan> findFirstByPatientIdAndActiveTrueOrderByIdDesc(Long patientId);
    // Pasiyentin BÜTÜN aktiv planları (yeni plan yaradılanda hamısını deaktiv etmək üçün).
    List<DietPlan> findAllByPatientIdAndActiveTrue(Long patientId);
    List<DietPlan> findByDoctorId(Long doctorId);
}
