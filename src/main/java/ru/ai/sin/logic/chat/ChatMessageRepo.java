package ru.ai.sin.logic.chat;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ru.ai.sin.models.enums.ChatMessageKind;
import ru.ai.sin.models.enums.RoleEnum;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface ChatMessageRepo extends JpaRepository<ChatMessageEnt, UUID> {

    /**
     * Если {@code fullHistory} false — только системные сообщения (ожидание решения студента).
     */
    @Query("""
            SELECT m FROM ChatMessageEnt m
            WHERE m.chat.id = :chatId AND m.deletedAt IS NULL
              AND (:fullHistory = true OR m.messageKind = :systemKind)
            ORDER BY m.timestamps.createdAt ASC, m.id ASC
            """)
    Page<ChatMessageEnt> findVisibleByChatIdGated(
            @Param("chatId") UUID chatId,
            @Param("fullHistory") boolean fullHistory,
            @Param("systemKind") ChatMessageKind systemKind,
            Pageable pageable);

    @Query("""
            SELECT COUNT(m) FROM ChatMessageEnt m
            WHERE m.chat.id = :chatId
              AND m.deletedAt IS NULL
              AND m.timestamps.createdAt > :after
              AND (m.author IS NULL OR m.author.id <> :userId)
            """)
    long countIncomingUnreadAfter(
            @Param("chatId") UUID chatId,
            @Param("after") LocalDateTime after,
            @Param("userId") UUID userId);

    @Query("""
            SELECT m FROM ChatMessageEnt m
            WHERE m.chat.id = :chatId AND m.deletedAt IS NULL
              AND (:fullHistory = true OR m.messageKind = :systemKind)
            ORDER BY m.timestamps.createdAt DESC, m.id DESC
            """)
    Page<ChatMessageEnt> findLastByChatIdGated(
            @Param("chatId") UUID chatId,
            @Param("fullHistory") boolean fullHistory,
            @Param("systemKind") ChatMessageKind systemKind,
            Pageable pageable);

    @Query("""
            SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM ChatMessageEnt m
            WHERE m.chat.id = :chatId
              AND m.deletedAt IS NULL
              AND m.messageKind = :kind
              AND m.author IS NOT NULL
              AND m.author.role = :role
            """)
    boolean existsUserMessageFromRole(
            @Param("chatId") UUID chatId,
            @Param("kind") ChatMessageKind kind,
            @Param("role") RoleEnum role);

    long countByChat_IdAndDeletedAtIsNull(UUID chatId);
}
