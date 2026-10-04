package ru.ai.sin.logic.vacancy;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import ru.ai.sin.logic.recruiter.RecruiterEnt;
import ru.ai.sin.logic.skill.SkillEnt;
import ru.ai.sin.logic.speciality.SpecialityEnt;
import ru.ai.sin.models.embeddables.TimeStamped;
import ru.ai.sin.models.enums.VacancyEmploymentTypeEnum;
import ru.ai.sin.models.enums.VacancyStatus;
import ru.ai.sin.models.enums.WorkFormatEnum;
import ru.ai.sin.models.enums.convertor.VacancyEmploymentTypeEnumConverter;
import ru.ai.sin.models.enums.convertor.VacancyStatusConverter;
import ru.ai.sin.models.enums.convertor.WorkFormatEnumConverter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "vacancies")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class VacancyEnt {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recruiter_id", nullable = false)
    private RecruiterEnt recruiter;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "company_name")
    private String companyName;

    private String city;

    @Column(name = "work_format", length = 16)
    @Convert(converter = WorkFormatEnumConverter.class)
    private WorkFormatEnum workFormat;

    @Column(name = "employment_type", length = 16)
    @Convert(converter = VacancyEmploymentTypeEnumConverter.class)
    private VacancyEmploymentTypeEnum employmentType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "speciality_id")
    private SpecialityEnt speciality;

    @Column(nullable = false, length = 16)
    @Convert(converter = VacancyStatusConverter.class)
    private VacancyStatus status = VacancyStatus.DRAFT;

    @Column(name = "published_from")
    private LocalDateTime publishedFrom;

    @Column(name = "published_to")
    private LocalDateTime publishedTo;

    @Column(name = "slots_count")
    private Integer slotsCount;

    @Column(name = "submitted_for_review_at")
    private LocalDateTime submittedForReviewAt;

    @Column(name = "moderated_at")
    private LocalDateTime moderatedAt;

    @Column(name = "moderated_by_username", length = 64)
    private String moderatedByUsername;

    @Column(name = "moderation_rejection_reason", columnDefinition = "TEXT")
    private String moderationRejectionReason;

    @Column(name = "manual_sort_order")
    private Integer manualSortOrder;

    @Column(name = "visible_to_anonymous", nullable = false)
    private boolean visibleToAnonymous = false;

    @Embedded
    private TimeStamped timestamps = new TimeStamped();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "vacancy_skills",
            joinColumns = @JoinColumn(name = "vacancy_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    private Set<SkillEnt> skills = new HashSet<>();
}
