package az.azerkalori.auth.web;

import az.azerkalori.auth.entity.User;
import az.azerkalori.auth.repo.UserRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Pro abunə üçün Stripe ödənişi.
 *
 * Təhlükəsizlik: kart nömrəsi HEÇ VAXT bu backend-ə gəlmir — istifadəçi kartı
 * Stripe-ın öz hosted səhifəsində (Checkout) daxil edir. Biz yalnız checkout
 * sessiyası yaradırıq və uğuru webhook (imza yoxlanışı ilə) təsdiqləyirik.
 */
@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
@Slf4j
public class BillingController {

    private final UserRepository users;

    @Value("${stripe.secret-key:}")      private String secretKey;
    @Value("${stripe.webhook-secret:}")  private String webhookSecret;
    @Value("${stripe.currency:usd}")     private String currency;
    @Value("${stripe.pro-amount-cents:999}") private long amountCents;
    @Value("${stripe.pro-days:30}")      private long proDays;
    @Value("${app.frontend-url:http://localhost}") private String frontendUrl;

    /** Ödəniş səhifəsi üçün Stripe Checkout sessiyası yaradır və URL qaytarır. */
    @PostMapping("/checkout")
    public Map<String, Object> checkout(@RequestHeader("X-User-Id") Long userId) throws StripeException {
        User user = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Stripe.apiKey = secretKey;

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(frontendUrl + "/?pro=success")
                .setCancelUrl(frontendUrl + "/?pro=cancel")
                .setClientReferenceId(String.valueOf(userId))
                .setCustomerEmail(user.getEmail())
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(currency)
                                .setUnitAmount(amountCents)
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("AzərKalori Pro")
                                        .build())
                                .build())
                        .build())
                .build();

        Session session = Session.create(params);
        Map<String, Object> res = new HashMap<>();
        res.put("url", session.getUrl());
        res.put("sessionId", session.getId());
        return res;
    }

    /** Stripe geri çağırışı: ödəniş uğurlu olanda istifadəçini Pro edir. */
    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(@RequestBody String payload,
                                          @RequestHeader("Stripe-Signature") String signature) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, signature, webhookSecret);
        } catch (Exception e) {
            log.warn("Stripe webhook signature invalid: {}", e.getMessage());
            return ResponseEntity.badRequest().body("invalid signature");
        }

        if ("checkout.session.completed".equals(event.getType())) {
            Optional<StripeObject> obj = event.getDataObjectDeserializer().getObject();
            if (obj.isPresent() && obj.get() instanceof Session session) {
                String ref = session.getClientReferenceId();
                if (ref != null) {
                    users.findById(Long.valueOf(ref)).ifPresent(u -> {
                        u.setPro(true);
                        u.setProUntil(Instant.now().plus(proDays, ChronoUnit.DAYS));
                        users.save(u);
                        log.info("User {} upgraded to Pro until {}", u.getId(), u.getProUntil());
                    });
                }
            }
        }
        return ResponseEntity.ok("ok");
    }

    /** İstifadəçinin Pro statusu. */
    @GetMapping("/status")
    public Map<String, Object> status(@RequestHeader("X-User-Id") Long userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Map<String, Object> res = new HashMap<>();
        res.put("pro", user.isPro());
        res.put("proUntil", user.getProUntil());
        return res;
    }
}
