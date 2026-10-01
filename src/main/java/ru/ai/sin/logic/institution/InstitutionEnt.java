package ru.ai.sin.logic.institution;

import jakarta.persistence.*;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import ru.ai.sin.logic.student.StudentEnt;
import ru.ai.sin.models.embeddables.TimeStamped;

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

    @Embedded
    private TimeStamped timestamps = new TimeStamped();

    @ManyToOne
    @JoinColumn(name = "education_id", nullable = false)
    private EducationEnt education;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private StudentEnt student;
}
