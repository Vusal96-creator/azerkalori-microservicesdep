package az.azerkalori.tracking.repo;

import az.azerkalori.tracking.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // İki istifadəçi arasındakı bütün yazışma (hər iki istiqamət), vaxt sırası ilə.
    @Query("""
           SELECT m FROM ChatMessage m
           WHERE (m.senderId = :a AND m.recipientId = :b)
              OR (m.senderId = :b AND m.recipientId = :a)
           ORDER BY m.createdAt ASC
           """)
    List<ChatMessage> conversation(@Param("a") Long a, @Param("b") Long b);
}
