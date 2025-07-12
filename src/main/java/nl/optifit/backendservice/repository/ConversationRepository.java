package nl.optifit.backendservice.repository;

import nl.optifit.backendservice.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    List<Conversation> findTop10ByChatSessionIdOrderByCreatedAtDesc(UUID chatSessionId);
}
