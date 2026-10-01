package ru.ai.sin.logic.education;

import jakarta.persistence.*;

import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import ru.ai.sin.logic.institution.InstitutionEnt;
import ru.ai.sin.models.embeddables.TimeStamped;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "education")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EducationEnt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String institution;

    @Column(name = "additional_info", columnDefinition = "TEXT")
    @Basic(fetch = FetchType.LAZY)
    @Size(max = 2000, message = "Additional info must be less than 2000 characters")
    private String additionalInfo;

    @Column(name = "web_url", nullable = false)
    private String webUrl;

    @Embedded
    private TimeStamped timestamps = new TimeStamped();

    @OneToMany(mappedBy = "education", fetch = FetchType.LAZY)
    private Set<InstitutionEnt> institutions = new HashSet<>();
}
