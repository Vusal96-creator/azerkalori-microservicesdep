package az.azerkalori.tracking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "daily_summaries",
       uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "day"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DailySummary {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private LocalDate day;

    private Double calories;
    private Double proteinG;
    private Double fatG;
    private Double carbsG;

    private Double targetCalories;

    private Long doctorId;
}
