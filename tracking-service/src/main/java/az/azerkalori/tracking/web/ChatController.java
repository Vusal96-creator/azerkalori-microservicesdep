package az.azerkalori.tracking.web;

import az.azerkalori.tracking.entity.ChatMessage;
import az.azerkalori.tracking.repo.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Həkim ↔ pasiyent canlı yazışması.
 * - WebSocket: /app/chat.send -> mesaj DB-yə yazılır və hər iki tərəfin
 *   /user/queue/chat kanalına canlı göndərilir.
 * - REST: keçmiş yazışmanı gətirir.
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatMessageRepository messages;
    private final SimpMessagingTemplate ws;

    // ---- WebSocket: mesaj göndər ----
    @MessageMapping("/chat.send")
    public void send(@Payload ChatIn in, Principal principal) {
        Long senderId = Long.valueOf(principal.getName());

        ChatMessage saved = messages.save(ChatMessage.builder()
                .senderId(senderId)
                .recipientId(in.recipientId())
                .content(in.content())
                .createdAt(Instant.now())
                .build());

        Map<String, Object> out = Map.of(
                "id", saved.getId(),
                "senderId", saved.getSenderId(),
                "recipientId", saved.getRecipientId(),
                "content", saved.getContent(),
                "createdAt", saved.getCreatedAt().toString());

        // Alıcıya və göndərənin öz digər cihazlarına canlı çatdır.
        ws.convertAndSendToUser(String.valueOf(in.recipientId()), "/queue/chat", out);
        ws.convertAndSendToUser(String.valueOf(senderId), "/queue/chat", out);
    }

    // ---- REST: keçmiş yazışma ----
    @GetMapping("/{peerId}")
    public List<ChatMessage> history(@RequestHeader("X-User-Id") Long userId,
                                     @PathVariable Long peerId) {
        return messages.conversation(userId, peerId);
    }

    public record ChatIn(Long recipientId, String content) {}
}
