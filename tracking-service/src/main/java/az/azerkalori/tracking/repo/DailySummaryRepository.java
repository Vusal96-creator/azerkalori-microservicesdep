package az.azerkalori.tracking.repo;

import az.azerkalori.tracking.entity.DailySummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailySummaryRepository extends JpaRepository<DailySummary, Long> {
    Optional<DailySummary> findByUserIdAndDay(Long userId, LocalDate day);

    List<DailySummary> findByUserIdAndDayBetweenOrderByDayAsc(Long userId, LocalDate from, LocalDate to);
}
