package ru.ai.sin.logic.experience;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import ru.ai.sin.logic.student.StudentEnt;
import ru.ai.sin.models.embeddables.TimeStamped;

import ru.ai.sin.logic.company.CompanyEnt;

import java.time.LocalDate;

@Entity
@Table(name = "experiences")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class ExperienceEnt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String position;

    @Column(name = "additional_info", columnDefinition = "TEXT")
    @Size(max = 2000, message = "Additional info must be less than 2000 characters")
    private String additionalInfo;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Embedded
    private TimeStamped timestamps = new TimeStamped();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyEnt company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentEnt student;
}
