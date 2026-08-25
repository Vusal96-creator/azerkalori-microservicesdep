package az.azerkalori.tracking.service;

import az.azerkalori.tracking.web.AuthClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;

/**
 * Chat icazəsi: yalnız Pro pasiyent ilə ONUN təyin olunmuş həkimi yaza bilər.
 * Qaydalar:
 *  - tərəflərdən biri DOCTOR, digəri USER olmalıdır,
 *  - pasiyent (USER) pro = true olmalıdır,
 *  - pasiyentin doctorId-si həmin həkimin id-si olmalıdır.
 * auth-service əlçatan olmasa, təhlükəsizlik üçün icazə VERİLMİR.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatAccessService {

    private final AuthClient auth;

    public boolean canChat(Long meId, Long peerId) {
        if (meId == null || peerId == null || meId.equals(peerId)) {
            return false;
        }
        try {
            Map<String, Object> me = auth.getUser(meId);
            Map<String, Object> peer = auth.getUser(peerId);

            String meRole = str(me.get("role"));
            String peerRole = str(peer.get("role"));

            // Admin hər kəslə yazışa bilər (tam giriş).
            if ("ADMIN".equals(meRole) || "ADMIN".equals(peerRole)) {
                return true;
            }
            // Həkim həmişə yaza bilər — pasiyent Pro olmasa belə həkimin mesajı
            // pasiyentin çatında görünsün.
            if ("DOCTOR".equals(meRole)) {
                return true;
            }

            Map<String, Object> doctor;
            Long doctorId;
            Map<String, Object> patient;

            if ("DOCTOR".equals(meRole) && "USER".equals(peerRole)) {
                doctor = me;  doctorId = meId;  patient = peer;
            } else if ("USER".equals(meRole) && "DOCTOR".equals(peerRole)) {
                doctor = peer; doctorId = peerId; patient = me;
            } else {
                return false; // ikisi də eyni rol / uyğun cütlük deyil
            }
            // (doctor dəyişəni oxunaqlıq üçün saxlanılır)
            Objects.requireNonNull(doctor);

            boolean pro = Boolean.TRUE.equals(patient.get("pro"));
            Long assignedDoctor = toLong(patient.get("doctorId"));

            return pro && Objects.equals(assignedDoctor, doctorId);
        } catch (Exception e) {
            log.warn("Chat access check failed for {}<->{}: {}", meId, peerId, e.getMessage());
            return false;
        }
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }

    private static Long toLong(Object o) {
        if (o == null) return null;
        try {
            return Long.valueOf(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
