package az.azerkalori.auth.security;

import az.azerkalori.auth.entity.Role;
import az.azerkalori.auth.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Bu testdə MOCK YOXDUR. JwtService saf məntiqdir (real kriptoqrafiya),
// ona görə real obyekt yaradıb, çıxan token-i geri oxuyub yoxlayırıq (round-trip).
class JwtServiceTest {

    // HMAC-SHA üçün açar ən azı 32 simvol olmalıdır.
    private static final String SECRET = "test-secret-key-min-32-chars-long-1234567890";

    private JwtService jwtService;
    private SecretKey key;

    @BeforeEach
    void setUp() {
        // Spring konteyneri YOX -> servisi əl ilə, test açarı ilə qururuq.
        jwtService = new JwtService(SECRET);
        // Token-i geri oxumaq üçün eyni açarı ayrıca hazırlayırıq.
        key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    private User sampleUser() {
        return User.builder()
                .id(42L)
                .email("vusal@azerkalori.az")
                .role(Role.USER)
                .build();
    }

    @Test
    void issue_returnsWellFormedJwt() {
        String token = jwtService.issue(sampleUser());

        assertNotNull(token);
        // JWT struktur: header.payload.signature -> 3 hissə, 2 nöqtə
        assertEquals(3, token.split("\\.").length);
    }

    @Test
    void issue_encodesUserClaims() {
        String token = jwtService.issue(sampleUser());

        // Token-i eyni açarla geri oxu (imza da yoxlanılır)
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertEquals("42", claims.getSubject());                 // subject = user.id
        assertEquals("USER", claims.get("role", String.class));
        assertEquals("vusal@azerkalori.az", claims.get("email", String.class));
    }

    @Test
    void issue_setsTwelveHourExpiry() {
        String token = jwtService.issue(sampleUser());

        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        long lifetimeMs = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        // 12 saat = 12 * 60 * 60 * 1000 ms
        assertEquals(12L * 60 * 60 * 1000, lifetimeMs);
    }

    @Test
    void token_cannotBeVerifiedWithWrongKey() {
        String token = jwtService.issue(sampleUser());

        // Fərqli açar -> imza uyğun gəlmir -> istisna atılmalıdır (təhlükəsizlik)
        SecretKey wrongKey = Keys.hmacShaKeyFor(
                "totally-different-secret-min-32-chars-xxxx".getBytes(StandardCharsets.UTF_8));

        assertThrows(JwtException.class, () ->
                Jwts.parser()
                        .verifyWith(wrongKey)
                        .build()
                        .parseSignedClaims(token));
    }

    @Test
    void issue_producesDifferentTokensForDifferentUsers() {
        String tokenA = jwtService.issue(User.builder().id(1L).email("a@x.az").role(Role.USER).build());
        String tokenB = jwtService.issue(User.builder().id(2L).email("b@x.az").role(Role.DOCTOR).build());

        assertTrue(!tokenA.equals(tokenB));
    }
}
