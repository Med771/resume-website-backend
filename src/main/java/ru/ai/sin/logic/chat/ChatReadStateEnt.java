package ru.ai.sin.logic.chat;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chat_read_states")
@IdClass(ChatReadStateId.class)
@Getter
@Setter
@NoArgsConstructor
public class ChatReadStateEnt {

    @Id
    @Column(name = "chat_id", nullable = false)
    private UUID chatId;

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "last_read_message_id")
    private UUID lastReadMessageId;

    @Column(name = "last_read_at")
    private LocalDateTime lastReadAt;
}
