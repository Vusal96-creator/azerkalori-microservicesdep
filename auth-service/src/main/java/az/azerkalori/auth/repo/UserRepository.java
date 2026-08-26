package az.azerkalori.auth.repo;

import az.azerkalori.auth.entity.Role;
import az.azerkalori.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    List<User> findByDoctorId(Long doctorId);
    List<User> findByRole(Role role);
    List<User> findByApprovedFalse();
}
