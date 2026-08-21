package az.azerkalori.tracking.repo;

import az.azerkalori.tracking.entity.FoodLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface FoodLogRepository extends JpaRepository<FoodLog, Long> {
    List<FoodLog> findByUserIdAndLogDate(Long userId, LocalDate date);
}
