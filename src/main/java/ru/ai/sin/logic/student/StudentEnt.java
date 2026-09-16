package ru.ai.sin.logic.student;

import jakarta.persistence.*;

import jakarta.validation.constraints.Size;

import lombok.*;

import org.hibernate.annotations.UuidGenerator;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import ru.ai.sin.models.enums.convertor.BusynessEnumConverter;
import ru.ai.sin.models.enums.convertor.CourseEnumConverter;

import ru.ai.sin.logic.experience.ExperienceEnt;
import ru.ai.sin.logic.institution.InstitutionEnt;
import ru.ai.sin.logic.portfolio.PortfolioEnt;
import ru.ai.sin.logic.siteproject.SiteProjectEnt;
import ru.ai.sin.logic.skill.SkillEnt;
import ru.ai.sin.logic.speciality.SpecialityEnt;
import ru.ai.sin.models.embeddables.ContactInformation;
import ru.ai.sin.models.embeddables.TimeStamped;
import ru.ai.sin.models.embeddables.UserInformation;
import ru.ai.sin.models.enums.BusynessEnum;
import ru.ai.sin.models.enums.CourseEnum;
import ru.ai.sin.models.enums.GenderEnum;
import ru.ai.sin.models.enums.convertor.GenderEnumConverter;

import java.time.LocalDate;
import java.util.*;

@Entity
@Table(name = "students")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class StudentEnt {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    private String city;

    private String hhLink;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(columnDefinition = "TEXT")
    @Basic(fetch = FetchType.LAZY)
    @Size(max = 2000, message = "Additional info must be less than 2000 characters")
    private String bio;

    @Column(unique = true)
    private String imagePath;

    @Column(name = "course", length = 16)
    @Convert(converter = CourseEnumConverter.class)
    private CourseEnum course;

    @Column(name = "busyness", length = 32)
    @Convert(converter = BusynessEnumConverter.class)
    private BusynessEnum busyness;

    /** Отчество (не фамилия — фамилия в {@code userInformation.lastName}). */
    @Column(name = "middle_name")
    private String middleName;

    @Column(name = "gender", length = 16)
    @Convert(converter = GenderEnumConverter.class)
    private GenderEnum gender;

    @Embedded
    private UserInformation userInformation = new UserInformation();

    @Embedded
    private TimeStamped timestamps = new TimeStamped();

    @Embedded
    private ContactInformation contactInformation = new ContactInformation();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "speciality_id")
    private SpecialityEnt speciality;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "student_skills",
            joinColumns = @JoinColumn(name = "student_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id", referencedColumnName = "id")
    )
    private Set<SkillEnt> skills = new HashSet<>();

    @OneToMany(mappedBy = "student", fetch = FetchType.LAZY)
    private List<PortfolioEnt> portfolio = new ArrayList<>();

    @OneToMany(mappedBy = "student", fetch = FetchType.LAZY)
    private List<InstitutionEnt> education = new ArrayList<>();

    @OneToMany(mappedBy = "student", fetch = FetchType.LAZY)
    private List<ExperienceEnt> companies = new ArrayList<>();

    @Column(name = "catalog_visible", nullable = false)
    private boolean catalogVisible = true;

    @Column(name = "public_profile_consent", nullable = false)
    private boolean publicProfileConsent;

    @Column(name = "profile_text_score", nullable = false)
    private int profileTextScore;

    /**
     * Ручной приоритет в каталоге: при сортировке меньшие значения выше (ASC). {@code null} — не задано.
     */
    @Column(name = "manual_sort_order")
    private Integer manualSortOrder;

    @ManyToMany(mappedBy = "students", fetch = FetchType.LAZY)
    private Set<SiteProjectEnt> siteProjects = new HashSet<>();
}
