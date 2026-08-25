package az.azerkalori.auth.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false)
    private String password;

    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private Long doctorId;

    // Pro abunə (Stripe ödənişindən sonra aktivləşir)
    @Column(nullable = false)
    private boolean pro;
    private Instant proUntil;

    private Integer age;
    private Double weightKg;
    private Double heightCm;
    private String sex;
    private String activityLevel;
}
