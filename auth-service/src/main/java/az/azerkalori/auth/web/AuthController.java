package az.azerkalori.auth.web;

import az.azerkalori.auth.entity.Role;
import az.azerkalori.auth.entity.User;
import az.azerkalori.auth.mail.MailService;
import az.azerkalori.auth.repo.UserRepository;
import az.azerkalori.auth.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final MailService mail;

    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("status", "auth-service up");
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody RegisterRequest req) {
        users.findByEmail(req.email()).ifPresent(u -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        });
        // İstifadəçi rol seçir. Həkim admin təsdiqi gözləyir, USER dərhal təsdiqlidir.
        Role chosen = "DOCTOR".equalsIgnoreCase(req.role()) ? Role.DOCTOR : Role.USER;
        boolean approved = chosen == Role.USER;
        User user = User.builder()
                .email(req.email())
                .password(encoder.encode(req.password()))
                .fullName(req.fullName())
                .role(chosen)
                .approved(approved)
                .age(req.age()).weightKg(req.weightKg()).heightCm(req.heightCm())
                .sex(req.sex()).activityLevel(req.activityLevel())
                .build();
        users.save(user);
        mail.sendWelcome(user.getEmail(), user.getFullName());
        Map<String, Object> res = new HashMap<>();
        res.put("id", user.getId());
        res.put("role", user.getRole());
        res.put("approved", approved);
        if (approved) res.put("token", jwt.issue(user)); // pending həkimə token verilmir
        return res;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest req) {
        User user = users.findByEmail(req.email())
                .filter(u -> encoder.matches(req.password(), u.getPassword()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bad credentials"));
        if (user.getRole() == Role.DOCTOR && !user.isApproved()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Hesabınız admin təsdiqini gözləyir");
        }
        return Map.of("id", user.getId(), "token", jwt.issue(user), "role", user.getRole());
    }

    @GetMapping("/admin/pending")
    public List<User> pending(@RequestHeader("X-User-Role") String role) {
        requireRole(role, Role.ADMIN);
        return users.findByApprovedFalse();
    }

    @PutMapping("/admin/approve/{id}")
    public Map<String, Object> approve(@RequestHeader("X-User-Role") String role,
                                       @PathVariable Long id) {
        requireRole(role, Role.ADMIN);
        User u = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        u.setApproved(true);
        users.save(u);
        return Map.of("id", id, "approved", true);
    }

    @GetMapping("/me")
    public User me(@RequestHeader("X-User-Id") Long userId) {
        return users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    @PostMapping("/admin/doctors")
    public Map<String, Object> createDoctor(@RequestHeader("X-User-Role") String role,
                                            @RequestBody RegisterRequest req) {
        requireRole(role, Role.ADMIN);
        User doc = User.builder()
                .email(req.email())
                .password(encoder.encode(req.password()))
                .fullName(req.fullName())
                .role(Role.DOCTOR)
                .approved(true) // admin-yaratdığı həkim dərhal təsdiqlidir
                .build();
        users.save(doc);
        return Map.of("id", doc.getId(), "role", doc.getRole());
    }

    @GetMapping("/admin/users")
    public List<User> allUsers(@RequestHeader("X-User-Role") String role) {
        requireRole(role, Role.ADMIN);
        return users.findAll();
    }

    @GetMapping("/admin/doctors")
    public List<User> allDoctors(@RequestHeader("X-User-Role") String role) {
        requireRole(role, Role.ADMIN);
        return users.findByRole(Role.DOCTOR);
    }

    @PutMapping("/admin/patients/{patientId}/doctor/{doctorId}")
    public Map<String, Object> assignDoctor(@RequestHeader("X-User-Role") String role,
                                            @PathVariable Long patientId,
                                            @PathVariable Long doctorId) {
        requireRole(role, Role.ADMIN);
        User patient = users.findById(patientId)
                .filter(u -> u.getRole() == Role.USER)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));
        users.findById(doctorId)
                .filter(u -> u.getRole() == Role.DOCTOR)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found"));
        patient.setDoctorId(doctorId);
        users.save(patient);
        return Map.of("patientId", patientId, "doctorId", doctorId);
    }

    @GetMapping("/doctor/patients")
    public List<User> myPatients(@RequestHeader("X-User-Role") String role,
                                 @RequestHeader("X-User-Id") Long doctorId) {
        requireRole(role, Role.DOCTOR);
        return users.findByDoctorId(doctorId);
    }

    @GetMapping("/internal/users/{id}")
    public User getUser(@PathVariable Long id) {
        return users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private void requireRole(String actual, Role required) {
        if (!required.name().equals(actual)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Requires role " + required);
        }
    }

    public record RegisterRequest(String email, String password, String fullName,
                                  Integer age, Double weightKg, Double heightCm,
                                  String sex, String activityLevel, String role) {}

    public record LoginRequest(String email, String password) {}
}
