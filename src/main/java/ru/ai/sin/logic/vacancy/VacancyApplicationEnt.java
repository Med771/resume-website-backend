package ru.ai.sin.logic.vacancy;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import ru.ai.sin.logic.chat.ChatEnt;
import ru.ai.sin.logic.student.StudentEnt;
import ru.ai.sin.models.embeddables.TimeStamped;
import ru.ai.sin.models.enums.VacancyApplicationStatus;
import ru.ai.sin.models.enums.convertor.VacancyApplicationStatusConverter;

import java.util.UUID;

@Entity
@Table(name = "vacancy_applications")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class VacancyApplicationEnt {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vacancy_id", nullable = false)
    private VacancyEnt vacancy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentEnt student;

    @Column(nullable = false, length = 16)
    @Convert(converter = VacancyApplicationStatusConverter.class)
    private VacancyApplicationStatus status = VacancyApplicationStatus.SUBMITTED;

    @Column(name = "cover_letter", columnDefinition = "TEXT")
    private String coverLetter;

    @Column(name = "recruiter_comment", columnDefinition = "TEXT")
    private String recruiterComment;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_chat_id")
    private ChatEnt appChat;

    @Column(name = "student_tu_confirmed_at")
    private java.time.LocalDateTime studentTuConfirmedAt;

    @Column(name = "recruiter_tu_confirmed_at")
    private java.time.LocalDateTime recruiterTuConfirmedAt;

    @Column(name = "rejection_reason_code", length = 64)
    private String rejectionReasonCode;

    @Column(name = "rejection_comment", columnDefinition = "TEXT")
    private String rejectionComment;

    @Embedded
    private TimeStamped timestamps = new TimeStamped();
}
