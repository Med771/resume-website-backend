package ru.ai.sin.logic.chat;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import ru.ai.sin.logic.recruiter.RecruiterEnt;
import ru.ai.sin.logic.student.StudentEnt;
import ru.ai.sin.models.embeddables.TimeStamped;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chats")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class ChatEnt {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recruiter_id", nullable = false)
    private RecruiterEnt recruiter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentEnt student;

    @Embedded
    private TimeStamped timestamps = new TimeStamped();

    @Column(name = "last_activity_at", nullable = false)
    private LocalDateTime lastActivityAt = LocalDateTime.now();
}
