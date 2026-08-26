package az.azerkalori.tracking.repo;

import az.azerkalori.tracking.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findTop50ByDoctorIdOrderByCreatedAtDesc(Long doctorId);
}
