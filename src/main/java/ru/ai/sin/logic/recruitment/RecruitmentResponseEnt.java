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
import ru.ai.sin.models.enums.RecruitmentResponseStatus;
import ru.ai.sin.models.enums.convertor.RecruitmentResponseStatusConverter;

import java.util.UUID;

@Entity
@Table(name = "recruitment_responses")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class RecruitmentResponseEnt {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @Column(name = "recruitment_request_id", nullable = false)
    private UUID recruitmentRequestId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Convert(converter = RecruitmentResponseStatusConverter.class)
    @Column(nullable = false, length = 32)
    private RecruitmentResponseStatus status = RecruitmentResponseStatus.PENDING;

    @Embedded
    private TimeStamped timestamps = new TimeStamped();
}
