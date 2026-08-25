package az.azerkalori.auth.mail;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Qeydiyyatdan keçən istifadəçiyə xoşgəldin / dəvət məktubu göndərir.
 * Qeyd: @Async ilə arxa fonda işləyir və istənilən SMTP xətası udulur —
 * yəni məktub getməsə belə qeydiyyat uğursuz olmur.
 */
@Service
@Slf4j
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:AzərKalori <no-reply@azerkalori.az>}")
    private String from;

    @Value("${app.mail.enabled:true}")
    private boolean enabled;

    @Value("${app.mail.app-url:http://localhost}")
    private String appUrl;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendWelcome(String toEmail, String fullName) {
        if (!enabled) {
            log.debug("Mail disabled — skipping welcome email to {}", toEmail);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(from);
            helper.setTo(toEmail);
            helper.setSubject("AzərKalori-yə xoş gəlmisiniz! 🥗");
            helper.setText(buildHtml(fullName), true);
            mailSender.send(message);
            log.info("Welcome email sent to {}", toEmail);
        } catch (Exception e) {
            // Qeydiyyatı sındırmırıq — sadəcə loglayırıq.
            log.warn("Could not send welcome email to {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildHtml(String fullName) {
        String name = (fullName == null || fullName.isBlank()) ? "istifadəçi" : fullName;
        return """
                <div style="font-family:Arial,sans-serif;max-width:520px;margin:auto;
                            border:1px solid #eee;border-radius:12px;overflow:hidden">
                  <div style="background:#16a34a;color:#fff;padding:20px 24px">
                    <h2 style="margin:0">🥗 AzərKalori</h2>
                  </div>
                  <div style="padding:24px;color:#222;line-height:1.6">
                    <p>Salam, <b>%s</b>!</p>
                    <p>AzərKalori ailəsinə xoş gəlmisiniz. Artıq gündəlik kalorinizi
                       hesablaya, yediklərinizi izləyə və hədəflərinizə çata bilərsiniz.</p>
                    <p style="text-align:center;margin:28px 0">
                      <a href="%s" style="background:#16a34a;color:#fff;text-decoration:none;
                         padding:12px 22px;border-radius:8px;display:inline-block">
                         Tətbiqə keç
                      </a>
                    </p>
                    <p style="color:#666;font-size:13px">
                       Bu məktubu səhvən aldınızsa, nəzərə almayın.</p>
                  </div>
                </div>
                """.formatted(name, appUrl);
    }
}
