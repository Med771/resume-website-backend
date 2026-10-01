package ru.ai.sin.logic.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatReadStateRepo extends JpaRepository<ChatReadStateEnt, ChatReadStateId> {

    Optional<ChatReadStateEnt> findByChatIdAndUserId(UUID chatId, UUID userId);
}
