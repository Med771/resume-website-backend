package ru.ai.sin.logic.institution;

import jakarta.persistence.*;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import ru.ai.sin.logic.student.StudentEnt;
import ru.ai.sin.models.embeddables.TimeStamped;
import ru.ai.sin.models.enums.EducationType;
import ru.ai.sin.models.enums.convertor.EducationTypeConverter;

import ru.ai.sin.logic.education.EducationEnt;

@Entity
@Table(name = "institution")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class InstitutionEnt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "start_year", nullable = false)
    @Min(1900)
    @Max(2100)
    private int startYear;

    @Column(name = "end_year", nullable = false)
    @Min(1900)
    @Max(2100)
    private int endYear;

    /** Гос. образование или образовательный курс. Без поля сохраняется гос. образование. */
    @Column(name = "education_type", nullable = false, length = 32)
    @Convert(converter = EducationTypeConverter.class)
    private EducationType educationType = EducationType.STATE;

    /** Направление подготовки. Пусто, пока студент его не указал. */
    @Column(length = 255)
    @Size(max = 255)
    private String direction;

    /** Уровень образования. Пусто, пока студент его не указал. */
    @Column(name = "education_level", length = 255)
    @Size(max = 255)
    private String educationLevel;

    @Embedded
    private TimeStamped timestamps = new TimeStamped();

    @ManyToOne
    @JoinColumn(name = "education_id", nullable = false)
    private EducationEnt education;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private StudentEnt student;
}
