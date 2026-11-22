package ru.ai.sin.logic.recruitment;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import ru.ai.sin.models.embeddables.TimeStamped;
import ru.ai.sin.models.enums.CooperationFormat;
import ru.ai.sin.models.enums.RecruitmentRequestStatus;
import ru.ai.sin.models.enums.convertor.CooperationFormatConverter;
import ru.ai.sin.models.enums.convertor.RecruitmentRequestStatusConverter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "recruitment_requests")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class RecruitmentRequestEnt {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @Column(name = "recruiter_id")
    private UUID recruiterId;

    @Column(name = "company_name")
    private String companyName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String tasks;

    @Column(name = "qualification_requirements", nullable = false, columnDefinition = "TEXT")
    private String qualificationRequirements;

    @Column(name = "work_conditions", nullable = false, columnDefinition = "TEXT")
    private String workConditions;

    @Column(name = "student_count", nullable = false)
    private int studentCount;

    @Convert(converter = CooperationFormatConverter.class)
    @Column(name = "cooperation_format", length = 32)
    private CooperationFormat cooperationFormat;

    @Column(name = "ready_to_request_students")
    private Boolean readyToRequestStudents;

    @Column(name = "schedule_note", columnDefinition = "TEXT")
    private String scheduleNote;

    @Column(name = "extra_info", columnDefinition = "TEXT")
    private String extraInfo;

    @Column(name = "published_from")
    private LocalDateTime publishedFrom;

    @Column(name = "published_to")
    private LocalDateTime publishedTo;

    @Convert(converter = RecruitmentRequestStatusConverter.class)
    @Column(nullable = false, length = 32)
    private RecruitmentRequestStatus status = RecruitmentRequestStatus.OPEN;

    @Embedded
    private TimeStamped timestamps = new TimeStamped();
}
