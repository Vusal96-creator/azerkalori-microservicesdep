package az.azerkalori.nutrition.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "diet_plans")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DietPlan {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long patientId;

    @Column(nullable = false)
    private Long doctorId;

    @Column(nullable = false)
    private Double dailyCalorieTarget;

    private Double proteinG;
    private Double fatG;
    private Double carbsG;

    @Column(length = 2000)
    private String notes;

    private LocalDate startDate;
    private LocalDate endDate;

    private boolean active;
}
