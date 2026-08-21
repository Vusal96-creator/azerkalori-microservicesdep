package az.azerkalori.tracking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "food_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FoodLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long productId;

    private String productName;
    private Double grams;

    private Double calories;
    private Double proteinG;
    private Double fatG;
    private Double carbsG;

    private LocalDate logDate;
    private Instant createdAt;
}
