package ru.ai.sin.logic.chat;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.models.embeddables.TimeStamped;
import ru.ai.sin.models.enums.ChatMessageKind;
import ru.ai.sin.models.enums.convertor.ChatMessageKindConverter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chat_messages")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class ChatMessageEnt {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_id", nullable = false)
    private ChatEnt chat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_user_id")
    private UserEnt author;

    @Column(name = "message_kind", nullable = false, length = 16)
    @Convert(converter = ChatMessageKindConverter.class)
    private ChatMessageKind messageKind;

    /** Для SYSTEM: код события (REQUEST_SENT, STUDENT_ACCEPTED, …) */
    @Column(name = "system_event", length = 64)
    private String systemEvent;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Column(name = "attachment_storage_name", length = 512)
    private String attachmentStorageName;

    @Embedded
    private TimeStamped timestamps = new TimeStamped();

    @Column(name = "edited_at")
    private LocalDateTime editedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by_admin", nullable = false)
    private boolean deletedByAdmin;
}
