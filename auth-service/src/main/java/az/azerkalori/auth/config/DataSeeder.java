package az.azerkalori.auth.config;

import az.azerkalori.auth.entity.Role;
import az.azerkalori.auth.entity.User;
import az.azerkalori.auth.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository users;
    private final PasswordEncoder encoder;

    @Override
    public void run(String... args) {
        if (users.count() > 0) return;

        User admin = users.save(User.builder()
                .email("admin@azerkalori.az")
                .password(encoder.encode("admin123"))
                .fullName("System Admin")
                .role(Role.ADMIN)
                .build());

        User doctor = users.save(User.builder()
                .email("doctor@azerkalori.az")
                .password(encoder.encode("doctor123"))
                .fullName("Dr. Leyla Mammadova")
                .role(Role.DOCTOR)
                .build());

        users.save(User.builder()
                .email("user@azerkalori.az")
                .password(encoder.encode("user123"))
                .fullName("Elvin Aliyev")
                .role(Role.USER)
                .doctorId(doctor.getId())
                .age(28).weightKg(82.0).heightCm(178.0)
                .sex("MALE").activityLevel("MODERATE")
                .build());
    }
}
