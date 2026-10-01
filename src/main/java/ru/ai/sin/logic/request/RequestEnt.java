package ru.ai.sin.logic.request;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import ru.ai.sin.logic.chat.ChatEnt;
import ru.ai.sin.models.enums.convertor.ResultEnumConverter;
import ru.ai.sin.models.enums.ResultEnum;
import ru.ai.sin.models.embeddables.TimeStamped;

import ru.ai.sin.logic.recruiter.RecruiterEnt;
import ru.ai.sin.logic.student.StudentEnt;

@Entity
@Table(name = "requests")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class RequestEnt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "result", length = 16, nullable = false)
    @Convert(converter = ResultEnumConverter.class)
    private ResultEnum result = ResultEnum.WAITING;

    @Column(name = "student_response_text", columnDefinition = "TEXT")
    private String studentResponseText;

    @Embedded
    private TimeStamped timestamps = new TimeStamped();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_chat_id", nullable = false)
    private ChatEnt appChat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiter_id", nullable = false)
    private RecruiterEnt recruiter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentEnt student;

    @Column(name = "student_tu_confirmed_at")
    private java.time.LocalDateTime studentTuConfirmedAt;

    @Column(name = "recruiter_tu_confirmed_at")
    private java.time.LocalDateTime recruiterTuConfirmedAt;

    @Column(name = "rejection_reason_code", length = 64)
    private String rejectionReasonCode;

    @Column(name = "rejection_comment", columnDefinition = "TEXT")
    private String rejectionComment;
}
